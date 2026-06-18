<script setup>
/**
 * 新建评测 - FR-04
 * <p>
 * 路由:/evaluation/new
 * <p>
 * 选模型(已启用) + 选问题(支持关键字搜索),提交创建
 */
import { onMounted, reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listEnabledModels, pageModels } from '@/api'
import { pageQuestions } from '@/api'
import { createEvaluation, runEvaluation } from '@/api'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const step = ref(1)  // 1=填名字+选模型 2=选问题 3=确认

const form = reactive({
  name: '',
  description: '',
  modelIds: [],
  questionIds: []
})

const rules = {
  name: [{ required: true, message: '请输入评测名称', trigger: 'blur' }],
  modelIds: [{ type: 'array', required: true, min: 1, message: '至少选择 1 个模型', trigger: 'change' }],
  questionIds: [{ type: 'array', required: true, min: 1, message: '至少选择 1 个问题', trigger: 'change' }]
}

const models = ref([])
const questions = ref([])
const modelLoading = ref(false)
const questionLoading = ref(false)

const modelQuery = reactive({ pageNum: 1, pageSize: 50, provider: '' })
const questionQuery = reactive({ pageNum: 1, pageSize: 20, keyword: '' })

async function loadModels() {
  modelLoading.value = true
  try {
    const data = await pageModels({ pageNum: 1, pageSize: 100 })
    models.value = (data.list || []).filter(m => m.status === 1)
  } catch (e) {
    ElMessage.error('加载模型失败:' + (e?.message || ''))
  } finally {
    modelLoading.value = false
  }
}

async function loadQuestions() {
  questionLoading.value = true
  try {
    const data = await pageQuestions({ pageNum: questionQuery.pageNum, pageSize: questionQuery.pageSize, keyword: questionQuery.keyword })
    questions.value = data.list || []
  } catch (e) {
    ElMessage.error('加载问题失败:' + (e?.message || ''))
  } finally {
    questionLoading.value = false
  }
}

function next() {
  if (step.value === 1) {
    formRef.value?.validateField(['name', 'modelIds']).then(() => {
      step.value = 2
      if (questions.value.length === 0) loadQuestions()
    }).catch(() => {})
  } else if (step.value === 2) {
    if (form.questionIds.length === 0) {
      ElMessage.warning('请至少选择 1 个问题')
      return
    }
    if (form.questionIds.length > 50) {
      ElMessage.warning('单次评测问题数不能超过 50')
      return
    }
    step.value = 3
  }
}
function prev() {
  if (step.value > 1) step.value--
}

async function onSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const e = await createEvaluation({
      name: form.name,
      description: form.description,
      modelIds: form.modelIds,
      questionIds: form.questionIds
    })
    ElMessage.success('创建成功')
    // 立即启动
    try {
      await runEvaluation(e.id)
      ElMessage.success('已启动评测')
    } catch (err) {
      ElMessage.warning('创建成功,但启动失败:' + (err?.response?.data?.message || err?.message || ''))
    }
    router.push({ name: 'evaluation-detail', params: { id: e.id } })
  } catch (e) {
    ElMessage.error('创建失败:' + (e?.response?.data?.message || e?.message || ''))
  } finally {
    submitting.value = false
  }
}

function onCancel() {
  router.back()
}

function selectAllQuestions() {
  form.questionIds = questions.value.map(q => q.id)
  ElMessage.success(`已选 ${form.questionIds.length} 题`)
}
function clearQuestions() {
  form.questionIds = []
}

const selectedModelNames = computed(() => {
  return form.modelIds.map(id => models.value.find(m => m.id === id)?.name).filter(Boolean)
})

onMounted(() => {
  loadModels()
})
</script>

