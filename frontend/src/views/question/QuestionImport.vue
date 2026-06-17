<script setup>
/**
 * 问题管理 — 批量导入 CSV
 * <p>
 * 路由:/question/import
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { importQuestions } from '@/api'

const router = useRouter()
const file = ref(null)
const uploading = ref(false)
const result = ref(null)

const sampleCsv = `content,category,difficulty,type,expected_answer
什么是光合作用?,科学,中等,事实,植物利用光将二氧化碳和水转化为葡萄糖
写一个 Python 斐波那契函数,编程,简单,代码,def fib(n): ...
请解释相对论的核心思想,科学,困难,推理,
写一首关于秋天的诗,语文,中等,创作,`

function onFileChange(uploadFile) {
  file.value = uploadFile.raw
}

function onRemove() {
  file.value = null
  result.value = null
}

async function onSubmit() {
  if (!file.value) {
    ElMessage.warning('请先选择 CSV 文件')
    return
  }
  uploading.value = true
  try {
    const data = await importQuestions(file.value)
    result.value = data
    if (data.failed === 0) {
      ElMessage.success(`成功导入 ${data.success} 题`)
    } else {
      ElMessage.warning(`导入完成:成功 ${data.success},失败 ${data.failed}`)
    }
  } finally {
    uploading.value = false
  }
}

function onBack() {
  router.push({ name: 'question' })
}
</script>

<template>
  <div>
    <el-card shadow="never" header="批量导入 CSV">
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        <p><strong>CSV 格式</strong>(每行 1 题,字段顺序:content, category, difficulty, type, expected_answer)</p>
        <p><strong>限制</strong>:单次最多 200 题,单题 content ≤ 4000 字,第一行是表头(可省略)</p>
      </el-alert>

      <el-form label-width="100px" style="max-width: 700px">
        <el-form-item label="选择文件">
          <el-upload
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            accept=".csv,.txt"
            :on-change="onFileChange"
            :on-remove="onRemove"
          >
            <el-button type="primary">选择 CSV 文件</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="success" :loading="uploading" @click="onSubmit">开始导入</el-button>
          <el-button @click="onBack">返回列表</el-button>
        </el-form-item>
      </el-form>

      <el-divider content-position="left">CSV 示例</el-divider>
    </el-card>

    <el-card v-if="result" shadow="never" style="margin-top: 16px" header="导入结果">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-statistic title="成功" :value="result.success" :value-style="{ color: '#67c23a' }" />
        </el-col>
        <el-col :span="8">
          <el-statistic title="失败" :value="result.failed" :value-style="{ color: '#f56c6c' }" />
        </el-col>
        <el-col :span="8">
          <el-statistic title="合计" :value="result.success + result.failed" />
        </el-col>
      </el-row>
      <el-divider v-if="result.errorMessages && result.errorMessages.length > 0" content-position="left">错误明细</el-divider>
      <el-table v-if="result.errorMessages && result.errorMessages.length > 0"
                :data="result.errorMessages" border max-height="300" empty-text="无错误">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column label="错误信息" prop="*" />
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <pre style="background: #f5f7fa; padding: 16px; border-radius: 4px;
                  white-space: pre-wrap; font-family: 'Cascadia Code', monospace;
                  font-size: 12px; line-height: 1.6">{{ sampleCsv }}</pre>
    </el-card>
  </div>
</template>
