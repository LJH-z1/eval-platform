<script setup>
/**
 * 首页 Dashboard - LMArena 风格
 * <p>
 * 角色适配:
 * - VISITOR 看到「访客模式」:只看 Arena / 报告导出 / 总览
 * - 其他角色看到全功能 + 可用模块卡片
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { pageQuestions, pageModels, arenaRanking, getDashboardStats } from '@/api'

const router = useRouter()
const auth = useAuthStore()
const stats = ref({ questions: 0, models: 0, evaluations: 0, scores: 0 })

// 能力分类(总览卡片用)— 3x3 九宫格,9 个能力,全部已上线
const categoryDefs = [
  { key: '',           label: '🌐 综合 Arena',         shortName: '综合',       icon: '🌐', status: 'live' },
  { key: 'code',       label: '💻 Code Arena',         shortName: '代码',       icon: '💻', status: 'live' },
  { key: 'writing',    label: '✍️ Writing Arena',      shortName: '写作',       icon: '✍️', status: 'live' },
  { key: 'math',       label: '🔢 Math Arena',         shortName: '数学',       icon: '🔢', status: 'live' },
  { key: 'vision',     label: '👁️ Vision Arena',       shortName: '视觉',       icon: '👁️', status: 'live' },
  { key: 'webdev',     label: '🌐 WebDev Arena',       shortName: 'Web 开发',   icon: '🌐', status: 'live' },
  { key: 'image',      label: '🎨 Text-to-Image Arena', shortName: '文生图',     icon: '🎨', status: 'live' },
  { key: 'search',     label: '🔍 Search Arena',       shortName: '搜索',       icon: '🔍', status: 'live' },
  { key: 'copilot',    label: '🤖 Copilot Arena',      shortName: '代码助手',   icon: '🤖', status: 'live' }
]

// 每个能力的排行快照(总览用)
const categorySnapshots = ref({})  // { '': [...], 'code': [...], ... }
const totalVotesByCat = ref({})    // { '': 5, 'code': 10, ... }

const ROLE_RANK = { VISITOR: 1, SCORER: 2, ORGANIZER: 3, ADMIN: 4 }
const isVisitor = computed(() => auth.role === 'VISITOR')

async function loadAllSnapshots() {
  // 并发拉 5 个 category 的排行榜(后端 rankingByCategory)
  const entries = await Promise.all(
    categoryDefs.map(async def => {
      try {
        const r = await arenaRanking(def.key || undefined)
        return [def.key, Array.isArray(r) ? r : []]
      } catch (e) {
        return [def.key, []]
      }
    })
  )
  categorySnapshots.value = Object.fromEntries(entries)
  // 每个 category 的总票数(用所有参与模型的 games 总和 / 2 估算,或单独接口;简单起见用 sum of games/2)
  const votes = {}
  for (const def of categoryDefs) {
    const list = categorySnapshots.value[def.key] || []
    const total = list.reduce((s, m) => s + (m.games || 0), 0) / 2
    votes[def.key] = Math.round(total)
  }
  totalVotesByCat.value = votes
}

// 总览卡片数据
const categoryOverviews = computed(() => {
  return categoryDefs.map(def => {
    const list = categorySnapshots.value[def.key] || []
    return {
      key: def.key,
      label: def.label,
      shortName: def.shortName,
      icon: def.icon,
      status: def.status,
      rankings: list,
      totalVotes: totalVotesByCat.value[def.key] || 0,
      modelCount: list.length
    }
  })
})

function goRanking(category) {
  router.push({ name: 'ranking-detail', params: { category: category || 'all' } })
}

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
  await loadAllSnapshots()
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

    <!-- 排行榜总览:多能力卡片 + 点击跳详情(LMArena 风格) -->
    <section class="page-wrap">
      <div class="lb-section">
        <h2 class="lb-title">🏆 排行榜总览</h2>
        <div class="lb-underline"></div>
        <div class="lb-meta">
          基于 Arena 盲评投票 · Elo 算法 · K=32 · 点击「查看完整榜单」进入对应能力排行榜
        </div>

        <div class="lb-overview-grid">
          <div
            v-for="ov in categoryOverviews"
            :key="ov.key || 'all'"
            class="ov-card"
            :class="{ empty: ov.rankings.length === 0, soon: ov.status === 'soon' }"
            @click="goRanking(ov.key)"
          >
            <div class="ov-head">
              <span class="ov-icon">{{ ov.icon }}</span>
              <span class="ov-name">{{ ov.shortName }}</span>
              <el-tag v-if="ov.status === 'soon'" type="info" size="small" effect="plain" class="ov-soon-tag">即将上线</el-tag>
            </div>
            <template v-if="ov.rankings.length > 0">
              <div class="ov-top">
                <span class="ov-top-emoji">🥇</span>
                <div class="ov-top-info">
                  <div class="ov-top-name">{{ ov.rankings[0].modelName }}</div>
                  <div class="ov-top-provider">{{ ov.rankings[0].provider }}</div>
                </div>
                <div class="ov-top-elo">{{ ov.rankings[0].elo }}</div>
              </div>
              <div class="ov-mid">
                <div class="ov-mid-item"><span class="num">🥈</span> {{ ov.rankings[1]?.modelName || '—' }}</div>
                <div class="ov-mid-item"><span class="num">🥉</span> {{ ov.rankings[2]?.modelName || '—' }}</div>
              </div>
              <div class="ov-foot">
                <span>共 {{ ov.totalVotes }} 票 · {{ ov.modelCount }} 个模型</span>
                <el-button type="primary" link size="small" @click.stop="goRanking(ov.key)">
                  查看完整榜单 →
                </el-button>
              </div>
            </template>
            <template v-else>
              <div class="ov-empty">
                <template v-if="ov.status === 'soon'">
                  <div class="ov-empty-icon">🚧</div>
                  <div class="ov-empty-text">该能力即将上线</div>
                  <div class="ov-empty-hint">支持多模态/专业领域后开放</div>
                </template>
                <template v-else>
                  <div class="ov-empty-icon">📭</div>
                  <div class="ov-empty-text">该能力暂无投票数据</div>
                  <div class="ov-empty-hint">前往 Arena 跑题时选此分类</div>
                </template>
              </div>
              <div class="ov-foot">
                <span>{{ ov.status === 'soon' ? '暂未开放投票' : '前往 Arena 跑题时选此分类' }}</span>
                <el-button type="primary" link size="small" :disabled="ov.status === 'soon'" @click.stop="goRanking(ov.key)">
                  查看完整榜单 →
                </el-button>
              </div>
            </template>
          </div>
        </div>
      </div>
    </section>

    <!-- 关于 -->
    <section class="page-wrap">
      <h2 class="page-title">🎯 关于 EvalArena</h2>
      <p class="page-subtitle">通过盲测对比机制,让真实用户投票决定模型排名</p>

      <div class="about-grid">
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
        <div class="feature-card">
          <div class="icon">🏆</div>
          <div class="title">多能力分榜</div>
          <div class="desc">代码 / 写作 / 数学 / 视觉 / 搜索 等 9 个能力维度独立排名</div>
        </div>
        <div class="feature-card">
          <div class="icon">⚔️</div>
          <div class="title">一键盲评</div>
          <div class="desc">选 2 个模型 + 1 个问题,真模型实时对决,投票后揭晓</div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* 局部微调(主样式在 global.css) */
