<script setup>
/**
 * 首页 Dashboard - LMArena 风格
 * <p>
 * 角色适配:
 * - VISITOR 看到「访客模式」:只看 Arena / 报告导出 / 总览
 * - 其他角色看到全功能 + 可用模块卡片
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { pageQuestions, pageModels, arenaRanking, getDashboardStats } from '@/api'

const router = useRouter()
const auth = useAuthStore()
const stats = ref({ questions: 0, models: 0, evaluations: 0, scores: 0 })
const leaderboard = ref([])

const ROLE_RANK = { VISITOR: 1, SCORER: 2, ORGANIZER: 3, ADMIN: 4 }
const isVisitor = computed(() => auth.role === 'VISITOR')

onMounted(async () => {
  try {
    const s = await getDashboardStats()
    stats.value.questions = s.questions || 0
    stats.value.models = s.models || 0
    stats.value.evaluations = s.evaluations || 0
    stats.value.scores = s.scores || 0
  } catch (_) {
    try {
      const q = await pageQuestions({ pageNum: 1, pageSize: 1 })
      stats.value.questions = q.total || 0
    } catch (_) {}
    try {
      const m = await pageModels({ pageNum: 1, pageSize: 1 })
      stats.value.models = m.total || 0
    } catch (_) {}
  }
  try { leaderboard.value = await arenaRanking() } catch { leaderboard.value = [] }
})

// 全部模块定义(每个有 minRole 最低门槛)
const allModules = [
  { icon: '📝', title: '问题管理',     desc: '管理评测题目,支持单题录入、批量导入 CSV',     path: 'question',    minRole: 'SCORER' },
  { icon: '🤖', title: '模型配置',     desc: '添加和管理多提供商模型,M3/OpenAI/智谱 等',     path: 'model',       minRole: 'ORGANIZER' },
  { icon: '⚔️', title: '对比评测',     desc: '盲测投票式对比两个模型的回答,得出 Elo 排名', path: 'arena',       minRole: 'VISITOR' },
  { icon: '🚀', title: '评测任务',     desc: '创建评测批次,选问题 × 选模型,异步执行',       path: 'evaluation',  minRole: 'ORGANIZER' },
  { icon: '⭐', title: '多维评分',     desc: '准确性 / 流畅性 / 创造性 / 安全性 4 维评分',   path: 'score',       minRole: 'SCORER' },
  { icon: '📊', title: '一致性分析',   desc: 'Fleiss Kappa 评分员一致性 + 争议样本识别',    path: 'stats',       minRole: 'SCORER' },
  { icon: '💰', title: '成本统计',     desc: '按模型 / 时间 / 评测 维度聚合调用成本',       path: 'billing',     minRole: 'ORGANIZER' },
  { icon: '📥', title: '报告导出',     desc: '评测结果导出 Excel / PDF 报告',                path: 'export',      minRole: 'VISITOR' },
  { icon: '👥', title: '用户管理',     desc: '管理员可创建用户、分配角色、禁用账号',         path: 'users',       minRole: 'ADMIN' }
]

// 根据当前 role 过滤模块
const modules = computed(() => {
  const myRank = ROLE_RANK[auth.role] || 0
  return allModules.filter(m => myRank >= (ROLE_RANK[m.minRole] || 0))
})

function go(path) { router.push({ name: path }) }
</script>

<template>
  <div>
    <!-- Hero:访客模式 — 简化版,只展示可看的内容 -->
    <section v-if="isVisitor" class="hero visitor-hero">
      <h1>👋 欢迎,{{ auth.user?.username }}!</h1>
      <p>
        你是「访客」角色,可以浏览公开的模型对比排行榜和评测报告,
        但不能创建/修改任何内容。如需参与投票或评分,
        请联系管理员升级为「评分员」账号。
      </p>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="go('arena')">
          🏆 查看模型排行榜
        </el-button>
        <el-button size="large" @click="go('export')">
          📥 浏览公开报告
        </el-button>
      </div>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="max-width:600px;margin:24px auto 0"
      >
        <template #title>访客权限说明</template>
        可访问:首页 · 对比评测(只读) · 报告导出(只读) · 个人中心<br/>
        不可访问:问题管理 · 模型配置 · 评测任务 · 多维评分 · 一致性分析 · 成本统计 · 用户管理
      </el-alert>
    </section>

    <!-- Hero:正常模式 -->
    <section v-else class="hero">
      <h1>EvalArena · 多模型对比评测平台</h1>
      <p>
        通过真实用户的盲测投票,权威评估 GPT-4、Claude、Gemini、MiniMax 等
        顶尖 AI 模型的表现,让数据告诉你哪个模型更适合你的任务。
      </p>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="go('arena')">
          ⚔️ 开始盲测对比
        </el-button>
        <el-button size="large" @click="go('question')">
          📝 浏览问题库
        </el-button>
      </div>
      <div class="hero-stats">
        <div class="hero-stat">
          <div class="num">{{ stats.questions }}</div>
          <div class="label">题库题目</div>
        </div>
        <div class="hero-stat">
          <div class="num">{{ stats.models }}</div>
          <div class="label">已配置模型</div>
        </div>
        <div class="hero-stat">
          <div class="num">{{ stats.evaluations }}</div>
          <div class="label">评测任务</div>
        </div>
        <div class="hero-stat">
          <div class="num">258</div>
          <div class="label">评测模型(全球)</div>
        </div>
      </div>
    </section>

    <!-- 功能模块(只显示当前角色可访问的) -->
    <section class="page-wrap">
      <h2 class="page-title">🚀 功能模块</h2>
      <p class="page-subtitle">
        <template v-if="isVisitor">访客可访问 {{ modules.length }} 个模块</template>
        <template v-else>当前角色「{{ auth.role }}」可访问 {{ modules.length }} 个模块</template>
      </p>
      <div class="feature-grid">
        <div
          v-for="m in modules"
          :key="m.title"
          class="feature-card"
          @click="go(m.path)"
        >
          <div class="icon">{{ m.icon }}</div>
          <div class="title">
            {{ m.title }}
            <span class="badge" :class="m.status">{{
              m.status === 'done' ? '已实现' :
              m.status === 'new'  ? 'NEW'     :
              m.status === 'todo' ? '待开发'  : m.status
            }}</span>
            <span v-if="m.adminOnly" class="badge todo">管理员</span>
          </div>
          <div class="desc">{{ m.desc }}</div>
          <div class="meta">
            <span>进入 →</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 排行榜(参考 LMArena Text Arena) -->
    <section class="page-wrap">
      <div class="lb-section">
        <h2 class="lb-title">📝 Text Arena 排行榜</h2>
        <div class="lb-underline"></div>
        <div class="lb-meta">
          最后更新: 2025年11月19日 | 总投票数: 4,278,480 | 参与模型: 258
        </div>

        <div class="lb-card">
          <table class="lb-table">
            <thead>
              <tr>
                <th style="width:80px">排名</th>
                <th style="text-align:left">模型名称</th>
                <th style="width:110px">Elo</th>
                <th style="width:120px">胜率</th>
                <th style="width:160px">战绩 (W·T·L)</th>
                <th style="width:120px">对局数</th>
                <th style="width:140px">状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!leaderboard.length">
                <td colspan="7" style="text-align:center;color:#94a3b8;padding:32px">
                  暂无投票数据 — 前往「对比评测」开始盲评
                </td>
              </tr>
              <tr v-for="r in leaderboard" :key="r.modelId" :class="`lb-row rank-${r.rank}`">
                <td>
                  <span class="lb-medal">
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
                <td><span class="lb-score">{{ r.elo }}</span></td>
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
    </section>

    <!-- 关于 -->
    <section class="page-wrap">
      <h2 class="page-title">🎯 关于 EvalArena</h2>
      <p class="page-subtitle">通过盲测对比机制,让真实用户投票决定模型排名</p>

      <div class="feature-grid">
        <div class="feature-card">
          <div class="icon">⚖️</div>
          <div class="title">公正透明</div>
          <div class="desc">盲测机制,投票时隐藏模型身份,消除品牌偏见</div>
        </div>
        <div class="feature-card">
          <div class="icon">📊</div>
          <div class="title">Elo 评分</div>
          <div class="desc">采用国际象棋级别的 Elo 算法,每次对比动态调整排名</div>
        </div>
        <div class="feature-card">
          <div class="icon">🔬</div>
          <div class="title">多维度评测</div>
          <div class="desc">文本 / 代码 / 视觉 / 多模态 / 评分一致性 全方位评估</div>
        </div>
        <div class="feature-card">
          <div class="icon">🔒</div>
          <div class="title">API Key 安全</div>
          <div class="desc">所有凭据 AES-256 加密存储,列表仅显示掩码</div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* 局部微调(主样式在 global.css) */
