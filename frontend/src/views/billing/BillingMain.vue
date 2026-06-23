<script setup>
/**
 * 成本统计 - FR-07
 * <p>
 * 显示某评测的总成本 + 各模型对比 + 时序图
 * 路由:/billing
 */
import { onMounted, ref } from 'vue'
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
    selectedId.value = evaluations.value[0].id
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
    summary.value = s.data
    byModel.value = m.data || []
    timeSeries.value = t.data
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

onMounted(() => {
  loadEvalList()
  loadPlatform()
})
</script>

<template>
  <div class="page-wrap">
    <h2 class="page-title">💰 成本统计</h2>
    <p class="page-subtitle">按评测 / 模型 / 时间维度聚合 token、费用、延迟</p>

    <!-- 平台级总览 -->
    <el-card v-if="platform" shadow="never" class="platform-card">
      <template #header><span>🌐 平台总览</span></template>
      <div class="stat-grid">
        <div class="stat-box">
          <div class="num">{{ platform.totalCalls }}</div>
          <div class="label">总调用数</div>
        </div>
        <div class="stat-box">
          <div class="num">{{ platform.totalTokens }}</div>
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
      <el-card shadow="never" style="margin-top:16px">
        <template #header><span>📊 评测总览</span></template>
        <div class="stat-grid">
          <div class="stat-box"><div class="num">{{ summary.totalCalls }}</div><div class="label">总调用</div></div>
          <div class="stat-box"><div class="num">{{ summary.totalInputTokens }}</div><div class="label">输入 Token</div></div>
          <div class="stat-box"><div class="num">{{ summary.totalOutputTokens }}</div><div class="label">输出 Token</div></div>
          <div class="stat-box"><div class="num">¥{{ summary.totalCost }}</div><div class="label">总费用</div></div>
          <div class="stat-box"><div class="num">{{ summary.avgLatencyMs }}ms</div><div class="label">平均耗时</div></div>
          <div class="stat-box"><div class="num">{{ summary.successCalls }}</div><div class="label">成功</div></div>
          <div class="stat-box"><div class="num" style="color:#dc2626">{{ summary.failCalls }}</div><div class="label">失败</div></div>
        </div>
      </el-card>

      <el-card shadow="never" style="margin-top:16px">
        <template #header><span>📈 各模型成本对比</span></template>
        <el-table :data="byModel" stripe>
          <el-table-column prop="modelName" label="模型" min-width="140" />
          <el-table-column prop="provider" label="提供商" width="100" />
          <el-table-column prop="calls" label="调用" width="80" />
          <el-table-column prop="inputTokens" label="输入 Token" width="120" />
          <el-table-column prop="outputTokens" label="输出 Token" width="120" />
          <el-table-column prop="totalTokens" label="总 Token" width="120" />
          <el-table-column prop="cost" label="费用" width="100">
            <template #default="{ row }">¥{{ row.cost }}</template>
          </el-table-column>
          <el-table-column prop="avgLatencyMs" label="平均延迟" width="120">
            <template #default="{ row }">{{ row.avgLatencyMs }} ms</template>
          </el-table-column>
        </el-table>
      </el-card>

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

    <el-empty v-if="!loading && evaluations.length === 0" description="还没有评测" />
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
