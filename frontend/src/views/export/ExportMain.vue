<script setup>
/**
 * 报告导出 - FR-08
 * <p>
 * 选择评测,下载 Excel/HTML 报告
 * 路由:/export
 */
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pageEvaluations } from '@/api'
import { getExportMeta, downloadExcelUrl, downloadPdfUrl } from '@/api'

const evaluations = ref([])
const selectedId = ref(null)
const meta = ref(null)

onMounted(loadEvalList)

async function loadEvalList() {
  try {
    const d = await pageEvaluations({ pageNum: 1, pageSize: 50 })
    evaluations.value = d.list || d.records || []
    if (!selectedId.value && evaluations.value.length > 0) {
      selectedId.value = evaluations.value[0].id
      await loadMeta()
    }
  } catch (e) {
    ElMessage.error('加载评测列表失败:' + (e?.message || ''))
  }
}

async function loadMeta() {
  if (!selectedId.value) return
  try {
    const r = await getExportMeta(selectedId.value)
    meta.value = r || {}
  } catch (e) {
    meta.value = null
  }
}

async function onDownloadExcel() {
  if (!selectedId.value) return ElMessage.warning('请选择评测')
  try {
    await downloadExcelUrl(selectedId.value)
    ElMessage.success('已下载 Excel(CSV 格式)')
  } catch (e) {
    ElMessage.error('下载失败:' + (e?.message || ''))
  }
}

async function onDownloadPdf() {
  if (!selectedId.value) return ElMessage.warning('请选择评测')
  try {
    await downloadPdfUrl(selectedId.value)
    ElMessage.success('已下载 HTML 报告')
  } catch (e) {
    ElMessage.error('下载失败:' + (e?.message || ''))
  }
}
</script>

<template>
  <div class="page-wrap">
    <h2 class="page-title">📥 报告导出</h2>
    <p class="page-subtitle">下载评测的完整报告(包含总览/Kappa/排名/详细结果)</p>

    <el-card shadow="never">
      <el-form inline>
        <el-form-item label="选择评测">
          <el-select v-model="selectedId" placeholder="选择评测" style="width:420px" filterable @change="loadMeta" no-data-text="暂无评测数据">
            <el-option v-for="e in evaluations" :key="e.id" :label="`#${e.id} · ${e.name}`" :value="e.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <div v-if="meta" class="meta-card">
        <h3>{{ meta.name }}</h3>
        <p class="meta-info">
          状态: <el-tag size="small">{{ meta.status }}</el-tag>
          · 创建: {{ meta.createdAt }}
        </p>
      </div>

      <div class="download-grid" v-if="selectedId">
        <el-card shadow="never" class="dl-card" @click="onDownloadExcel">
          <div class="dl-icon">📊</div>
          <h4>Excel 报告(.csv)</h4>
          <p>包含总览、Kappa、模型成本、详细结果 — Excel 可直接打开</p>
          <el-button type="primary">下载</el-button>
        </el-card>
        <el-card shadow="never" class="dl-card" @click="onDownloadPdf">
          <div class="dl-icon">🌐</div>
          <h4>HTML 报告</h4>
          <p>美化的网页报告 — 浏览器/Word 可查看,可打印为 PDF</p>
          <el-button type="success">下载</el-button>
        </el-card>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top:16px" v-if="selectedId">
      <template #header><span>📄 报告内容预览</span></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="第 1 章">📈 评测总览(总调用 / Token / 费用 / 延迟)</el-descriptions-item>
        <el-descriptions-item label="第 2 章">🎯 评分一致性(Fleiss Kappa 4 维度)</el-descriptions-item>
        <el-descriptions-item label="第 3 章">💰 各模型成本对比</el-descriptions-item>
        <el-descriptions-item label="第 4 章">🏆 模型排名(加权总分)</el-descriptions-item>
        <el-descriptions-item label="第 5 章">📝 详细结果(每条 answer × 模型 × 评分)</el-descriptions-item>
        <el-descriptions-item label="第 6 章">📌 报告说明 + 解读指南</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<style scoped>
.meta-card {
  background: #f8fafc;
  border-radius: 8px;
  padding: 16px;
  margin: 16px 0;
}
.meta-card h3 { margin: 0 0 8px; color: #1e293b; }
.meta-info { color: #94a3b8; font-size: 13px; margin: 0; }
.download-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-top: 16px;
}
.dl-card {
  cursor: pointer;
  text-align: center;
  padding: 24px 16px;
  transition: all .2s;
}
.dl-card:hover {
  border-color: #2563eb;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(15,23,42,.08);
}
.dl-icon { font-size: 48px; margin-bottom: 8px; }
.dl-card h4 { margin: 0 0 6px; }
.dl-card p { color: #94a3b8; font-size: 13px; margin: 0 0 12px; }
</style>
