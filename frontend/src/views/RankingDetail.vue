<script setup>
/**
 * 排行榜详情页 - LMArena 风格
 * <p>
 * 路由: /ranking/:category
 * category = all / code / writing / math / vision / general
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { arenaRanking } from '@/api'

const route = useRoute()
const router = useRouter()

const category = computed(() => route.params.category || 'all')
const loading = ref(false)
const rankings = ref([])

const categoryInfo = computed(() => {
  const map = {
    all:      { name: '🌐 综合 Arena',  desc: '全能力维度的综合表现' },
    code:     { name: '💻 Code Arena',  desc: '代码生成 / 理解 / 调试能力' },
    writing:  { name: '✍️ Writing Arena', desc: '中文 / 英文 / 文案 / 故事能力' },
    math:     { name: '🔢 Math Arena',  desc: '数学推理 / 逻辑证明能力' },
    vision:   { name: '👁️ Vision Arena', desc: '图像理解 / 视觉问答能力' },
    general:  { name: '🌐 General Arena', desc: '通用对话 / 百科问答' }
  }
  return map[category.value] || { name: category.value, desc: '' }
})

async function load() {
  loading.value = true
  try {
    const cat = category.value === 'all' ? undefined : category.value
    const r = await arenaRanking(cat)
    rankings.value = r || []
  } catch (e) {
    rankings.value = []
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push({ name: 'dashboard' })
}

watch(category, load)
onMounted(load)
</script>

<template>
  <div class="page-wrap">
    <el-button @click="goBack" :icon="'ArrowLeft'" style="margin-bottom:16px">
      ← 返回总览
    </el-button>

    <div class="rd-section">
      <h1 class="rd-title">{{ categoryInfo.name }} 排行榜</h1>
      <div class="rd-underline"></div>
      <p class="rd-meta">{{ categoryInfo.desc }} · 基于 Arena 盲评投票 · Elo 算法 · K=32</p>

      <div class="rd-card" v-loading="loading">
        <table class="rd-table">
          <thead>
            <tr>
              <th style="width:80px">排名</th>
              <th style="text-align:left">模型名称</th>
              <th style="width:110px">Elo</th>
              <th style="width:120px">胜率</th>
              <th style="width:180px">战绩 (W·T·L)</th>
              <th style="width:120px">对局数</th>
              <th style="width:140px">状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!rankings.length">
              <td colspan="7" style="text-align:center;color:#94a3b8;padding:48px">
                <div style="font-size:40px;margin-bottom:8px">📭</div>
                <div style="font-size:15px">该能力维度暂无投票数据</div>
                <div style="font-size:12px;margin-top:8px">
                  前往「<a href="javascript:void(0)" @click="router.push('arena')" style="color:#4f46e5">对比评测</a>」,
                  跑题时选择此能力,投几票就能看到排名
                </div>
              </td>
            </tr>
            <tr v-for="r in rankings" :key="r.modelId" :class="`rd-row rank-${r.rank}`">
              <td>
                <span class="rd-medal">
                  <template v-if="r.rank === 1">🥇</template>
                  <template v-else-if="r.rank === 2">🥈</template>
                  <template v-else-if="r.rank === 3">🥉</template>
                  <template v-else>{{ r.rank }}</template>
                </span>
              </td>
              <td style="text-align:left">
                <strong>{{ r.modelName }}</strong>
                <el-tag size="small" style="margin-left:6px">{{ r.provider }}</el-tag>
              </td>
              <td><span class="rd-score">{{ r.elo }}</span></td>
              <td>{{ ((r.winRate||0) * 100).toFixed(1) }}%</td>
              <td>{{ (r.wins||0) }}W · {{ (r.ties||0) }}T · {{ (r.losses||0) }}L</td>
              <td>{{ (r.games||0) }} 局</td>
              <td>
                <el-tag size="small" type="success" effect="light" round>活跃</el-tag>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.rd-section { margin: 24px 0; text-align: center; }
.rd-title {
  font-size: 36px;
  font-weight: 800;
  margin: 0 0 12px;
  letter-spacing: -0.02em;
}
.rd-underline {
  width: 80px;
  height: 4px;
  margin: 0 auto 20px;
  border-radius: 2px;
  background: linear-gradient(90deg, #6366f1 0%, #8b5cf6 100%);
}
.rd-meta {
  text-align: center;
  color: var(--text-soft);
  font-size: 14px;
  margin: 0 0 32px;
}

.rd-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0,0,0,.06);
  border: 1px solid var(--border);
}
.rd-table {
  width: 100%;
  border-collapse: collapse;
}
.rd-table th {
  padding: 20px 16px;
  background: linear-gradient(90deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  text-align: center;
}
.rd-table td {
  padding: 18px 16px;
  border-bottom: 1px solid var(--border);
  text-align: center;
  font-size: 14px;
  color: var(--text);
  transition: background .15s;
}
.rd-table tbody tr:last-child td { border-bottom: none; }
.rd-table tbody tr:hover td { filter: brightness(0.97); }

.rd-medal { font-size: 24px; display: inline-block; }
.rd-score {
  font-weight: 700;
  color: #4f46e5;
  font-variant-numeric: tabular-nums;
  font-size: 16px;
}

.rd-row.rank-1 td { background: #fefce8; }
.rd-row.rank-2 td { background: #f1f5f9; }
.rd-row.rank-3 td { background: #ffedd5; }
.rd-row.rank-1 td:first-child { border-left: 4px solid #f59e0b; }
.rd-row.rank-2 td:first-child { border-left: 4px solid #94a3b8; }
.rd-row.rank-3 td:first-child { border-left: 4px solid #b45309; }
.rd-row.rank-4 td:first-child,
.rd-row.rank-5 td:first-child,
.rd-row.rank-6 td:first-child,
.rd-row.rank-7 td:first-child,
.rd-row.rank-8 td:first-child,
.rd-row.rank-9 td:first-child,
.rd-row.rank-10 td:first-child { border-left: 4px solid transparent; }
</style>
