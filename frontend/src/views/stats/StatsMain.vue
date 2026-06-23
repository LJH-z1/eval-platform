<script setup>
/**
 * 一致性分析 - FR-06
 * <p>
 * 显示 Fleiss Kappa + 模型排名 + 争议项 + 评分员排行
 * 路由:/stats
 */
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pageEvaluations } from '@/api'
import { getKappa, getControversial, getScorerRanking, getModelRanking } from '@/api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const evaluations = ref([])
const selectedId = ref(null)
const kappa = ref(null)
const controversial = ref([])
const scorerRank = ref([])
const modelRank = ref([])

onMounted(loadEvalList)

async function loadEvalList() {
  try {
    const d = await pageEvaluations({ pageNum: 1, pageSize: 50, status: 'COMPLETED' })
    evaluations.value = d.list || []
    if (!selectedId.value && evaluations.value.length > 0) {
      // 默认选"有评分数据的第一条":查每条 score 数,挑 scoreCount>0 的
      const candidates = await Promise.all(
        evaluations.value.map(async e => {
          try {
            const r = await getKappa(e.id)
            return { id: e.id, totalScores: r.data?.totalScores || 0, name: e.name }
          } catch { return { id: e.id, totalScores: 0, name: e.name } }
        })
      )
      // 优先 totalScores>0 的,否则用最新
      const withData = candidates.filter(c => c.totalScores > 0)
      const pick = withData[0] || candidates[0]
      selectedId.value = pick.id
      console.log('[Stats] default picked:', pick)
      await loadStats()
    }
  } catch (e) {
    console.error('[Stats] loadEvalList failed:', e)
    ElMessage.error('加载评测列表失败:' + (e?.response?.data?.message || e?.message))
  }
}

async function loadStats() {
  if (!selectedId.value) return
  console.log('[Stats] loadStats 入口, selectedId=', selectedId.value)
  loading.value = true
  // 串行调用(任一失败不影响其他)
  try {
    const k = await getKappa(selectedId.value)
    console.log('[Stats] getKappa 返 (typeof):', typeof k, '值:', k)
    kappa.value = k || null
  } catch (e) { console.error('[Stats] getKappa 失败:', e?.message) }
  try {
    const c = await getControversial(selectedId.value)
    controversial.value = (c || []).length ? c : []
  } catch (e) { console.error('[Stats] getControversial 失败:', e?.message) }
  try {
    const s = await getScorerRanking(selectedId.value)
    console.log('[Stats] getScorerRanking 返:', s, 'len:', s?.length)
    scorerRank.value = Array.isArray(s) ? s : []
  } catch (e) { console.error('[Stats] getScorerRanking 失败:', e?.message) }
  try {
    const m = await getModelRanking(selectedId.value)
    console.log('[Stats] getModelRanking 返:', m, 'len:', m?.length)
    modelRank.value = Array.isArray(m) ? m : []
  } catch (e) { console.error('[Stats] getModelRanking 失败:', e?.message) }
  console.log('[Stats] 写入完成: kappa=', !!kappa.value, 'scorerRank.len=', scorerRank.value.length, 'modelRank.len=', modelRank.value.length)
  loading.value = false
}

function kappaColor(k) {
  if (k == null) return '#94a3b8'
  if (k < 0) return '#dc2626'
  if (k < 0.4) return '#f59e0b'
  if (k < 0.6) return '#3b82f6'
  if (k < 0.8) return '#10b981'
  return '#16a34a'
}

function kappaBarWidth(k) {
  if (k == null) return 0
  return Math.max(0, Math.min(100, (k + 1) * 50))  // -1..1 -> 0..100
}

function onEvalChange() {
  console.log('[Stats] onEvalChange, new id=', selectedId.value)
  loadStats()
}
</script>

