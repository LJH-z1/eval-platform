<script setup>
/**
 * 对比评测 Arena - LMArena 风格
 * <p>
 * 两种模式:
 * - 单题模式:输入 1 题,随机 2 模型对比,投票
 * - 批量模式:输入 N 题(每行一题),后端并发跑,逐题投票
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listEnabledModels, arenaQuickEval, arenaBatchEval, arenaVote, arenaRanking } from '@/api'

const phase = ref('input')           // input | compare | batch
const mode = ref('single')           // single | batch
const promptText = ref('')
const batchPrompts = ref('')         // 批量模式的多行输入
const left = ref(null)
const right = ref(null)
const pickedSide = ref(null)
const winnerRevealed = ref(false)
const evaluationId = ref(null)
const sending = ref(false)
const ranking = ref([])

const modelList = ref([])

// 批量相关
const batchItems = ref([])           // 后端返回的 N 条
const batchIdx = ref(0)              // 当前题
const batchDone = ref(false)         // 全部投完
const batchLoading = ref(false)      // 批量请求中

onMounted(async () => {
  await loadModels()
  await loadRanking()
})

async function loadModels() {
  try { modelList.value = await listEnabledModels() }
  catch { modelList.value = [] }
}
async function loadRanking() {
  try { ranking.value = await arenaRanking() } catch { ranking.value = [] }
}

function pickTwo() {
  if (modelList.value.length < 2) {
    ElMessage.warning('至少需要 2 个已启用模型')
    return null
  }
  const pool = [...modelList.value]
  const a = pool.splice(Math.floor(Math.random() * pool.length), 1)[0]
  const b = pool[Math.floor(Math.random() * pool.length)]
  return Math.random() < 0.5 ? [a, b] : [b, a]
}

// ============== 单题模式 ==============
async function onSingleSubmit() {
  if (!promptText.value.trim()) { ElMessage.warning('请输入要测试的问题'); return }
  const pair = pickTwo()
  if (!pair) return
  sending.value = true
  phase.value = 'compare'
  winnerRevealed.value = false
  pickedSide.value = null
  left.value = { loading: true }
  right.value = { loading: true }
  try {
    const res = await arenaQuickEval({
      prompt: promptText.value, modelAId: pair[0].id, modelBId: pair[1].id
    })
    evaluationId.value = res.evaluationId
    left.value = {
      loading: false, answerId: res.left.answerId, content: res.left.content,
      latency: res.left.latencyMs, modelId: res.left.modelId, modelName: res.left.modelName,
      modelProvider: res.left.modelProvider, success: res.left.success, errorMessage: res.left.errorMessage
    }
    right.value = {
      loading: false, answerId: res.right.answerId, content: res.right.content,
      latency: res.right.latencyMs, modelId: res.right.modelId, modelName: res.right.modelName,
      modelProvider: res.right.modelProvider, success: res.right.success, errorMessage: res.right.errorMessage
    }
  } catch (e) {
    ElMessage.error('快速评测失败:' + (e?.message || '未知错误'))
    phase.value = 'input'
  } finally {
    sending.value = false
  }
}

async function onSingleVote(side) {
  if (winnerRevealed.value) return
  pickedSide.value = side
  winnerRevealed.value = true
  try {
    await arenaVote({
      evaluationId: evaluationId.value, prompt: promptText.value,
      leftModelId: left.value.modelId, rightModelId: right.value.modelId, winner: side
    })
    ElMessage.success('投票已记录')
    loadRanking()
  } catch (e) {
    ElMessage.error('投票失败:' + (e?.message || '未知错误'))
  }
}

function onSingleNext() {
  promptText.value = ''
  phase.value = 'input'
  left.value = null
  right.value = null
  pickedSide.value = null
  winnerRevealed.value = false
  evaluationId.value = null
}

// ============== 批量模式 ==============
async function onBatchStart() {
  const lines = batchPrompts.value.split('\n').map(s => s.trim()).filter(Boolean)
  if (lines.length === 0) { ElMessage.warning('请至少输入 1 道题(每行一题)'); return }
  if (lines.length > 30) { ElMessage.warning('批量最多 30 题'); return }
  const pair = pickTwo()
  if (!pair) return

  batchLoading.value = true
  batchItems.value = []
  batchIdx.value = 0
  batchDone.value = false
  phase.value = 'batch'
  ElMessage.info(`开始批量评测 ${lines.length} 题 × 2 模型(${pair[0].name} vs ${pair[1].name}),请稍候...`)

  try {
    const start = Date.now()
    const res = await arenaBatchEval({
      prompts: lines, modelAId: pair[0].id, modelBId: pair[1].id
    })
    const ms = Date.now() - start
    // 给每个 item 加 voted 状态
    batchItems.value = res.map((it, i) => ({
      ...it,
      picked: null,
      voted: false,
      modelAName: pair[0].name,
      modelBName: pair[1].name
    }))
    ElMessage.success(`批量完成 ${res.length} 题,耗时 ${(ms/1000).toFixed(1)}s`)
    // 展示第 1 题
    showBatchItem(0)
  } catch (e) {
    ElMessage.error('批量评测失败:' + (e?.message || '未知错误'))
    phase.value = 'input'
  } finally {
    batchLoading.value = false
  }
}

function showBatchItem(idx) {
  if (idx < 0 || idx >= batchItems.value.length) return
  batchIdx.value = idx
  const it = batchItems.value[idx]
  evaluationId.value = it.evaluationId
  promptText.value = it.prompt
  left.value = {
    loading: false, answerId: it.left.answerId, content: it.left.content,
    latency: it.left.latencyMs, modelId: it.left.modelId, modelName: it.left.modelName,
    modelProvider: it.left.modelProvider, success: it.left.success, errorMessage: it.left.errorMessage
  }
  right.value = {
    loading: false, answerId: it.right.answerId, content: it.right.content,
    latency: it.right.latencyMs, modelId: it.right.modelId, modelName: it.right.modelName,
    modelProvider: it.right.modelProvider, success: it.right.success, errorMessage: it.right.errorMessage
  }
  pickedSide.value = it.picked
  winnerRevealed.value = it.voted
}

async function onBatchVote(side) {
  if (winnerRevealed.value) return
  pickedSide.value = side
  winnerRevealed.value = true
  // 记录到 batchItems
  const it = batchItems.value[batchIdx.value]
  it.picked = side
  it.voted = true
  try {
    await arenaVote({
      evaluationId: it.evaluationId, prompt: it.prompt,
      leftModelId: it.left.modelId, rightModelId: it.right.modelId, winner: side
    })
  } catch (e) {
    ElMessage.error('投票失败:' + (e?.message || '未知错误'))
  }
}

function onBatchNext() {
  if (batchIdx.value + 1 >= batchItems.value.length) {
    // 全部投完
    batchDone.value = true
    loadRanking()
    return
  }
  showBatchItem(batchIdx.value + 1)
}

function onBatchSkip() {
  if (batchIdx.value + 1 >= batchItems.value.length) {
    batchDone.value = true
    return
  }
  showBatchItem(batchIdx.value + 1)
}

function onBatchRestart() {
  batchPrompts.value = ''
  batchItems.value = []
  batchIdx.value = 0
  batchDone.value = false
  phase.value = 'input'
}

// ============== 共享 ==============
const currentItem = computed(() => batchItems.value[batchIdx.value])
const batchProgress = computed(() => {
  if (batchItems.value.length === 0) return 0
  return Math.round((batchItems.value.filter(it => it.voted).length / batchItems.value.length) * 100)
})
const batchVoted = computed(() => batchItems.value.filter(it => it.voted).length)
const voteResultText = computed(() => {
  if (!pickedSide.value) return ''
  if (pickedSide.value === 'A')   return '✅ 你选择了 A 更好'
  if (pickedSide.value === 'B')   return '✅ 你选择了 B 更好'
  if (pickedSide.value === 'tie') return '🤝 你认为两个回答一样好'
  if (pickedSide.value === 'bad') return '👎 你认为两个回答都不好'
  return ''
})
</script>

<template>
  <div class="page-wrap-narrow">
    <h2 class="page-title">⚔️ 对比评测 Arena</h2>
    <p class="page-subtitle">
      盲测两个模型的回答,投票选出更好的一个。每次投票更新 Elo 排名。
    </p>

    <!-- 实时排行榜 -->
    <el-card v-if="ranking.length" shadow="never" style="margin-bottom:16px">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
        <h3 style="margin:0;font-size:16px">🏆 实时 Elo 排行榜</h3>
        <el-button text size="small" @click="loadRanking">刷新</el-button>
      </div>
      <el-table :data="ranking.slice(0, 10)" size="small" stripe>
        <el-table-column prop="rank" label="排名" width="60" />
        <el-table-column label="模型" min-width="180">
          <template #default="{ row }">
            <strong>{{ row.modelName }}</strong>
            <el-tag size="small" type="info" style="margin-left:6px">{{ row.provider }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="elo" label="Elo" width="80" sortable />
        <el-table-column label="战绩" width="120">
          <template #default="{ row }">
            <span style="color:#16a34a">{{ row.wins }}W</span> ·
            <span style="color:#64748b">{{ row.ties }}T</span> ·
            <span style="color:#dc2626">{{ row.losses }}L</span>
          </template>
        </el-table-column>
        <el-table-column label="胜率" width="80">
          <template #default="{ row }">
            {{ (row.winRate * 100).toFixed(1) }}%
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 模式切换 -->
    <el-tabs v-model="mode" @tab-change="(v) => { if (v === 'single') phase = 'input' }" style="margin-bottom:16px">
      <el-tab-pane label="单题模式" name="single" />
      <el-tab-pane label="批量模式" name="batch" />
    </el-tabs>

    <!-- 单题输入 -->
    <template v-if="mode === 'single' && phase === 'input'">
      <el-card shadow="never">
        <el-input
          v-model="promptText"
          type="textarea" :rows="6"
          placeholder="输入你的问题..."
          maxlength="4000" show-word-limit
          :disabled="sending"
        />
        <div style="display:flex;justify-content:space-between;align-items:center;margin-top:16px">
          <div style="font-size:12px;color:#94a3b8">已加载 {{ modelList.length }} 个已启用模型</div>
          <el-button type="primary" size="large" :loading="sending" @click="onSingleSubmit" icon="Promotion">
            发送对比
          </el-button>
        </div>
      </el-card>
    </template>

    <!-- 批量输入 -->
    <template v-if="mode === 'batch' && phase === 'input'">
      <el-card shadow="never">
        <div style="margin-bottom:8px;font-size:14px;font-weight:600">📋 批量输入(每行一题,最多 30 题)</div>
        <el-input
          v-model="batchPrompts"
          type="textarea" :rows="10"
          placeholder="用 Python 写 hello world
用 JS 写阶乘函数
解释一下 REST API
..."
          :disabled="batchLoading"
        />
        <div style="display:flex;justify-content:space-between;align-items:center;margin-top:16px">
          <div style="font-size:12px;color:#94a3b8">
            共 {{ batchPrompts.split('\n').filter(s => s.trim()).length }} 题 · 模型随机 2 个 · 后端并发跑
          </div>
          <el-button type="primary" size="large" :loading="batchLoading" @click="onBatchStart" icon="Promotion">
            开始批量对比
          </el-button>
        </div>
      </el-card>
    </template>

    <!-- 单题对比 -->
    <template v-if="phase === 'compare' && left && right">
      <div class="arena-prompt">
        <div style="font-size:12px;color:#94a3b8;margin-bottom:8px">
          📝 {{ mode === 'batch' ? `第 ${batchIdx + 1}/${batchItems.length} 题` : '你的问题' }}
        </div>
        {{ promptText }}
      </div>

      <div class="arena-grid">
        <div
          class="arena-side"
          :class="{
            winner: winnerRevealed && pickedSide === 'A',
            loser:  winnerRevealed && (pickedSide === 'B' || pickedSide === 'tie' || pickedSide === 'bad')
          }"
          @click="!winnerRevealed && (mode === 'single' ? onSingleVote('A') : onBatchVote('A'))"
        >
          <span class="label A">A</span>
          <div v-if="left?.loading" class="loading-area">
            <el-icon class="is-loading" style="font-size:24px"><Loading /></el-icon>
            <span style="margin-left:8px">A 模型正在思考...</span>
          </div>
          <template v-else>
            <div class="model-name">模型 A · 延迟 {{ left.latency }}ms</div>
            <div class="answer">{{ left.content || left.errorMessage || '(无内容)' }}</div>
          </template>
        </div>

        <div
          class="arena-side"
          :class="{
            winner: winnerRevealed && pickedSide === 'B',
            loser:  winnerRevealed && (pickedSide === 'A' || pickedSide === 'tie' || pickedSide === 'bad')
          }"
          @click="!winnerRevealed && (mode === 'single' ? onSingleVote('B') : onBatchVote('B'))"
        >
          <span class="label B">B</span>
          <div v-if="right?.loading" class="loading-area">
            <el-icon class="is-loading" style="font-size:24px"><Loading /></el-icon>
            <span style="margin-left:8px">B 模型正在思考...</span>
          </div>
          <template v-else>
            <div class="model-name">模型 B · 延迟 {{ right.latency }}ms</div>
            <div class="answer">{{ right.content || right.errorMessage || '(无内容)' }}</div>
          </template>
        </div>
      </div>

      <div v-if="!winnerRevealed" class="arena-actions">
        <el-button size="large" :disabled="sending" @click="mode === 'single' ? onSingleVote('A') : onBatchVote('A')">A 更好 👈</el-button>
        <el-button size="large" :disabled="sending" @click="mode === 'single' ? onSingleVote('tie') : onBatchVote('tie')">🤝 平局</el-button>
        <el-button size="large" :disabled="sending" @click="mode === 'single' ? onSingleVote('B') : onBatchVote('B')">👉 B 更好</el-button>
        <el-button size="large" type="danger" plain :disabled="sending" @click="mode === 'single' ? onSingleVote('bad') : onBatchVote('bad')">👎 都不好</el-button>
        <el-button v-if="mode === 'batch'" size="large" plain @click="onBatchSkip">⏭ 跳过</el-button>
      </div>

      <div v-else class="vote-result">
        <el-card shadow="never">
          <div style="font-size:18px;font-weight:700;margin-bottom:12px">{{ voteResultText }}</div>
          <el-divider style="margin:12px 0" />
          <div class="reveal">
            <div>
              <span class="reveal-label">A 是:</span>
              <strong>{{ left.modelName }}</strong>
              <el-tag size="small" style="margin-left:8px">{{ left.modelProvider }}</el-tag>
            </div>
            <div>
              <span class="reveal-label">B 是:</span>
              <strong>{{ right.modelName }}</strong>
              <el-tag size="small" style="margin-left:8px">{{ right.modelProvider }}</el-tag>
            </div>
          </div>
          <div style="text-align:center;margin-top:16px">
            <el-button v-if="mode === 'single'" type="primary" size="large" @click="onSingleNext">下一题 →</el-button>
            <el-button v-else type="primary" size="large" @click="onBatchNext">
              {{ batchIdx + 1 >= batchItems.length ? '查看汇总 →' : `下一题 (${batchIdx + 2}/${batchItems.length}) →` }}
            </el-button>
          </div>
        </el-card>
      </div>
    </template>

    <!-- 批量全部完成 -->
    <template v-if="phase === 'batch' && batchDone">
      <el-card shadow="never">
        <h3 style="margin-top:0">🎉 批量完成!共 {{ batchItems.length }} 题</h3>
        <el-progress :percentage="batchProgress" :status="batchProgress === 100 ? 'success' : 'warning'" />
        <div style="margin-top:8px;font-size:14px">
          已投 {{ batchVoted }} 题 · 跳过 {{ batchItems.length - batchVoted }} 题
        </div>
        <el-table :data="batchItems" size="small" stripe style="margin-top:16px">
          <el-table-column label="#" type="index" width="50" />
          <el-table-column label="题目" min-width="300">
            <template #default="{ row }">
              {{ row.prompt.length > 50 ? row.prompt.slice(0, 50) + '...' : row.prompt }}
            </template>
          </el-table-column>
          <el-table-column label="A 状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.left.success" type="success" size="small">OK</el-tag>
              <el-tag v-else type="danger" size="small">失败</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="B 状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.right.success" type="success" size="small">OK</el-tag>
              <el-tag v-else type="danger" size="small">失败</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="你的投票" width="120">
            <template #default="{ row }">
              <el-tag v-if="!row.voted" type="info" size="small">跳过</el-tag>
              <el-tag v-else-if="row.picked === 'A'" type="success" size="small">A 更好</el-tag>
              <el-tag v-else-if="row.picked === 'B'" type="primary" size="small">B 更好</el-tag>
              <el-tag v-else-if="row.picked === 'tie'" type="warning" size="small">平局</el-tag>
              <el-tag v-else-if="row.picked === 'bad'" type="danger" size="small">都差</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="揭晓" min-width="200">
            <template #default="{ row }">
              <span style="font-size:12px">A: {{ row.left.modelName }} ({{ row.left.modelProvider }})</span><br/>
              <span style="font-size:12px">B: {{ row.right.modelName }} ({{ row.right.modelProvider }})</span>
            </template>
          </el-table-column>
        </el-table>
        <div style="text-align:center;margin-top:16px">
          <el-button type="primary" size="large" @click="onBatchRestart">再来一批</el-button>
          <el-button size="large" @click="mode = 'single'; phase = 'input'">切到单题</el-button>
        </div>
      </el-card>
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
