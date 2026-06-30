<script setup>
/**
 * 成本统计 - FR-07
 * <p>
 * 平台总览 + 评测总览 + 各模型成本对比 + 时序图 + 关键洞察 + 效率排行 + 节省建议 + Token 结构
 * 路由:/billing
 */
import { onMounted, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { pageEvaluations } from '@/api'
import { getBillingSummary, getBillingByModel, getBillingTimeSeries, getBillingPlatform, downloadBillingCsv } from '@/api'

const loading = ref(false)
const evaluations = ref([])
const selectedId = ref(null)
const summary = ref(null)
const byModel = ref([])
const timeSeries = ref(null)
const platform = ref(null)

async function loadEvalList() {
  const d = await pageEvaluations({ pageNum: 1, pageSize: 50 })
  evaluations.value = d.list || []
  if (!selectedId.value && evaluations.value.length > 0) {
    // 选第一条有调用数据的评测(MOCK 模式 cost=0,所以看 calls)
    for (const ev of evaluations.value) {
      try {
        const r = await getBillingSummary(ev.id)
        if (r && r.totalCalls > 0) {
          selectedId.value = ev.id
          break
        }
      } catch (_) {}
    }
    if (!selectedId.value) selectedId.value = evaluations.value[0].id
    await loadStats()
  }
}

async function loadStats() {
  if (!selectedId.value) return
  loading.value = true
  try {
    const [s, m, t] = await Promise.all([
      getBillingSummary(selectedId.value),
      getBillingByModel(selectedId.value),
      getBillingTimeSeries(selectedId.value, 'hour')
    ])
    // axios 拦截器已把 {code,data,message} 解包,这里 r 就是真正的业务对象
    summary.value = s || null
    byModel.value = Array.isArray(m) ? m : []
    timeSeries.value = t || null
  } catch (e) {
    ElMessage.error('加载失败:' + (e?.response?.data?.message || e?.message))
  } finally {
    loading.value = false
  }
}

async function loadPlatform() {
  try {
    const r = await getBillingPlatform()
    platform.value = r || {}
  } catch (_) {}
}

async function onDownload() {
  if (!selectedId.value) return
  try {
    await downloadBillingCsv(selectedId.value)
    ElMessage.success('已下载 CSV')
  } catch (e) {
    ElMessage.error('下载失败:' + (e?.message || ''))
  }
}

// === 派生指标 ===
const sortedByCost = computed(() =>
  [...byModel.value].sort((a, b) => (b.cost || 0) - (a.cost || 0))
)
const efficiencyRank = computed(() => {
  return byModel.value
    .filter(m => m.calls > 0 && m.totalTokens > 0)
    .map(m => ({
      ...m,
      costPerCall: m.cost / m.calls,
      costPer1kToken: (m.cost / m.totalTokens) * 1000,
      latencyPerCall: m.avgLatencyMs
    }))
    .sort((a, b) => a.costPer1kToken - b.costPer1kToken)
})
const insights = computed(() => {
  if (byModel.value.length === 0) return []
  const list = sortedByCost.value
  const expensive = list[0]
  const cheapest = list[list.length - 1]
  const totalCost = list.reduce((s, m) => s + (m.cost || 0), 0)
  const totalTokens = list.reduce((s, m) => s + (m.totalTokens || 0), 0)
  const totalCalls = list.reduce((s, m) => s + (m.calls || 0), 0)
  const inTok = summary.value?.totalInputTokens || 0
  const outTok = summary.value?.totalOutputTokens || 0
  const result = []
  if (expensive) {
    result.push({
      icon: '🔥',
      type: 'warning',
      title: '最贵模型',
      text: `${expensive.modelName} — 共 ¥${expensive.cost?.toFixed(2)}(${list[0] ? Math.round((expensive.cost / totalCost) * 100) : 0}% 总费用)`
    })
  }
  if (cheapest && cheapest !== expensive) {
    result.push({
      icon: '💎',
      type: 'success',
      title: '最便宜模型',
      text: `${cheapest.modelName} — 单千 token 仅 ¥${((cheapest.cost / cheapest.totalTokens) * 1000).toFixed(4)}`
    })
  }
  if (expensive && cheapest && expensive !== cheapest && expensive.calls > 0 && cheapest.calls > 0) {
    const saved = expensive.cost - (expensive.calls * (cheapest.cost / cheapest.calls))
    if (saved > 0.01) {
      result.push({
        icon: '🧮',
        type: 'info',
        title: '节省潜力',
        text: `如果把「${expensive.modelName}」替换成「${cheapest.modelName}」(同等调用次数),可省 ¥${saved.toFixed(2)}(约 ${Math.round((saved / totalCost) * 100)}%)`
      })
    }
  }
  if (totalCalls > 0) {
    result.push({
      icon: '📊',
      type: 'primary',
      title: '平均成本',
      text: `每千次调用 ¥${((totalCost / totalCalls) * 1000).toFixed(2)} · 每千 token ¥${totalTokens > 0 ? ((totalCost / totalTokens) * 1000).toFixed(4) : '—'}`
    })
  }
  if (totalTokens > 0) {
    const inPct = Math.round((inTok / totalTokens) * 100)
    result.push({
      icon: '🍩',
      type: 'info',
      title: 'Token 结构',
      text: `输入 ${inPct}% / 输出 ${100 - inPct}%${inTok > outTok ? '  · 提示型任务(输入多)' : outTok > inTok * 2 ? '  · 生成型任务(输出多)' : ''}`
    })
  }
  return result
})

onMounted(() => {
  loadEvalList()
  loadPlatform()
})
</script>

<template>
  <div class="page-wrap">
    <h2 class="page-title">💰 成本统计</h2>
    <p class="page-subtitle">按平台 / 评测 / 模型 / 时间维度聚合 token、费用、延迟 — 附效率排行与节省建议</p>

    <!-- 1. 平台级总览 -->
    <el-card v-if="platform" shadow="never" class="platform-card">
      <template #header><span>🌐 平台总览(所有评测累计)</span></template>
      <div class="stat-grid">
        <div class="stat-box">
          <div class="num">{{ platform.totalCalls }}</div>
          <div class="label">总调用数</div>
        </div>
        <div class="stat-box">
          <div class="num">{{ platform.totalTokens.toLocaleString() }}</div>
          <div class="label">总 Token</div>
        </div>
        <div class="stat-box">
          <div class="num">¥{{ platform.totalCost }}</div>
          <div class="label">总费用</div>
        </div>
      </div>
    </el-card>

    <el-card v-if="evaluations.length > 0" shadow="never" style="margin-top:16px">
      <el-form inline>
        <el-form-item label="评测">
          <el-select v-model="selectedId" placeholder="选择评测" style="width:340px" filterable @change="loadStats">
            <el-option v-for="e in evaluations" :key="e.id" :label="`#${e.id} · ${e.name}`" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadStats">刷新</el-button>
          <el-button @click="onDownload">下载 CSV</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <template v-if="summary">
      <!-- 2. 评测总览(7 指标) -->
      <el-card shadow="never" style="margin-top:16px">
        <template #header><span>📊 评测总览</span></template>
        <div class="stat-grid">
          <div class="stat-box"><div class="num">{{ summary.totalCalls }}</div><div class="label">总调用</div></div>
          <div class="stat-box"><div class="num">{{ summary.totalInputTokens.toLocaleString() }}</div><div class="label">输入 Token</div></div>
          <div class="stat-box"><div class="num">{{ summary.totalOutputTokens.toLocaleString() }}</div><div class="label">输出 Token</div></div>
          <div class="stat-box highlight"><div class="num">¥{{ summary.totalCost }}</div><div class="label">总费用</div></div>
          <div class="stat-box"><div class="num">{{ summary.avgLatencyMs }}ms</div><div class="label">平均耗时</div></div>
          <div class="stat-box success"><div class="num">{{ summary.successCalls }}</div><div class="label">成功</div></div>
          <div class="stat-box danger"><div class="num">{{ summary.failCalls }}</div><div class="label">失败</div></div>
        </div>
      </el-card>

      <!-- 3. 关键洞察(自动算) -->
      <el-card v-if="insights.length" shadow="never" style="margin-top:16px" class="insight-card">
        <template #header><span>💡 关键洞察(自动分析)</span></template>
        <div class="insight-grid">
          <div v-for="(it, i) in insights" :key="i" class="insight-box" :class="it.type">
            <div class="insight-icon">{{ it.icon }}</div>
            <div class="insight-body">
              <div class="insight-title">{{ it.title }}</div>
              <div class="insight-text">{{ it.text }}</div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 4. 各模型成本对比 -->
      <el-card shadow="never" style="margin-top:16px">
        <template #header><span>📈 各模型成本对比</span></template>
        <el-table :data="byModel" stripe>
          <el-table-column prop="modelName" label="模型" min-width="140" />
          <el-table-column prop="provider" label="提供商" width="100" />
          <el-table-column prop="calls" label="调用" width="80" />
          <el-table-column prop="inputTokens" label="输入 Token" width="110" />
          <el-table-column prop="outputTokens" label="输出 Token" width="110" />
          <el-table-column prop="totalTokens" label="总 Token" width="110" />
          <el-table-column label="费用占比" width="180">
            <template #default="{ row }">
              <div class="cost-bar">
                <div class="cost-bar-fill" :style="{
                  width: (byModel.reduce((s,m)=>s+(m.cost||0),0) > 0
                          ? (row.cost / byModel.reduce((s,m)=>s+(m.cost||0),0) * 100) : 0) + '%'
                }">
                  <span>¥{{ row.cost }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="avgLatencyMs" label="平均延迟" width="110">
            <template #default="{ row }">{{ row.avgLatencyMs }} ms</template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 5. 成本效率排行(单次 / 单千 token) -->
      <el-card v-if="efficiencyRank.length" shadow="never" style="margin-top:16px">
        <template #header><span>🏆 成本效率排行(单千 token 越低越便宜)</span></template>
        <el-table :data="efficiencyRank" stripe>
          <el-table-column label="排名" width="60">
            <template #default="{ $index }">
              <span v-if="$index === 0">🥇</span>
              <span v-else-if="$index === 1">🥈</span>
              <span v-else-if="$index === 2">🥉</span>
              <span v-else>{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="modelName" label="模型" min-width="140" />
          <el-table-column prop="calls" label="调用" width="80" />
          <el-table-column label="单次成本" width="120">
            <template #default="{ row }">¥{{ row.costPerCall.toFixed(4) }}</template>
          </el-table-column>
          <el-table-column label="单千 token" width="140">
            <template #default="{ row }">
              <strong style="color:#16a34a">¥{{ row.costPer1kToken.toFixed(4) }}</strong>
            </template>
          </el-table-column>
          <el-table-column label="平均延迟" width="120">
            <template #default="{ row }">{{ row.latencyPerCall }} ms</template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 6. 调用时序 -->
      <el-card shadow="never" style="margin-top:16px" v-if="timeSeries && timeSeries.xAxis && timeSeries.xAxis.length > 0">
        <template #header><span>⏰ 调用时序(按小时)</span></template>
        <div class="ts-chart">
          <div v-for="(x, i) in timeSeries.xAxis" :key="i" class="ts-bar">
            <div class="ts-label">{{ x }}</div>
            <div class="ts-row">
              <div class="ts-bar-fill call" :style="{ width: Math.min(100, (timeSeries.series[0].data[i] || 0) * 5) + '%' }">
                {{ timeSeries.series[0].data[i] || 0 }} 次
              </div>
            </div>
            <div class="ts-row">
              <div class="ts-bar-fill cost" :style="{ width: Math.min(100, (timeSeries.series[1].data[i] || 0) * 500) + '%' }">
                ¥{{ timeSeries.series[1].data[i] || 0 }}
              </div>
            </div>
          </div>
        </div>
        <div style="margin-top:12px;font-size:12px;color:#94a3b8">
          <span class="legend call"></span> 调用量 &nbsp;&nbsp; <span class="legend cost"></span> 费用(元)
        </div>
      </el-card>
    </template>

    <!-- 没选评测 / 选了但无数据 -->
    <el-card v-if="!loading && evaluations.length === 0" shadow="never" style="margin-top:16px">
      <el-empty description="还没有评测 · 请先在「评测任务」创建一个" />
    </el-card>

    <el-card v-else-if="!loading && !summary" shadow="never" style="margin-top:16px">
      <el-empty :description="`评测 #${selectedId} 暂无成本数据(answer 表为空) — 换一个评测或跑一次试试`">
        <el-button type="primary" @click="loadStats" style="margin-top:8px">重新加载</el-button>
      </el-empty>
    </el-card>
  </div>
</template>

<style scoped>
.platform-card { background: linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%); }
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}
.stat-box {
  background: #fff;
  padding: 16px 12px;
  border-radius: 8px;
  text-align: center;
  border: 1px solid #e2e8f0;
}
.stat-box .num { font-size: 24px; font-weight: 800; color: #2563eb; }
.stat-box .label { font-size: 12px; color: #94a3b8; margin-top: 4px; }
.stat-box.highlight { background: linear-gradient(135deg, #eff6ff 0%, #fff 100%); border-color: #93c5fd; }
.stat-box.success .num { color: #16a34a; }
.stat-box.danger .num { color: #dc2626; }

/* 关键洞察 */
.insight-card { background: linear-gradient(135deg, #fefce8 0%, #fff 100%); }
.insight-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 12px;
}
.insight-box {
  background: #fff;
  padding: 14px 16px;
  border-radius: 8px;
  border-left: 4px solid #94a3b8;
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.insight-box.warning { border-color: #f59e0b; }
.insight-box.success { border-color: #16a34a; }
.insight-box.info    { border-color: #3b82f6; }
.insight-box.primary { border-color: #7c3aed; }
.insight-icon { font-size: 28px; line-height: 1; flex-shrink: 0; }
.insight-body { flex: 1; min-width: 0; }
.insight-title { font-size: 13px; font-weight: 700; color: #1e293b; margin-bottom: 4px; }
.insight-text  { font-size: 13px; color: #475569; line-height: 1.5; }

/* 费用占比条 */
.cost-bar {
  background: #f1f5f9;
  border-radius: 4px;
  height: 22px;
  overflow: hidden;
  min-width: 80px;
}
.cost-bar-fill {
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 8px;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  min-width: 50px;
  white-space: nowrap;
  border-radius: 4px;
  transition: width .3s;
}

/* 时序图 */
.ts-chart { display: flex; flex-direction: column; gap: 8px; }
.ts-bar { padding: 8px; background: #f8fafc; border-radius: 6px; }
.ts-label { font-size: 12px; color: #475569; margin-bottom: 4px; font-weight: 600; }
.ts-row { display: flex; margin-bottom: 4px; }
.ts-bar-fill { padding: 4px 8px; border-radius: 4px; font-size: 11px; color: #fff; min-width: 60px; }
.ts-bar-fill.call { background: #3b82f6; }
.ts-bar-fill.cost { background: #10b981; }
.legend { display: inline-block; width: 10px; height: 10px; border-radius: 2px; vertical-align: middle; }
.legend.call { background: #3b82f6; }
.legend.cost { background: #10b981; }
</style>
