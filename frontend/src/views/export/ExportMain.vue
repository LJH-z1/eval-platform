<script setup>
/**
 * 报告导出 - FR-08
 * <p>
 * 选择评测,在页面内在线预览 HTML 报告 + 下载 Excel/HTML
 * 路由:/export
 */
import { onMounted, ref, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { pageEvaluations } from '@/api'
import { getExportMeta, getExportHtml, downloadExcelUrl, downloadPdfUrl } from '@/api'

const evaluations = ref([])
const selectedId = ref(null)
const meta = ref(null)
const htmlContent = ref('')
const previewLoading = ref(false)
const previewExpanded = ref(false)
const iframeRef = ref(null)

onMounted(loadEvalList)

async function loadEvalList() {
  try {
    const d = await pageEvaluations({ pageNum: 1, pageSize: 50 })
    evaluations.value = d.list || d.records || []
    if (!selectedId.value && evaluations.value.length > 0) {
      selectedId.value = evaluations.value[0].id
      await onSelectChange()
    }
  } catch (e) {
    ElMessage.error('加载评测列表失败:' + (e?.message || ''))
  }
}

async function onSelectChange() {
  if (!selectedId.value) return
  meta.value = null
  htmlContent.value = ''
  previewExpanded.value = false
  await Promise.all([loadMeta(), loadPreview()])
}

async function loadMeta() {
  try {
    const r = await getExportMeta(selectedId.value)
    meta.value = r || {}
  } catch (e) {
    meta.value = null
  }
}

async function loadPreview() {
  if (!selectedId.value) return
  previewLoading.value = true
  try {
    const r = await getExportHtml(selectedId.value)
    htmlContent.value = r || ''
  } catch (e) {
    htmlContent.value = ''
    ElMessage.error('加载预览失败:' + (e?.message || ''))
  } finally {
    previewLoading.value = false
  }
}

function togglePreview() {
  previewExpanded.value = !previewExpanded.value
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

watch(selectedId, onSelectChange)
</script>

<template>
  <div class="page-wrap">
    <h2 class="page-title">📥 报告导出</h2>
    <p class="page-subtitle">在页面内直接预览完整报告(包含总览/Kappa/排名/详细结果),也可下载 Excel / HTML 离线查看</p>

    <el-card shadow="never">
      <el-form inline>
        <el-form-item label="选择评测">
          <el-select v-model="selectedId" placeholder="选择评测" style="width:420px" filterable no-data-text="暂无评测数据">
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
          <h4>下载 Excel(.csv)</h4>
          <p>包含总览、Kappa、模型成本、详细结果 — Excel 可直接打开</p>
          <el-button type="primary" :icon="'Download'">下载 Excel</el-button>
        </el-card>
        <el-card shadow="never" class="dl-card" @click="onDownloadPdf">
          <div class="dl-icon">🌐</div>
          <h4>下载 HTML 报告</h4>
          <p>美化的网页报告 — 浏览器/Word 可查看,可打印为 PDF</p>
          <el-button type="success" :icon="'Download'">下载 HTML</el-button>
        </el-card>
      </div>
    </el-card>

    <!-- 在线预览 -->
    <el-card shadow="never" style="margin-top:16px" v-if="selectedId">
      <template #header>
        <div class="preview-header">
          <span>👀 报告在线预览</span>
          <div class="preview-actions">
            <el-tag v-if="htmlContent" type="info" size="small">{{ (htmlContent.length / 1024).toFixed(1) }} KB</el-tag>
            <el-button v-if="htmlContent" size="small" @click="togglePreview">
              {{ previewExpanded ? '收起' : '展开' }}
            </el-button>
            <el-button v-if="htmlContent && previewExpanded" type="primary" size="small" @click="onDownloadPdf">下载</el-button>
          </div>
        </div>
      </template>

      <div v-if="previewLoading" class="preview-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>正在加载报告内容...</span>
      </div>

      <div v-else-if="!htmlContent" class="preview-empty">
        <el-empty description="暂无报告内容" />
      </div>

      <div v-else class="preview-wrap" :class="{ expanded: previewExpanded }">
        <iframe
          ref="iframeRef"
          :srcdoc="htmlContent"
          class="preview-iframe"
          sandbox="allow-same-origin"
          title="报告预览"
        ></iframe>
      </div>
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

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.preview-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
.preview-loading,
.preview-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #94a3b8;
  font-size: 14px;
  gap: 8px;
}
.preview-wrap {
  width: 100%;
  height: 360px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  transition: height .25s;
}
.preview-wrap.expanded { height: 80vh; min-height: 600px; }
.preview-iframe {
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
}
</style>
