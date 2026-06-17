<script setup>
/**
 * 模型配置 — 新建 / 编辑共用表单
 * <p>
 * 路由:/model/new | /model/:id/edit
 * <p>
 * 业务规则:
 * - API Key 入库前自动加密,界面只显示掩码
 * - 更新时 provider 字段不允许改(后端校验)
 */
import { onMounted, reactive, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createModel, updateModel, getModel, testModelConfig } from '@/api'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)
const modelId = computed(() => route.params.id ? Number(route.params.id) : null)
const loading = ref(false)
const submitting = ref(false)
const testing = ref(false)

const form = reactive({
  name: '',
  provider: 'M3',
  apiKey: '',
  endpoint: '',
  modelVersion: '',
  temperature: 0.7,
  topP: 0.9,
  maxTokens: 2048,
  pricePerK: 0,
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  provider: [{ required: true, message: '请选择提供商', trigger: 'change' }],
  apiKey: [{ required: !isEdit.value, message: '请输入 API Key', trigger: 'blur' }],
  temperature: [{ type: 'number', min: 0, max: 1, message: '范围 0-1', trigger: 'blur' }],
  topP: [{ type: 'number', min: 0, max: 1, message: '范围 0-1', trigger: 'blur' }],
  maxTokens: [{ type: 'number', min: 1, max: 32000, message: '范围 1-32000', trigger: 'blur' }]
}

const formRef = ref()

// 不同提供商的 endpoint 模板
const endpointTemplates = {
  'M3':     'https://api.MiniMax.chat/v1/text/chatcompletion_v2',
  'OPENAI': 'https://api.openai.com/v1/chat/completions',
  'ZHIPU':  'https://open.bigmodel.cn/api/paas/v4/chat/completions',
  'QWEN':   'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions',
  'WENXIN': 'https://qianfan.baidubce.com/v2/chat/completions',
  'KIMI':   'https://api.moonshot.cn/v1/chat/completions',
  'CUSTOM': ''
}

const versionTemplates = {
  'M3': 'M3-Plus',
  'OPENAI': 'gpt-4o-mini',
  'ZHIPU': 'glm-4-plus',
  'QWEN': 'qwen-max',
  'WENXIN': 'ernie-4.0',
  'KIMI': 'moonshot-v1-128k',
  'CUSTOM': ''
}

function applyTemplate(provider) {
  if (!isEdit.value) {
    if (!form.endpoint && endpointTemplates[provider]) form.endpoint = endpointTemplates[provider]
    if (!form.modelVersion && versionTemplates[provider]) form.modelVersion = versionTemplates[provider]
  }
}

async function loadExisting() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const data = await getModel(modelId.value)
    Object.assign(form, {
      name: data.name,
      provider: data.provider,
      // 编辑模式不加载原 apiKey(避免长显示),留空表示不改
      apiKey: '',
      endpoint: data.endpoint,
      modelVersion: data.modelVersion,
      temperature: data.temperature,
      topP: data.topP,
      maxTokens: data.maxTokens,
      pricePerK: data.pricePerK,
      status: data.status
    })
  } catch (e) {
    ElMessage.error('加载失败:' + (e?.message || ''))
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    // 编辑模式下,apiKey 为空表示不改
    const payload = { ...form }
    if (isEdit.value && !payload.apiKey) {
      // 后端要求 apiKey 非空,所以编辑模式下必须填一个新值
      ElMessage.warning('请输入新的 API Key(编辑模式下必填)')
      submitting.value = false
      return
    }
    if (isEdit.value) {
      await updateModel(modelId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createModel(payload)
      ElMessage.success('创建成功')
    }
    router.push({ name: 'model' })
  } catch (e) {
    if (!e?.response?.data?.message?.includes?.('校验')) {
      ElMessage.error('保存失败:' + (e?.response?.data?.message || e?.message || ''))
    }
  } finally {
    submitting.value = false
  }
}

function onCancel() {
  router.back()
}