.hero { margin: -32px -24px 0; padding: 80px 24px 56px; }

/* 关于模块:3x2 整齐布局 */
.about-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
  margin-top: 16px;
}
@media (max-width: 1100px) {
  .about-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 700px) {
  .about-grid { grid-template-columns: 1fr; }
}

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

/* 能力分类 Tab */
.lb-tabs {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}
.lb-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 999px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-soft);
  cursor: pointer;
  transition: all .2s;
}
.lb-tab:hover {
  border-color: #6366f1;
  color: #4f46e5;
  transform: translateY(-1px);
}
.lb-tab.active {
  background: linear-gradient(90deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(99,102,241,.25);
}
.lb-tab-icon {
  font-size: 16px;
  line-height: 1;
}

/* 总览卡片网格(LMArena 风格)— 3 列整齐布局 */
.lb-overview-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
  margin-top: 16px;
}
.ov-card {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 18px 20px;
  cursor: pointer;
  transition: all .2s;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 220px;
}
.ov-card:hover {
  border-color: #6366f1;
  box-shadow: 0 8px 20px rgba(99,102,241,.15);
  transform: translateY(-2px);
}
.ov-card.empty { opacity: 0.92; }
.ov-card.soon {
  background: linear-gradient(135deg, #fafafa 0%, #f1f5f9 100%);
  border-style: dashed;
  cursor: not-allowed;
}
.ov-card.soon:hover {
  transform: none;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
  border-color: #cbd5e1;
}
.ov-soon-tag {
  margin-left: auto;
  background: #fff;
  border-color: #cbd5e1;
  color: #94a3b8;
}

.ov-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
}
.ov-icon { font-size: 22px; }
.ov-name {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.ov-top {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: linear-gradient(90deg, #fef9c3 0%, #fefce8 100%);
  border-radius: 8px;
  border-left: 3px solid #f59e0b;
}
.ov-top-emoji { font-size: 22px; }
.ov-top-info { flex: 1; min-width: 0; }
.ov-top-name {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ov-top-provider {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}
.ov-top-elo {
  font-size: 18px;
  font-weight: 800;
  color: #4f46e5;
  font-variant-numeric: tabular-nums;
}

.ov-mid {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: var(--text-soft);
}
.ov-mid-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 0;
}
.ov-mid-item .num {
  font-size: 14px;
  width: 18px;
  text-align: center;
}

.ov-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #94a3b8;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
}

.ov-empty {
  text-align: center;
  padding: 18px 0;
  color: #94a3b8;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
.ov-empty-icon { font-size: 36px; line-height: 1; }
.ov-empty-text { font-size: 13px; font-weight: 600; margin-top: 6px; color: #64748b; }
.ov-empty-hint { font-size: 11px; color: #94a3b8; margin-top: 4px; }

/* 响应式:窄屏 2 列,极窄 1 列 */
@media (max-width: 1100px) {
  .lb-overview-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 700px) {
  .lb-overview-grid { grid-template-columns: 1fr; }
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
