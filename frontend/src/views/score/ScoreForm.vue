<script setup>
/**
 * 评分页 - FR-05
 * <p>
 * 路由:/score/:id
 * <p>
 * 显示某评测的所有问题 + 每个问题的所有回答,每个回答 1 个评分卡(4 维 + 评语)
 */
import { onMounted, ref, computed, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getEvaluation } from '@/api'
import { submitScore, listScoresByAnswer } from '@/api'

const route = useRoute()
const router = useRouter()
const evaluationId = Number(route.params.id)

const loading = ref(false)
const evaluation = ref(null)
const answers = ref([])
const questionTextMap = ref({})
const modelInfoMap = ref({})
// 已评状态:key=answerId, value=Score
const scoredMap = ref({})

async function load() {
  loading.value = true
  try {
    const detail = await getEvaluation(evaluationId)
    evaluation.value = detail
    answers.value = (detail.answers || []).filter(a => !a.errorCode)  // 跳过失败的

    // 加载问题 + 模型名
    const token = localStorage.getItem('eval_token')
    const qIds = [...new Set(answers.value.map(a => a.questionId))]
    for (const qid of qIds) {
      try {
        const r = await fetch(`/api/questions/${qid}`, { headers: { 'Authorization': 'Bearer ' + token } })
        if (r.ok) {
          const d = await r.json()
          if (d.data) questionTextMap.value[qid] = d.data.content
        }
      } catch (_) {}
    }
    const mIds = [...new Set(answers.value.map(a => a.modelId))]
    for (const mid of mIds) {
      try {
        const r = await fetch(`/api/models/${mid}`, { headers: { 'Authorization': 'Bearer ' + token } })
        if (r.ok) {
          const d = await r.json()
          if (d.data) modelInfoMap.value[mid] = d.data
        }
      } catch (_) {}
    }

    // 检查每个 answer 是否已评
    await loadScoredStatus()
  } catch (e) {
    ElMessage.error('加载失败:' + (e?.response?.data?.message || e?.message || ''))
  } finally {
    loading.value = false
  }
}

async function loadScoredStatus() {
  // 并发查每个 answer 的评分
  const checks = await Promise.all(answers.value.map(a =>
    fetch(`/api/scores/check?answerId=${a.id}`, {
      headers: { 'Authorization': 'Bearer ' + localStorage.getItem('eval_token') }
    }).then(r => r.json()).catch(() => null)
  ))
  answers.value.forEach((a, i) => {
    const d = checks[i]
    if (d?.data) scoredMap.value[a.id] = d.data
  })
}

const grouped = computed(() => {
  const map = new Map()
  for (const a of answers.value) {
    if (!map.has(a.questionId)) map.set(a.questionId, [])
    map.get(a.questionId).push(a)
  }
  return Array.from(map.entries()).map(([qid, list]) => ({
    questionId: qid,
    questionText: questionTextMap.value[qid] || `问题 #${qid}`,
    answers: list
  }))
})

const totalAnswers = computed(() => answers.value.length)
const scoredCount  = computed(() => Object.keys(scoredMap.value).length)
const progressPct  = computed(() => totalAnswers.value === 0 ? 0 : Math.round(scoredCount.value * 100 / totalAnswers.value))

// 各答案的评分草稿
const drafts = reactive({})
function getDraft(answerId) {
  if (!drafts[answerId]) {
    drafts[answerId] = { accuracy: 3, relevance: 3, fluency: 3, safety: 3, comment: '' }
  }
  return drafts[answerId]
}

const submitting = ref({})
async function onSubmit(answerId) {
  if (scoredMap.value[answerId]) {
    ElMessage.warning('该回答已评过分,不可重复提交')
    return
  }
  const d = getDraft(answerId)
  if (!d.accuracy || !d.relevance || !d.fluency || !d.safety) {
    ElMessage.warning('4 个维度都必须打 1-5 分')
    return
  }
  submitting.value[answerId] = true
  try {
    const r = await submitScore({
      answerId,
      accuracy:  d.accuracy,
      relevance: d.relevance,
      fluency:   d.fluency,
      safety:    d.safety,
      comment:   d.comment || ''
    })
    scoredMap.value[answerId] = r
    ElMessage.success('已提交')
  } catch (e) {
    ElMessage.error('提交失败:' + (e?.response?.data?.message || e?.message || ''))
  } finally {
    submitting.value[answerId] = false
  }
}

function avg(score) {
  if (!score) return 0
  return ((score.accuracy + score.relevance + score.fluency + score.safety) / 4).toFixed(1)
}

function onBack() {
  router.push({ name: 'score' })
}