.hero { margin: -32px -24px 0; padding: 80px 24px 56px; }

/* 排行榜(LMArena 风格) */
.lb-section {
  margin: 48px 0;
  text-align: center;
}
.lb-title {
  font-size: 32px;
  font-weight: 800;
  margin: 0 0 12px;
  letter-spacing: -0.02em;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.lb-underline {
  width: 80px;
  height: 4px;
  margin: 0 auto 20px;
  border-radius: 2px;
  background: linear-gradient(90deg, #6366f1 0%, #8b5cf6 100%);
}
.lb-meta {
  text-align: center;
  color: var(--text-soft);
  font-size: 13px;
  margin-bottom: 28px;
}

.lb-card {
  background: var(--bg);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow);
  border: 1px solid var(--border);
}

.lb-table {
  width: 100%;
  border-collapse: collapse;
}
.lb-table th {
  padding: 18px 16px;
  background: linear-gradient(90deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  text-align: center;
  letter-spacing: 0.02em;
}
.lb-table th:first-child { border-top-left-radius: var(--radius-lg); }
.lb-table th:last-child  { border-top-right-radius: var(--radius-lg); }

.lb-table td {
  padding: 16px;
  border-bottom: 1px solid var(--border);
  text-align: center;
  font-size: 14px;
  color: var(--text);
  transition: background .15s;
}
.lb-table tbody tr:last-child td { border-bottom: none; }
.lb-table tbody tr:hover td { filter: brightness(0.97); }

.lb-medal {
  font-size: 22px;
  display: inline-block;
}
.lb-score {
  font-weight: 700;
  color: #4f46e5;
  font-variant-numeric: tabular-nums;
  font-size: 15px;
}

/* 按排名分色 — 1=金,2=银,3=铜,4+=白 */
.lb-row.rank-1 td { background: #fefce8; }     /* 金:淡黄 */
.lb-row.rank-2 td { background: #f1f5f9; }     /* 银:浅灰 */
.lb-row.rank-3 td { background: #ffedd5; }     /* 铜:淡橙 */
.lb-row.rank-4 td,
.lb-row.rank-5 td,
.lb-row.rank-6 td,
.lb-row.rank-7 td,
.lb-row.rank-8 td,
.lb-row.rank-9 td,
.lb-row.rank-10 td { background: #ffffff; }

/* 行首单元格左侧色条 */
.lb-row.rank-1 td:first-child { border-left: 4px solid #f59e0b; }
.lb-row.rank-2 td:first-child { border-left: 4px solid #94a3b8; }
.lb-row.rank-3 td:first-child { border-left: 4px solid #b45309; }
.lb-row.rank-4 td:first-child,
.lb-row.rank-5 td:first-child,
.lb-row.rank-6 td:first-child,
.lb-row.rank-7 td:first-child,
.lb-row.rank-8 td:first-child,
.lb-row.rank-9 td:first-child,
.lb-row.rank-10 td:first-child { border-left: 4px solid transparent; }
</style>
