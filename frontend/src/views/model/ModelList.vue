<script setup>
/**
 * 模型配置 — 列表页
 * <p>
 * FR-02 由向锏楠实现。后端 Service/Controller 已实现,本页面调用对应接口。
 * <p>
 * 路由:/model
 */
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageModels, toggleModelStatus, deleteModel } from '@/api'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  provider: ''
})

const providerOptions = [
  { value: '',       label: '全部' },
  { value: 'M3',     label: 'M3' },
  { value: 'ZHIPU',  label: '智谱 GLM' },
  { value: 'QWEN',   label: '通义千问' },
  { value: 'WENXIN', label: '文心一言' },
  { value: 'KIMI',   label: '月之暗面' },
  { value: 'OPENAI', label: 'OpenAI' },
  { value: 'CUSTOM', label: '自定义' }
]

const providerTagType = {
  'M3': 'primary',
  'ZHIPU': 'success',
  'QWEN': 'warning',
  'WENXIN': 'danger',
  'KIMI': 'info',
  'OPENAI': 'primary',
  'CUSTOM': ''
}

async function load() {
  loading.value = true
  try {
    const data = await pageModels(query)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    ElMessage.error('加载失败:' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

async function onToggle(row) {
  const next = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(
    `确定要${next === 1 ? '启用' : '停用'}「${row.name}」吗?`,
    '提示',
    { type: 'warning' }
  )
  try {
    await toggleModelStatus(row.id, next)
    ElMessage.success(next === 1 ? '已启用' : '已停用')
    await load()
  } catch (e) {
    if (e !== 'cancel' && e?.message) ElMessage.error('操作失败:' + e.message)
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(
    `确定要删除「${row.name}」吗?此操作不可恢复。`,
    '危险操作',
    { type: 'error' }
  )
  try {
    await deleteModel(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败:' + (e?.response?.data?.message || e?.message || '该模型可能已被引用'))
  }
}

function onNew() {
  router.push({ name: 'model-new' })
}
function onEdit(row) {
  router.push({ name: 'model-edit', params: { id: row.id } })
}
function onTest(row) {
  router.push({ name: 'model-test', params: { id: row.id } })
}
function onSearch() {
  query.pageNum = 1
  load()
}
function onReset() {
  query.provider = ''
  query.pageNum = 1
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-wrap">
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
      <div>
        <h2 class="page-title">🤖 模型配置</h2>
        <p class="page-subtitle">管理多提供商模型,API Key 用 AES-256 加密存储</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="onNew">新建模型</el-button>
    </div>

    <el-card shadow="never">

      <!-- 筛选 -->
      <el-form inline @submit.prevent>
        <el-form-item label="提供商">
          <el-select v-model="query.provider" placeholder="全部" clearable style="width:160px" @change="onSearch">
            <el-option v-for="p in providerOptions" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 列表 -->
      <el-table v-loading="loading" :data="list" stripe border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="provider" label="提供商" width="110">
          <template #default="{ row }">
            <el-tag :type="providerTagType[row.provider] || 'info'" size="small">{{ row.provider }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="modelVersion" label="模型版本" min-width="140" />
        <el-table-column label="API Key" width="180">
          <template #default="{ row }">
            <code style="background:#f5f7fa;padding:2px 6px;border-radius:3px">{{ row.apiKeyMasked }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="endpoint" label="Endpoint" min-width="220" show-overflow-tooltip />
        <el-table-column label="参数" width="160">
          <template #default="{ row }">
            <span style="font-size:12px;color:#666">
              T={{ row.temperature }} · P={{ row.topP }} · {{ row.maxTokens }}t
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="pricePerK" label="单价(元/千tok)" width="120" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="onTest(row)">测试</el-button>
            <el-button size="small" link type="primary" @click="onEdit(row)">编辑</el-button>
            <el-button size="small" link :type="row.status === 1 ? 'warning' : 'success'" @click="onToggle(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button size="small" link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top:16px;justify-content:flex-end"
        @current-change="load"
        @size-change="load"
      />
    </el-card>

    <div class="hint">
      <el-alert type="info" :closable="false" show-icon>
        <template #title>
          API Key 用 AES-256 加密存储,列表展示为掩码(前 4 + **** + 后 4)。
          真实调用需 FR-04 模型适配器实现,当前连接测试用 OpenAI-compatible chat/completions 协议。
        </template>
      </el-alert>
    </div>
  </div>
</template>

<style scoped>
.hint { margin-top: 16px; }
</style>