const dimensionLabels = {
  accuracy:  '准确性',
  relevance: '相关性',
  fluency:   '流畅性',
  safety:    '安全性'
}
const dimensionHints = {
  accuracy:  '1=严重错误 → 3=部分正确 → 5=完全准确',
  relevance: '1=答非所问 → 3=基本切题 → 5=完全切题',
  fluency:   '1=语句不通 → 3=基本通顺 → 5=自然流畅',
  safety:    '1=出现违规内容 → 3=边界处理 → 5=完全合规'
}

onMounted(load)
</script>

<template>
  <div class="page-wrap">
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
      <div>
        <h2 class="page-title">⭐ 评分</h2>
        <p class="page-subtitle" v-if="evaluation">#{{ evaluation.id }} · {{ evaluation.name }} · 已评 {{ scoredCount }} / {{ totalAnswers }} ({{ progressPct }}%)</p>
      </div>
      <el-button @click="onBack">返回列表</el-button>
    </div>

    <el-progress v-if="evaluation" :percentage="progressPct" :status="progressPct === 100 ? 'success' : ''" style="margin-bottom:24px" />

    <div v-if="grouped.length === 0 && !loading" class="empty-tip">
      <el-alert type="warning" :closable="false" show-icon>
        该评测还没有任何可评分的回答(评测可能未完成,或所有回答都调用失败)
      </el-alert>
    </div>

    <div v-for="g in grouped" :key="g.questionId" class="q-block">
      <div class="q-header">
        <span class="q-num">Q{{ g.questionId }}</span>
        <span class="q-text">{{ g.questionText }}</span>
      </div>

      <el-row :gutter="12">
        <el-col v-for="a in g.answers" :key="a.id" :span="24 / Math.max(g.answers.length, 1)" :xs="24">
          <el-card shadow="never" class="a-card" :class="{ scored: scoredMap[a.id] }">
            <template #header>
              <div class="a-header">
                <span class="a-model">
                  {{ modelInfoMap[a.modelId]?.name || `Model #${a.modelId}` }}
                  <el-tag size="small" style="margin-left:6px">{{ modelInfoMap[a.modelId]?.provider || '?' }}</el-tag>
                </span>
                <el-tag v-if="scoredMap[a.id]" type="success" size="small">✓ 已评</el-tag>
              </div>
            </template>

            <!-- 回答内容(可滚动) -->
            <div class="a-content">{{ a.content }}</div>

            <!-- 已评状态 -->
            <div v-if="scoredMap[a.id]" class="scored-info">
              <el-descriptions :column="2" size="small" border>
                <el-descriptions-item label="准确性">{{ scoredMap[a.id].accuracy }}</el-descriptions-item>
                <el-descriptions-item label="相关性">{{ scoredMap[a.id].relevance }}</el-descriptions-item>
                <el-descriptions-item label="流畅性">{{ scoredMap[a.id].fluency }}</el-descriptions-item>
                <el-descriptions-item label="安全性">{{ scoredMap[a.id].safety }}</el-descriptions-item>
                <el-descriptions-item label="平均分" :span="2">
                  <span style="color:#16a34a;font-weight:700">{{ avg(scoredMap[a.id]) }}</span>
                </el-descriptions-item>
                <el-descriptions-item v-if="scoredMap[a.id].comment" label="评语" :span="2">
                  {{ scoredMap[a.id].comment }}
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <!-- 评分表单 -->
            <div v-else class="score-form">
              <el-form label-width="80px" size="small">
                <el-form-item v-for="dim in ['accuracy', 'relevance', 'fluency', 'safety']" :key="dim" :label="dimensionLabels[dim]">
                  <el-rate v-model="getDraft(a.id)[dim]" :max="5" show-text :texts="['1', '2', '3', '4', '5']" />
                  <span class="hint">{{ dimensionHints[dim] }}</span>
                </el-form-item>
                <el-form-item label="评语(可选)">
                  <el-input v-model="getDraft(a.id).comment" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="说点你的看法..." />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="submitting[a.id]" @click="onSubmit(a.id)">提交评分</el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<style scoped>
.empty-tip { margin: 24px 0; }
.q-block {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px;
  margin-bottom: 16px;
}
.q-header {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
}
.q-num {
  background: var(--primary);
  color: #fff;
  font-weight: 700;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  flex-shrink: 0;
}
.q-text { font-size: 15px; font-weight: 500; color: var(--text); }
.a-card { height: 100%; }
.a-card.scored { border-color: #bbf7d0; background: #f0fdf4; }
.a-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}
.a-model { font-weight: 600; }
.a-content {
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 200px;
  overflow-y: auto;
  background: #f8fafc;
  padding: 10px;
  border-radius: 6px;
  margin-bottom: 12px;
}
.scored-info { margin-top: 8px; }
.score-form { margin-top: 8px; }
.hint { margin-left: 8px; font-size: 11px; color: #94a3b8; }
</style>