<template>
  <div class="page-wrap-narrow">
    <h2 class="page-title">➕ 新建评测</h2>
    <p class="page-subtitle">选模型 × 选问题,系统并发调用各模型给出回答</p>

    <el-card shadow="never">
      <el-steps :active="step - 1" finish-status="success" simple style="margin-bottom:24px">
        <el-step title="基本信息" />
        <el-step title="选择问题" />
        <el-step title="确认提交" />
      </el-steps>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <!-- 步骤 1: 名称 + 描述 + 模型 -->
        <template v-if="step === 1">
          <el-form-item label="评测名称" prop="name">
            <el-input v-model="form.name" placeholder="例如:M3 vs Qwen 对比评测" maxlength="100" show-word-limit />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选,描述这次评测的目的" maxlength="500" show-word-limit />
          </el-form-item>
          <el-form-item label="选择模型" prop="modelIds">
            <el-table
              :data="models"
              v-loading="modelLoading"
              border
              max-height="360"
              @selection-change="sel => form.modelIds = sel.map(s => s.id)"
            >
              <el-table-column type="selection" width="50" />
              <el-table-column prop="name" label="名称" min-width="140" />
              <el-table-column prop="provider" label="提供商" width="120" />
              <el-table-column prop="modelVersion" label="模型版本" min-width="140" />
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag v-if="row.status === 1" type="success" size="small">启用</el-tag>
                </template>
              </el-table-column>
            </el-table>
            <div style="margin-top:6px;font-size:12px;color:#94a3b8">
              提示:勾选要对比的模型,已选 {{ form.modelIds.length }} 个(建议至少 2 个做对比)
            </div>
          </el-form-item>
        </template>

        <!-- 步骤 2: 选择问题 -->
        <template v-if="step === 2">
          <el-form-item label="搜索问题">
            <el-input v-model="questionQuery.keyword" placeholder="按问题内容/分类搜索" clearable @keyup.enter="loadQuestions">
              <template #append>
                <el-button @click="loadQuestions">搜索</el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="问题列表" prop="questionIds">
            <div style="display:flex;gap:8px;margin-bottom:8px">
              <el-button size="small" @click="selectAllQuestions">全选当前页</el-button>
              <el-button size="small" @click="clearQuestions">清空</el-button>
              <span style="font-size:12px;color:#94a3b8;align-self:center">已选 {{ form.questionIds.length }} 题(上限 50)</span>
            </div>
            <el-table
              :data="questions"
              v-loading="questionLoading"
              border
              max-height="360"
              @selection-change="sel => form.questionIds = sel.map(s => s.id)"
            >
              <el-table-column type="selection" width="50" :reserve-selection="true" />
              <el-table-column prop="id" label="ID" width="60" />
              <el-table-column prop="content" label="问题" min-width="280" show-overflow-tooltip />
              <el-table-column prop="category" label="分类" width="80" />
              <el-table-column prop="difficulty" label="难度" width="70" />
              <el-table-column prop="type" label="题型" width="70" />
            </el-table>
          </el-form-item>
        </template>

        <!-- 步骤 3: 确认 -->
        <template v-if="step === 3">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="评测名称">{{ form.name }}</el-descriptions-item>
            <el-descriptions-item label="描述">{{ form.description || '(无)' }}</el-descriptions-item>
            <el-descriptions-item label="模型数">{{ form.modelIds.length }} 个 — {{ selectedModelNames.join(', ') }}</el-descriptions-item>
            <el-descriptions-item label="问题数">{{ form.questionIds.length }} 题</el-descriptions-item>
            <el-descriptions-item label="预计耗时">视模型响应速度,通常 5-30 秒/题</el-descriptions-item>
          </el-descriptions>
          <el-alert type="info" :closable="false" show-icon style="margin-top:16px">
            提交后将立即创建并启动评测,可在「评测详情」页查看实时进度。
          </el-alert>
        </template>

        <el-form-item>
          <el-button v-if="step > 1" @click="prev">上一步</el-button>
          <el-button v-if="step < 3" type="primary" @click="next">下一步</el-button>
          <el-button v-if="step === 3" type="primary" :loading="submitting" @click="onSubmit">提交并启动</el-button>
          <el-button @click="onCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
