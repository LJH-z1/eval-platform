<script setup>
/**
 * 对比评测 (Arena) - LMArena 风格核心页
 * <p>
 * 类似 lmarena.ai 投票页:
 * - 用户输入 prompt
 * - 系统盲测两个模型回答(隐藏名字,投票后揭晓)
 * - 投票 A / B / Tie / Both bad
 * <p>
 * 数据流:
 * - 加载已启用模型:listEnabledModels()
 * - 单次提问:直接通过 /api/evaluations/{id}/run 触发真评测(简化为单问题即时)
 * - 当前实现:仍用 mock + 真模型列表(真 API 等异步评测后页面会接)
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listEnabledModels, runEvaluation, getEvaluation } from '@/api'

const phase = ref('input')
const promptText = ref('')
const left = ref(null)
const right = ref(null)
const pickedSide = ref(null)
const winnerRevealed = ref(false)

const modelList = ref([])
const realApiAvailable = ref(false)

onMounted(async () => {
  try {
    modelList.value = await listEnabledModels()
    realApiAvailable.value = modelList.value.length >= 2
  } catch (_) {
    // 后端不可达 — 用 mock
    modelList.value = mockModels
    realApiAvailable.value = false
  }
})

// ---- Mock 模型列表(后端未接时 fallback) ----
const mockModels = [
  { id: 1, name: 'M3-Plus',      provider: 'M3' },
  { id: 2, name: 'gpt-4o-mini',  provider: 'OPENAI' },
  { id: 3, name: 'glm-4-plus',   provider: 'ZHIPU' },
  { id: 4, name: 'qwen-max',     provider: 'QWEN' }
]

// ---- Mock 回答模板(让演示有真实感,无后端时用) ----
const mockAnswers = [
  (q) => `关于"${q}",这是一个非常好的问题。\n\n首先,从定义上说,${q.slice(0, 20)}... 涉及多个核心要素。我会从以下几个角度来分析:\n\n1. 理论层面:基础概念与原理\n2. 实践层面:具体应用场景\n3. 发展趋势:未来可能演化方向\n\n综合来看,这个问题没有一个简单的是非答案,而是需要根据具体场景灵活处理。`,
  (q) => `让我来回答你的问题:"${q}"\n\n这是一个经典的开放性问题。我认为关键点在于:\n\n• 核心要素 A — 是问题的关键所在\n• 核心要素 B — 容易被忽视但很重要\n• 注意事项 — 实际应用时需要关注\n\n建议你可以进一步明确具体场景,这样我能给出更精准的回答。`
]

function pickTwo() {
  if (modelList.value.length < 2) {
    ElMessage.warning('至少需要 2 个已启用模型才能对比')
    return null
  }
  const pool = [...modelList.value]
  const a = pool.splice(Math.floor(Math.random() * pool.length), 1)[0]
  const b = pool[Math.floor(Math.random() * pool.length)]
  if (Math.random() < 0.5) return [a, b]
  return [b, a]
}

async function callMock(model, prompt, idx) {
  await new Promise(r => setTimeout(r, 600 + Math.random() * 900))
  return {
    model,
    content: mockAnswers[idx % mockAnswers.length](prompt),
    latency: 600 + Math.floor(Math.random() * 900)
  }
}

async function onSubmit() {
  if (!promptText.value.trim()) {
    ElMessage.warning('请输入要测试的问题')
    return
  }
  const pair = pickTwo()
  if (!pair) return
  phase.value = 'compare'
  winnerRevealed.value = false
  pickedSide.value = null
  left.value = { loading: true }
  right.value = { loading: true }

  if (realApiAvailable.value) {
    // 真 API 模式:创建 1 题 2 模型的评测,异步等待
    await runViaApi(pair)
  } else {
    // Mock 模式
    const [l, r] = await Promise.all([
      callMock(pair[0], promptText.value, 0),
      callMock(pair[1], promptText.value, 1)
    ])
    left.value = l
    right.value = r
  }
}

/**
 * 真实 API 路径:
 * 1) 创建 evaluation(name="Arena-YYYYMMDD-HHmmss", 2 models, 1 question)
 * 2) 创建一个临时 question(content=prompt),questionId 入参
 * 3) 启动 run,轮询直到完成
 * 4) 拉 answer 列表填充 left/right
 *
 * 简化:用 mock 替代真 API 调用(真 API 需要 question 入库),但用真模型列表
 */
async function runViaApi(pair) {
  // 简化版:用 mock + 真模型名展示
  await new Promise(r => setTimeout(r, 1200))
  const [l, r] = await Promise.all([
    callMock(pair[0], promptText.value, 0),
    callMock(pair[1], promptText.value, 1)
  ])
  left.value = l
  right.value = r
  ElMessage.info('当前为前端 mock 演示,真 API 调用需在"评测任务"页创建正式评测')
}

function onVote(side) {
  pickedSide.value = side
  winnerRevealed.value = true
  console.log('[vote]', side, {
    prompt: promptText.value,
    A: left.value.model.name,
    B: right.value.model.name,
    picked: side
  })
}

function onNext() {
  promptText.value = ''
  phase.value = 'input'
  left.value = null
  right.value = null
  pickedSide.value = null
  winnerRevealed.value = false
}

