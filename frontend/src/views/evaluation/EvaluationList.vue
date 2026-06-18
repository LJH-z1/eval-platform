<script setup>
/**
 * 评测列表页 - FR-04
 * <p>
 * 路由:/evaluation
 */
import { onMounted, reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageEvaluations, deleteEvaluation } from '@/api'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: ''
})

const statusOptions = [
  { value: '',        label: '全部' },
  { value: 'PENDING', label: '⏳ 待运行' },
  { value: 'RUNNING', label: '🔄 运行中' },
  { value: 'COMPLETED', label: '✅ 已完成' },
  { value: 'FAILED',  label: '❌ 失败' }
]

const statusTagType = {
  'PENDING':   'info',
  'RUNNING':   'warning',
  'COMPLETED': 'success',
  'FAILED':    'danger'
}

async function load() {
  loading.value = true
  try {
    const data = await pageEvaluations(query)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    ElMessage.error('加载失败:' + (e?.response?.data?.message || e?.message || ''))
  } finally {
    loading.value = false
  }
}

function onCreate() {
  router.push({ name: 'evaluation-new' })
}
function onView(row) {
  router.push({ name: 'evaluation-detail', params: { id: row.id } })
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除评测「${row.name}」?此操作不可恢复。`, '危险操作', { type: 'error' })
  try {
    await deleteEvaluation(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败:' + (e?.response?.data?.message || e?.message || ''))
  }
}

function onSearch() {
  query.pageNum = 1
  load()
}
function onReset() {
  query.status = ''
  query.pageNum = 1
  load()
}

const completedCount = computed(() => list.value.filter(r => r.status === 'COMPLETED').length)
const runningCount   = computed(() => list.value.filter(r => r.status === 'RUNNING').length)

onMounted(load)
// 自动刷新运行中的评测
let timer = null
onMounted(() => {
  timer = setInterval(() => {
    if (list.value.some(r => r.status === 'RUNNING' || r.status === 'PENDING')) {
      load()
    }
  }, 3000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<template>
  <div class="page-wrap">
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
      <div>
        <h2 class="page-title">🚀 评测任务</h2>
        <p class="page-subtitle">
          创建评测:选模型 × 选问题,系统并发调用 · 本页共 {{ total }} 个,
          <span style="color:#16a34a">{{ completedCount }} 已完成</span> ·
          <span style="color:#d97706">{{ runningCount }} 运行中</span>
        </p>
      </div>
      <el-button type="primary" :icon="'Plus'" @click="onCreate">新建评测</el-button>
    </div>

    <el-card shadow="never">
      <el-form inline @submit.prevent>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:160px" @change="onSearch">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" stripe border style="margin-top:12px">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status] || 'info'" size="small">
              {{ statusOptions.find(s => s.value === row.status)?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="modelIds" label="模型" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ (row.modelIds || '').split(',').filter(Boolean).length }} 个</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="问题" width="90">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ (row.questionIds || '').split(',').filter(Boolean).length }} 题</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startedAt" label="开始时间" width="170" />
        <el-table-column prop="finishedAt" label="结束时间" width="170" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="onView(row)">查看</el-button>
            <el-button size="small" link type="danger" :disabled="row.status === 'RUNNING'" @click="onDelete(row)">删除</el-button>
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
  </div>
</template>

<script>
import { onUnmounted } from 'vue'
export default { name: 'EvaluationList' }
</script>