<template>
  <div class="page-wrap">
    <h2 class="page-title">📊 一致性分析</h2>
    <p class="page-subtitle">Fleiss Kappa 评分员一致性 + 争议项 + 排名</p>

    <el-card shadow="never" v-if="evaluations.length > 0">
      <el-form inline>
        <el-form-item label="评测">
          <el-select v-model="selectedId" placeholder="选择评测" style="width:340px" @change="onEvalChange" filterable>
            <el-option v-for="e in evaluations" :key="e.id" :label="`#${e.id} · ${e.name}`" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadStats" :loading="loading">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-empty v-if="!loading && evaluations.length === 0" description="还没有已完成的评测" />

    <template v-if="kappa">
      <!-- 4 维 Kappa -->
      <el-card shadow="never" style="margin-top:16px">
        <template #header>
          <span>🎯 Fleiss Kappa 评分员一致性(0-1,越大越一致)</span>
        </template>
        <div v-if="Object.keys(kappa.kappas || {}).length > 0" class="kappa-grid">
          <div v-for="(v, k) in kappa.kappas" :key="k" class="kpi-box">
            <div class="kpi-label">{{ k }}</div>
            <div class="kpi-value" :style="{ color: kappaColor(v) }">
              {{ v == null ? 'N/A' : Number(v).toFixed(3) }}
            </div>
            <div class="kpi-bar">
              <div class="kpi-bar-fill" :style="{ width: kappaBarWidth(v) + '%', background: kappaColor(v) }"></div>
            </div>
            <div class="kpi-interpret">{{ kappa.interpretation }}</div>
          </div>
        </div>
        <el-alert
          v-else
          :title="`${kappa.interpretation || '数据不足'}: ${(kappa.warnings || []).join(' / ')}`"
          type="warning"
          :closable="false"
          show-icon
          style="margin-top:8px"
        >
          <template #title>
            <div style="font-size:14px">{{ kappa.interpretation || '数据不足' }}</div>
          </template>
          <div style="margin-top:4px">
            <span v-for="w in (kappa.warnings || [])" :key="w" style="display:block">{{ w }}</span>
            <div style="color:#94a3b8;font-size:12px;margin-top:8px">
              该评测没有足够评分数据(需要 ≥2 个评分员对同一回答评分)。试试下拉框选其他有数据的评测。
            </div>
          </div>
        </el-alert>
        <div style="color:#94a3b8;font-size:12px;margin-top:12px">
          总评分数: {{ kappa.totalScores }} · 评分员: {{ kappa.scorerCount }} · 回答: {{ kappa.totalAnswers }}
        </div>
      </el-card>

      <!-- 模型排名 -->
      <el-card shadow="never" style="margin-top:16px">
        <template #header>
          <span>🏆 模型排名(加权总分 = 准确性×0.4 + 相关性×0.3 + 流畅性×0.2 + 安全性×0.1)</span>
        </template>
        <el-table v-if="modelRank.length > 0" :data="modelRank" stripe>
          <el-table-column label="排名" width="80">
            <template #default="{ $index }">
              <span v-if="$index === 0">🥇</span>
              <span v-else-if="$index === 1">🥈</span>
              <span v-else-if="$index === 2">🥉</span>
              <span v-else>{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="modelName" label="模型" min-width="140" />
          <el-table-column prop="provider" label="提供商" width="100" />
          <el-table-column prop="accuracy" label="准确性" width="90" />
          <el-table-column prop="relevance" label="相关性" width="90" />
          <el-table-column prop="fluency" label="流畅性" width="90" />
          <el-table-column prop="safety" label="安全性" width="90" />
          <el-table-column prop="weightedScore" label="加权总分" width="120">
            <template #default="{ row }">
              <strong style="color:#2563eb;font-size:16px">{{ row.weightedScore }}</strong>
            </template>
          </el-table-column>
          <el-table-column prop="scoredCount" label="评分数" width="100" />
        </el-table>
        <el-empty v-else description="该评测暂无模型排名数据" :image-size="80" />
      </el-card>

      <!-- 评分员排行 -->
      <el-card shadow="never" style="margin-top:16px">
        <template #header>
          <span>👥 评分员排行</span>
        </template>
        <el-table v-if="scorerRank.length > 0" :data="scorerRank" stripe>
          <el-table-column prop="scorerId" label="ID" width="80" />
          <el-table-column prop="scoredCount" label="已评数" width="100" />
          <el-table-column prop="avgScore" label="平均分" width="120" />
          <el-table-column prop="coveragePct" label="覆盖率" width="120">
            <template #default="{ row }">
              <el-progress :percentage="row.coveragePct" :show-text="false" />
              <span style="font-size:12px;margin-left:6px">{{ row.coveragePct }}%</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="该评测暂无评分员数据" :image-size="80" />
      </el-card>

      <!-- 争议项 -->
      <el-card shadow="never" style="margin-top:16px">
        <template #header>
          <span>⚠️ 争议项(同问题评分标准差 > 1.5)</span>
        </template>
        <el-table :data="controversial" stripe v-if="controversial.length > 0">
          <el-table-column prop="answerId" label="Answer ID" width="110" />
          <el-table-column prop="questionId" label="问题 ID" width="100" />
          <el-table-column prop="modelId" label="模型 ID" width="100" />
          <el-table-column prop="std" label="标准差" width="100">
            <template #default="{ row }">
              <el-tag type="danger" size="small">{{ row.std }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="meanScore" label="平均分" width="100" />
          <el-table-column prop="scorerCount" label="评分员数" width="100" />
          <el-table-column label="各评分员打分">
            <template #default="{ row }">
              <el-tag v-for="(s, i) in row.scorerAvgs" :key="i" size="small" style="margin-right:4px">{{ s }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="没有争议项" :image-size="80" />
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.kappa-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.kpi-box {
  background: #f8fafc;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}
.kpi-label { font-size: 13px; color: #64748b; text-transform: uppercase; font-weight: 600; }
.kpi-value { font-size: 32px; font-weight: 800; margin: 8px 0; font-variant-numeric: tabular-nums; }
.kpi-bar { background: #e2e8f0; height: 6px; border-radius: 3px; overflow: hidden; margin: 8px 0; }
.kpi-bar-fill { height: 100%; transition: width 0.3s, background 0.3s; }
.kpi-interpret { font-size: 12px; color: #94a3b8; margin-top: 4px; }
</style>
