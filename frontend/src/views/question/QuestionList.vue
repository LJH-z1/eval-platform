<script setup>
/**
 * 问题管理 — 列表页
 * <p>
 * 由【向锏楠(后端)+ 靳磊(前端)】实现。本分支向锏楠做了基础前端骨架 + 接口对接。
 * <p>
 * 路由:/question
 */
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageQuestions, deleteQuestion } from '@/api'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  category: '',
  type: '',
  difficulty: '',
  keyword: ''
})

const categories = ['科学', '编程', '数学', '语文', '英语', '历史', '地理', '其他']
const types = ['事实', '推理', '创作', '代码']
const difficulties = ['简单', '中等', '困难']

const difficultyTagType = {
  '简单': 'success',
  '中等': 'warning',
  '困难': 'danger'
}

const typeTagType = {
  '事实': 'info',
  '推理': 'primary',
  '创作': 'success',
  '代码': 'warning'
}

async function load() {
  loading.value = true
  try {
    const data = await pageQuestions(query)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onCreate() {
  router.push({ name: 'question-new' })
}

function onEdit(row) {
  router.push({ name: 'question-edit', params: { id: row.id } })
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除问题"${row.content.substring(0, 20)}..."吗?`,
      '提示',
      { type: 'warning' }
    )
    await deleteQuestion(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    if (e !== 'cancel' && e?.message) ElMessage.error(e.message)
  }
}

function onReset() {
  query.category = ''
  query.type = ''
  query.difficulty = ''
  query.keyword = ''
  query.pageNum = 1
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <el-card shadow="never">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="分类">
          <el-select v-model="query.category" clearable placeholder="全部" style="width: 140px">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="query.type" clearable placeholder="全部" style="width: 140px">
            <el-option v-for="t in types" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="query.difficulty" clearable placeholder="全部" style="width: 140px">
            <el-option v-for="d in difficulties" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="搜索问题/期望答案" clearable
                    style="width: 220px" @keyup.enter="query.pageNum = 1; load()" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="query.pageNum = 1; load()">查询</el-button>
          <el-button @click="onReset">重置</el-button>
          <el-button type="success" :icon="'Plus'" @click="onCreate">新建问题</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px" v-loading="loading">
      <el-table :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="问题内容" min-width="280">
          <template #default="{ row }">
            <el-tooltip :content="row.content" placement="top" :show-after="300">
              <span style="display: inline-block; max-width: 100%; overflow: hidden;
                           text-overflow: ellipsis; white-space: nowrap;">
                {{ row.content }}
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="难度" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.difficulty" :type="difficultyTagType[row.difficulty] || 'info'" size="small">
              {{ row.difficulty }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="题型" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.type" :type="typeTagType[row.type] || 'info'" size="small">
              {{ row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="期望答案" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span style="color: #909399">{{ row.expectedAnswer || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="公开" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isPublic" type="success" size="small">公共</el-tag>
            <el-tag v-else type="info" size="small">个人</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="onEdit(row)">编辑</el-button>
            <el-button type="danger"  link size="small" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 16px; text-align: right"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="load"
        @size-change="load"
      />
    </el-card>
  </div>
</template>
