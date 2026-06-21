<script setup>
/**
 * 评测详情页 - FR-04
 * <p>
 * 路由:/evaluation/:id
 * <p>
 * 显示状态、统计、并排展示每个问题的所有模型回答
 */
import { onMounted, onUnmounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEvaluation, runEvaluation, listAnswers } from '@/api'

const route = useRoute()
const router = useRouter()
const evaluationId = Number(route.params.id)

const loading = ref(false)
const evaluation = ref(null)
const answers = ref([])
const questionTextMap = ref({})
const modelInfoMap = ref({})

// ============== 数据加载 ==============
async function load() {
  loading.value = true
  try {
    const detail = await getEvaluation(evaluationId)
    evaluation.value = detail
    answers.value = detail.answers || []

    // 加载问题文本(逐个)
    const qIds = [...new Set(answers.value.map(a => a.questionId))]
    for (const qid of qIds) {
      try {
        const r = await fetch(`/api/questions/${qid}`, {
          headers: { 'Authorization': 'Bearer ' + localStorage.getItem('eval_token') }
        })
        if (r.ok) {
          const d = await r.json()
          if (d.data) questionTextMap.value[qid] = d.data.content
        }
      } catch (_) {}
    }

    // 加载模型名(逐个)
    const mIds = [...new Set(answers.value.map(a => a.modelId))]
    for (const mid of mIds) {
      try {
        const r = await fetch(`/api/models/${mid}`, {
          headers: { 'Authorization': 'Bearer ' + localStorage.getItem('eval_token') }
        })
        if (r.ok) {
          const d = await r.json()
          if (d.data) modelInfoMap.value[mid] = d.data
        }
      } catch (_) {}
    }
  } catch (e) {
    ElMessage.error('加载失败:' + (e?.response?.data?.message || e?.message || ''))
  } finally {
    loading.value = false
  }
}

async function onRun() {
  try {
    await ElMessageBox.confirm('确定要启动这次评测吗?', '提示', { type: 'warning' })
  } catch (_) { return }
  try {
    await runEvaluation(evaluationId)
    ElMessage.success('已启动')
    await load()
  } catch (e) {
    ElMessage.error('启动失败:' + (e?.response?.data?.message || e?.message || ''))
  }
}

function onBack() {
  router.push({ name: 'evaluation' })
}

// ============== 自动刷新(运行中) ==============
let timer = null
onMounted(() => {
  load()
  timer = setInterval(() => {
    if (evaluation.value && (evaluation.value.status === 'RUNNING' || evaluation.value.status === 'PENDING')) {
      load()
    }
  }, 3000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })

// ============== 计算属性 ==============
const grouped = computed(() => {
  // 按 questionId 分组
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

const totalCost = computed(() => {
  return answers.value.reduce((s, a) => s + (a.estimatedCost || 0), 0)
})
const successCount = computed(() => answers.value.filter(a => !a.errorCode).length)
const failCount    = computed(() => answers.value.filter(a => a.errorCode).length)
const avgLatency   = computed(() => {
  const ok = answers.value.filter(a => a.latencyMs)
  if (ok.length === 0) return 0
  return Math.round(ok.reduce((s, a) => s + a.latencyMs, 0) / ok.length)
})

const statusTagType = {
  'PENDING':   'info',
  'RUNNING':   'warning',
  'COMPLETED': 'success',
  'FAILED':    'danger'
}
</script>

<template>
  <div class="page-wrap">
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
      <div>
        <h2 class="page-title">📊 评测详情</h2>
        <p class="page-subtitle" v-if="evaluation">#{{ evaluation.id }} · {{ evaluation.name }}</p>
      </div>
      <div style="display:flex;gap:8px">
        <el-button @click="onBack">返回列表</el-button>
        <el-button type="primary" v-if="evaluation?.status === 'PENDING' || evaluation?.status === 'FAILED'" @click="onRun">启动评测</el-button>
        <el-button v-if="evaluation?.status === 'COMPLETED'" type="success" @click="router.push({ name: 'score-form', params: { id: evaluationId } })">
          ⭐ 去评分
        </el-button>
      </div>
    </div>

    <!-- 概览信息 -->
    <el-card v-loading="loading" shadow="never" v-if="evaluation">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType[evaluation.status]" size="small">{{ evaluation.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="模型数">{{ (evaluation.modelIds || []).length }}</el-descriptions-item>
        <el-descriptions-item label="问题数">{{ (evaluation.questionIds || []).length }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ evaluation.startedAt || '—' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ evaluation.finishedAt || '—' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ evaluation.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="成功" :span="1">
          <span style="color:#16a34a;font-weight:600">{{ successCount }}</span> / {{ answers.length }}
        </el-descriptions-item>
        <el-descriptions-item label="失败" :span="1">
          <span style="color:#dc2626;font-weight:600">{{ failCount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="平均耗时" :span="1">
          {{ avgLatency }} ms
        </el-descriptions-item>
        <el-descriptions-item label="总费用" :span="3">
          <span style="color:#d97706;font-weight:600">¥{{ totalCost.toFixed(4) }}</span>
          <span style="color:#94a3b8;font-size:12px;margin-left:8px">(按模型单价估算)</span>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="3" v-if="evaluation.description">
          {{ evaluation.description }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 按问题并排展示回答 -->
    <div v-if="grouped.length > 0">
      <h3 style="margin:24px 0 12px;font-size:18px;font-weight:700">💬 答案详情</h3>
      <div v-for="g in grouped" :key="g.questionId" class="q-card">
        <div class="q-header">
          <span class="q-num">Q{{ g.questionId }}</span>
          <span class="q-text">{{ g.questionText }}</span>
        </div>
        <el-row :gutter="12">
          <el-col v-for="a in g.answers" :key="a.id" :span="24 / Math.max(g.answers.length, 1)" :xs="24">
            <el-card shadow="never" class="a-card" :class="{ fail: a.errorCode }">
              <template #header>
                <div class="a-header">
                  <span class="a-model">
                    {{ modelInfoMap[a.modelId]?.name || `Model #${a.modelId}` }}
                    <el-tag size="small" style="margin-left:6px">{{ modelInfoMap[a.modelId]?.provider || '?' }}</el-tag>
                  </span>
                  <span class="a-latency">{{ a.latencyMs }}ms</span>
                </div>
              </template>
              <template v-if="a.errorCode">
                <el-alert :title="a.errorCode" :type="'error'" :closable="false" show-icon>
                  {{ a.errorMessage }}
                </el-alert>
              </template>
              <template v-else>
                <div class="a-content">{{ a.content }}</div>
                <div v-if="a.tokenInput || a.tokenOutput" class="a-meta">
                  <el-tag size="small">输入 {{ a.tokenInput || 0 }} tok</el-tag>
                  <el-tag size="small" type="success">输出 {{ a.tokenOutput || 0 }} tok</el-tag>
                  <el-tag size="small" type="warning" v-if="a.estimatedCost">¥{{ a.estimatedCost.toFixed(4) }}</el-tag>
                </div>
              </template>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </div>

    <el-empty v-else-if="!loading && evaluation" description="暂无答案 · 点击右上角「启动评测」开始" />
  </div>
</template>

<style scoped>
.q-card {
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
.q-text {
  font-size: 15px;
  font-weight: 500;
  color: var(--text);
}
.a-card { height: 100%; }
.a-card.fail { border-color: #fecaca; background: #fef2f2; }
.a-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}
.a-model { font-weight: 600; }
.a-latency { color: #94a3b8; font-size: 12px; }
.a-content {
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 360px;
  overflow-y: auto;
}
.a-meta {
  margin-top: 12px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
</style>