const voteResultText = computed(() => {
  if (!pickedSide.value) return ''
  if (pickedSide.value === 'A')  return `✅ 你选择了 A 更好`
  if (pickedSide.value === 'B')  return `✅ 你选择了 B 更好`
  if (pickedSide.value === 'tie')return `🤝 你认为两个回答一样好`
  if (pickedSide.value === 'bad')return `👎 你认为两个回答都不好`
  return ''
})
</script>

<template>
  <div class="page-wrap-narrow">
    <h2 class="page-title">⚔️ 对比评测 Arena</h2>
    <p class="page-subtitle">
      输入问题,系统盲测两个模型的回答(隐藏名字),投票选出更好的一个。
    </p>

    <!-- 输入阶段 -->
    <template v-if="phase === 'input'">
      <el-card shadow="never">
        <el-input
          v-model="promptText"
          type="textarea"
          :rows="6"
          placeholder="输入你的问题...&#10;例如:请用 Python 写一个快速排序算法,要求带详细注释。&#10;或者:解释一下 Transformer 的注意力机制。"
          maxlength="4000"
          show-word-limit
        />
        <div style="display:flex;justify-content:space-between;align-items:center;margin-top:16px">
          <div style="font-size:12px;color:#94a3b8">
            提示词长度建议 50-500 字 · 可输入代码、数学公式、自然语言
          </div>
          <el-button type="primary" size="large" @click="onSubmit" :icon="'Promotion'">
            发送对比
          </el-button>
        </div>
      </el-card>

      <el-alert type="info" :closable="false" show-icon style="margin-top:16px">
        <template #title>已加载 {{ modelList.length }} 个模型可参与对比</template>
        <span v-if="realApiAvailable">✓ 已连后端,真模型列表生效</span>
        <span v-else>⚠️ 后端未连接,使用内置 mock 模型演示</span>
        <br/>
        Arena 当前为前端 mock 演示;真 API 评测请到「评测任务 → 新建评测」页操作,会异步跑完写库。
      </el-alert>
    </template>

    <!-- 对比阶段 -->
    <template v-if="phase === 'compare'">
      <div class="arena-prompt">
        <div style="font-size:12px;color:#94a3b8;margin-bottom:8px">📝 你的问题</div>
        {{ promptText }}
      </div>

      <div class="arena-grid">
        <div
          class="arena-side"
          :class="{
            winner: winnerRevealed && pickedSide === 'A',
            loser:  winnerRevealed && (pickedSide === 'B' || pickedSide === 'tie' || pickedSide === 'bad')
          }"
          @click="!winnerRevealed && onVote('A')"
        >
          <span class="label A">A</span>
          <div v-if="left?.loading" class="loading-area">
            <el-icon class="is-loading" style="font-size:24px"><Loading /></el-icon>
            <span style="margin-left:8px">A 模型正在思考...</span>
          </div>
          <template v-else>
            <div class="model-name">模型 A · 延迟 {{ left.latency }}ms</div>
            <div class="answer">{{ left.content }}</div>
          </template>
        </div>

        <div
          class="arena-side"
          :class="{
            winner: winnerRevealed && pickedSide === 'B',
            loser:  winnerRevealed && (pickedSide === 'A' || pickedSide === 'tie' || pickedSide === 'bad')
          }"
          @click="!winnerRevealed && onVote('B')"
        >
          <span class="label B">B</span>
          <div v-if="right?.loading" class="loading-area">
            <el-icon class="is-loading" style="font-size:24px"><Loading /></el-icon>
            <span style="margin-left:8px">B 模型正在思考...</span>
          </div>
          <template v-else>
            <div class="model-name">模型 B · 延迟 {{ right.latency }}ms</div>
            <div class="answer">{{ right.content }}</div>
          </template>
        </div>
      </div>

      <div v-if="!winnerRevealed" class="arena-actions">
        <el-button size="large" @click="onVote('A')">A 更好 👈</el-button>
        <el-button size="large" @click="onVote('tie')">🤝 平局</el-button>
        <el-button size="large" @click="onVote('B')">👉 B 更好</el-button>
        <el-button size="large" type="danger" plain @click="onVote('bad')">👎 都不好</el-button>
      </div>

      <div v-else class="vote-result">
        <el-card shadow="never">
          <div style="font-size:18px;font-weight:700;margin-bottom:12px">{{ voteResultText }}</div>
          <el-divider style="margin:12px 0" />
          <div class="reveal">
            <div>
              <span class="reveal-label">A 是:</span>
              <strong>{{ left.model.name }}</strong>
              <el-tag size="small" style="margin-left:8px">{{ left.model.provider }}</el-tag>
            </div>
            <div>
              <span class="reveal-label">B 是:</span>
              <strong>{{ right.model.name }}</strong>
              <el-tag size="small" style="margin-left:8px">{{ right.model.provider }}</el-tag>
            </div>
          </div>
          <div style="text-align:center;margin-top:16px">
            <el-button type="primary" size="large" @click="onNext">下一题 →</el-button>
          </div>
        </el-card>
      </div>
    </template>
  </div>
</template>

<style scoped>
.loading-area {
  display: flex;
  align-items: center;
  color: #94a3b8;
  font-size: 14px;
  padding: 40px 0;
  justify-content: center;
}
.reveal {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  font-size: 14px;
}
.reveal-label { color: #94a3b8; margin-right: 8px; }
.vote-result { margin-top: 8px; }
</style>
