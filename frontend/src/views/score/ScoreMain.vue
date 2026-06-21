<script setup>
/**
 * 评分列表页 - FR-05
 * <p>
 * 列出可评分的评测(状态=COMPLETED),显示我的评分进度
 * 路由:/score
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pageEvaluations } from '@/api'
import { getEvaluation } from '@/api'

const router = useRouter()
const loading = ref(false)
const list = ref([])

async function load() {
  loading.value = true
  try {
    const data = await pageEvaluations({ pageNum: 1, pageSize: 50, status: 'COMPLETED' })
    list.value = data.list || []
  } catch (e) {
    ElMessage.error('加载失败:' + (e?.response?.data?.message || e?.message || ''))
  } finally {
    loading.value = false
  }
}

function onScore(row) {
  router.push({ name: 'score-form', params: { id: row.id } })
}

onMounted(load)
</script>

<template>
  <div class="page-wrap">
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
      <div>
        <h2 class="page-title">⭐ 多维评分</h2>
        <p class="page-subtitle">从准确性 / 相关性 / 流畅性 / 安全性 4 个维度对模型回答评分,1-5 分制</p>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="评测名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="模型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ (row.modelIds || '').split(',').filter(Boolean).length }} 个</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="问题" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ (row.questionIds || '').split(',').filter(Boolean).length }} 题</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag type="success" size="small">已完成</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="'EditPen'" @click="onScore(row)">去评分</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="还没有已完成的评测。先在「评测任务」页创建一个并跑完。" />
    </el-card>
  </div>
</template>
