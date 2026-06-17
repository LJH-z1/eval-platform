<script setup>
/**
 * 模型连接测试页(独立路由)— 用于已存在模型测试
 * 路由:/model/:id/test
 */
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getModel, testModel } from '@/api'

const route = useRoute()
const router = useRouter()
const modelId = Number(route.params.id)
const model = ref(null)
const loading = ref(false)
const testing = ref(false)
const question = ref('你好,请用一句话介绍下你自己。')
const result = ref(null)

async function load() {
  loading.value = true
  try {
    model.value = await getModel(modelId)
  } catch (e) {
    ElMessage.error('加载失败:' + (e?.message || ''))
  } finally {
    loading.value = false
  }
}

async function onTest() {
  if (!question.value?.trim()) {
    ElMessage.warning('请输入测试问题')
    return
  }
  testing.value = true
  result.value = null
  try {
    result.value = await testModel(modelId, question.value)
  } catch (e) {
    result.value = { error: e?.response?.data?.message || e?.message || '请求失败' }
  } finally {
    testing.value = false
  }
}

function onBack() {
  router.push({ name: 'model' })
}

onMounted(load)
</script>

<template>
  <div class="page-wrap-narrow">
    <h2 class="page-title">🔌 连接测试</h2>
    <p class="page-subtitle">验证模型 API 是否可用,记录调用耗时</p>

    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span><el-icon><Connection /></el-icon> 连接测试</span>
          <el-button link @click="onBack">返回列表</el-button>
        </div>
      </template>

      <div v-if="model" class="model-info">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="名称">{{ model.name }}</el-descriptions-item>
          <el-descriptions-item label="提供商">
            <el-tag size="small">{{ model.provider }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="模型版本">{{ model.modelVersion }}</el-descriptions-item>
          <el-descriptions-item label="API Key">{{ model.apiKeyMasked }}</el-descriptions-item>
          <el-descriptions-item label="Endpoint" :span="2">{{ model.endpoint || '(未配置)' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <el-form style="margin-top:16px">
        <el-form-item label="测试问题">
          <el-input v-model="question" type="textarea" :rows="3" placeholder="输入测试问题..." />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="testing" @click="onTest">发送测试</el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <div v-if="result" class="result">
        <template v-if="result.error">
          <el-alert :title="'调用失败: ' + result.error" type="error" :closable="false" show-icon />
        </template>
        <template v-else>
          <div class="meta">
            <el-tag type="success" size="small">✅ 调用成功</el-tag>
            <span class="latency">耗时 {{ result.latencyMs }} ms</span>
          </div>
          <h4>响应内容:</h4>
          <pre class="response">{{ result.response }}</pre>
        </template>
      </div>

      <el-alert v-else type="info" :closable="false" show-icon style="margin-top:16px">
        <template #title>说明</template>
        - 当前实现仅支持 OpenAI-compatible chat/completions 协议(M3、OpenAI 等)
        <br />- 其它提供商(智谱/通义等)的真适配在 FR-04 完成
        <br />- Endpoint 为空时会返回 stub 响应(模拟成功)
      </el-alert>
    </el-card>
  </div>
</template>

<style scoped>
.meta { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.latency { color: #999; font-size: 13px; }
.response {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'Cascadia Code', Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
  max-height: 400px;
  overflow-y: auto;
}
</style>