async function onQuickTest() {
  if (!form.apiKey) {
    ElMessage.warning('请先填写 API Key 再测试')
    return
  }
  testing.value = true
  try {
    const res = await testModelConfig({
      provider: form.provider,
      apiKey: form.apiKey,
      endpoint: form.endpoint,
      modelVersion: form.modelVersion,
      temperature: form.temperature,
      topP: form.topP,
      maxTokens: 512,
      question: '你好,请用一句话介绍下你自己。'
    })
    if (res.error) {
      ElMessageBox.alert('测试失败:' + res.error, '连接测试', { type: 'error' })
    } else {
      ElMessageBox.alert(
        `✅ 调用成功\n耗时:${res.latencyMs}ms\n\n响应内容:\n${res.response || '(空)'}`,
        '连接测试',
        { type: 'success' }
      )
    }
  } catch (e) {
    ElMessageBox.alert('请求失败:' + (e?.response?.data?.message || e?.message || ''), '连接测试', { type: 'error' })
  } finally {
    testing.value = false
  }
}

onMounted(() => {
  loadExisting()
  applyTemplate(form.provider)
})
</script>

<template>
  <div class="page-wrap-narrow">
    <h2 class="page-title">{{ isEdit ? '✏️ 编辑模型' : '➕ 新建模型' }}</h2>
    <p class="page-subtitle">填入模型信息,API Key 服务端加密存储</p>

    <el-card shadow="never">

      <el-form ref="formRef" v-loading="loading" :model="form" :rules="rules" label-width="120px" style="max-width:720px">
        <el-form-item label="模型名称" prop="name">
          <el-input v-model="form.name" placeholder="例如:M3-生产环境" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="提供商" prop="provider">
          <el-select v-model="form.provider" placeholder="请选择" :disabled="isEdit" @change="applyTemplate" style="width:100%">
            <el-option label="M3" value="M3" />
            <el-option label="智谱 GLM" value="ZHIPU" />
            <el-option label="通义千问" value="QWEN" />
            <el-option label="文心一言" value="WENXIN" />
            <el-option label="月之暗面 Moonshot" value="KIMI" />
            <el-option label="OpenAI" value="OPENAI" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
          <span class="form-tip" v-if="isEdit">提供商创建后不允许修改</span>
        </el-form-item>

        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="form.apiKey"
                    type="password"
                    show-password
                    :placeholder="isEdit ? '编辑时必填,会覆盖原值' : '明文 API Key,服务端 AES-256 加密后存储'" />
          <span class="form-tip">列表/详情仅显示掩码,密文存储于数据库</span>
        </el-form-item>

        <el-form-item label="模型版本">
          <el-input v-model="form.modelVersion" placeholder="例如:M3-Plus / gpt-4o-mini / glm-4-plus" />
        </el-form-item>

        <el-form-item label="Endpoint">
          <el-input v-model="form.endpoint" placeholder="完整 API URL,留空则不真调用" />
        </el-form-item>

        <el-form-item label="温度 (temperature)">
          <el-input-number v-model="form.temperature" :min="0" :max="1" :step="0.1" :precision="2" />
          <span class="form-tip">范围 0-1,默认 0.7</span>
        </el-form-item>

        <el-form-item label="Top-P">
          <el-input-number v-model="form.topP" :min="0" :max="1" :step="0.1" :precision="2" />
          <span class="form-tip">范围 0-1,默认 0.9</span>
        </el-form-item>

        <el-form-item label="最大输出 Token">
          <el-input-number v-model="form.maxTokens" :min="1" :max="32000" :step="256" />
        </el-form-item>

        <el-form-item label="单价(元/千tok)">
          <el-input-number v-model="form.pricePerK" :min="0" :precision="4" :step="0.001" />
          <span class="form-tip">成本统计用,可填 0</span>
        </el-form-item>

        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0"
                     active-text="启用" inactive-text="停用" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">保存</el-button>
          <el-button type="success" :loading="testing" @click="onQuickTest" plain>连接测试</el-button>
          <el-button @click="onCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.form-tip { margin-left: 12px; font-size: 12px; color: #999; }
</style>
