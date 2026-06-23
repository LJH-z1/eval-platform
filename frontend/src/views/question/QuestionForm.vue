<script setup>
/**
 * 问题管理 — 新建/编辑表单(共用)
 * <p>
 * 路由:
 *   /question/new       — 新建
 *   /question/:id/edit  — 编辑(加载数据预填)
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createQuestion, updateQuestion, getQuestion } from '@/api'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => route.name === 'question-edit')
const questionId = computed(() => isEdit.value ? Number(route.params.id) : null)
const loading = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  content: '',
  category: '',
  difficulty: '中等',
  type: '事实',
  expectedAnswer: '',
  isPublic: false
})

const categories = ['科学', '编程', '数学', '语文', '英语', '历史', '地理', '其他']
const types = ['事实', '推理', '创作', '代码']
const difficulties = ['简单', '中等', '困难']

const contentLen = computed(() => form.content.length)

const rules = {
  content: [
    { required: true, message: '问题内容不能为空', trigger: 'blur' },
    { min: 1, max: 4000, message: '长度需在 1-4000 字', trigger: 'blur' }
  ],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  type: [{ required: true, message: '请选择题型', trigger: 'change' }]
}

async function loadIfEdit() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const data = await getQuestion(questionId.value)
    Object.assign(form, {
      id: data.id,
      content: data.content || '',
      category: data.category || '',
      difficulty: data.difficulty || '中等',
      type: data.type || '事实',
      expectedAnswer: data.expectedAnswer || '',
      isPublic: !!data.isPublic
    })
  } catch (e) {
    ElMessage.error('加载失败:' + e.message)
    router.push({ name: 'question' })
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = { ...form }
      // 清理空字符串
      if (!payload.category) payload.category = null
      if (!payload.expectedAnswer) payload.expectedAnswer = null
      if (isEdit.value) {
        await updateQuestion(questionId.value, payload)
        ElMessage.success('更新成功')
      } else {
        delete payload.id
        await createQuestion(payload)
        ElMessage.success('创建成功')
      }
      router.push({ name: 'question' })
    } catch (e) {
      // 拦截器已弹窗
    } finally {
      submitting.value = false
    }
  })
}

function onCancel() {
  router.push({ name: 'question' })
}

onMounted(loadIfEdit)
</script>

<template>
  <div v-loading="loading" class="question-form-wrap">
    <el-card shadow="never" :header="isEdit ? '编辑问题' : '新建问题'" class="question-form-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="question-form">
        <el-form-item label="问题内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            :maxlength="4000"
            show-word-limit
            placeholder="请输入问题内容,1-4000 字,支持 Markdown"
          />
        </el-form-item>

        <el-form-item label="学科分类" prop="category">
          <el-select v-model="form.category" clearable allow-create filterable
                     placeholder="选择或输入新分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>

        <el-form-item label="难度" prop="difficulty">
          <el-radio-group v-model="form.difficulty">
            <el-radio v-for="d in difficulties" :key="d" :value="d">{{ d }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="题型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio v-for="t in types" :key="t" :value="t">{{ t }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="期望答案">
          <el-input
            v-model="form.expectedAnswer"
            type="textarea"
            :rows="3"
            :maxlength="2000"
            show-word-limit
            placeholder="可选,用于后续自动评测"
          />
        </el-form-item>

        <el-form-item label="题库归属">
          <el-switch
            v-model="form.isPublic"
            active-text="公共题库(所有人可见)"
            inactive-text="个人题库(仅自己可见)"
            inline-prompt
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">
            {{ isEdit ? '保存修改' : '创建' }}
          </el-button>
          <el-button @click="onCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="isEdit" shadow="never" style="margin-top: 16px" header="实时预览">
      <h3 style="margin-top: 0">{{ form.content || '(问题内容)' }}</h3>
      <el-space>
        <el-tag v-if="form.category" type="info">{{ form.category }}</el-tag>
        <el-tag>{{ form.difficulty }}</el-tag>
        <el-tag type="success">{{ form.type }}</el-tag>
        <el-tag v-if="form.isPublic" type="warning">公共题库</el-tag>
      </el-space>
      <el-divider />
      <div v-if="form.expectedAnswer">
        <strong>期望答案:</strong>
        <p style="color: #606266; white-space: pre-wrap">{{ form.expectedAnswer }}</p>
      </div>
      <div v-else style="color: #909399">未填写期望答案</div>
    </el-card>
  </div>
</template>

<style scoped>
/* 内容居中:外层 flex 垂直堆叠 + 水平居中,卡片限定最大宽度 */
.question-form-wrap {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 24px 16px;
}
.question-form-card {
  width: 100%;
  max-width: 880px;
}
.question-form {
  max-width: 720px;
  margin: 0 auto;
}
</style>
