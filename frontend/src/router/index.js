import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

/**
 * 路由元数据说明:
 * - meta.roles: 允许访问的角色列表;未配置默认所有登录用户可进
 * - meta.public: true 表示无需登录
 * - meta.minRole: 最低可访问的角色(简化配置,VISITOR 最低,ADMIN 最高)
 */
const ROLES = { VISITOR: 1, SCORER: 2, ORGANIZER: 3, ADMIN: 4 }

const routes = [
  { path: '/login',    name: 'login',    component: () => import('@/views/auth/Login.vue'),       meta: { public: true, title: '登录' } },
  { path: '/register', name: 'register', component: () => import('@/views/auth/Register.vue'),    meta: { public: true, title: '注册' } },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/login',
    children: [
      // 首页(所有登录用户)
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/Dashboard.vue'),       meta: { title: '首页' } },
      // 个人中心(所有登录用户)
      { path: 'profile',   name: 'profile',   component: () => import('@/views/auth/Profile.vue'),  meta: { title: '个人中心' } },
      // 用户管理(仅 ADMIN)
      { path: 'users',     name: 'users',     component: () => import('@/views/auth/UserManagement.vue'), meta: { title: '用户管理', roles: ['ADMIN'] } },

      // ========== FR-02 模型配置(ORGANIZER + ADMIN)==========
      { path: 'model',          name: 'model',         component: () => import('@/views/model/ModelList.vue'),  meta: { title: '模型配置', minRole: 'ORGANIZER' } },
      { path: 'model/new',      name: 'model-new',     component: () => import('@/views/model/ModelForm.vue'), meta: { title: '新建模型', minRole: 'ADMIN' } },
      { path: 'model/:id/edit', name: 'model-edit',    component: () => import('@/views/model/ModelForm.vue'), meta: { title: '编辑模型', minRole: 'ADMIN' } },
      { path: 'model/:id/test', name: 'model-test',    component: () => import('@/views/model/ModelTest.vue'), meta: { title: '连接测试', minRole: 'ORGANIZER' } },

      // ========== Arena 对比评测(所有登录用户,访客只读)==========
      { path: 'arena', name: 'arena', component: () => import('@/views/arena/Arena.vue'), meta: { title: '对比评测' } },

      // ========== FR-03 问题管理(SCORER 起;访客不可)==========
      { path: 'question',          name: 'question',       component: () => import('@/views/question/QuestionList.vue'),   meta: { title: '问题管理', minRole: 'SCORER' } },
      { path: 'question/new',      name: 'question-new',   component: () => import('@/views/question/QuestionForm.vue'),  meta: { title: '新建问题', minRole: 'ORGANIZER' } },
      { path: 'question/:id/edit', name: 'question-edit',  component: () => import('@/views/question/QuestionForm.vue'),  meta: { title: '编辑问题', minRole: 'ORGANIZER' } },
      { path: 'question/import',   name: 'question-import',component: () => import('@/views/question/QuestionImport.vue'), meta: { title: '批量导入', minRole: 'ORGANIZER' } },

      // ========== FR-04 评测任务(ORGANIZER 起)==========
      { path: 'evaluation',        name: 'evaluation',         component: () => import('@/views/evaluation/EvaluationList.vue'),   meta: { title: '评测任务', minRole: 'ORGANIZER' } },
      { path: 'evaluation/new',    name: 'evaluation-new',     component: () => import('@/views/evaluation/EvaluationCreate.vue'), meta: { title: '新建评测', minRole: 'ORGANIZER' } },
      { path: 'evaluation/:id',    name: 'evaluation-detail',  component: () => import('@/views/evaluation/EvaluationDetail.vue'), meta: { title: '评测详情', minRole: 'SCORER' } },

      // ========== FR-05 评分(SCORER 起)==========
      { path: 'score',     name: 'score',      component: () => import('@/views/score/ScoreMain.vue'), meta: { title: '多维评分', minRole: 'SCORER' } },
      { path: 'score/:id', name: 'score-form', component: () => import('@/views/score/ScoreForm.vue'), meta: { title: '评分', minRole: 'SCORER' } },

      // ========== FR-06 一致性分析(SCORER 起)==========
      { path: 'stats', name: 'stats', component: () => import('@/views/stats/StatsMain.vue'), meta: { title: '一致性分析', minRole: 'SCORER' } },

      // ========== FR-07 成本统计(ORGANIZER 起)==========
      { path: 'billing', name: 'billing', component: () => import('@/views/billing/BillingMain.vue'), meta: { title: '成本统计', minRole: 'ORGANIZER' } },

      // ========== FR-08 报告导出(所有登录用户)==========
      { path: 'export', name: 'export', component: () => import('@/views/export/ExportMain.vue'), meta: { title: '报告导出' } }
    ]
  },
  // 排行榜详情(从 Dashboard 总览点击进入)
  { path: '/ranking/:category', name: 'ranking-detail', component: () => import('@/views/RankingDetail.vue'), meta: { title: '排行榜详情', public: true } },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFound.vue'), meta: { public: true } }
]

const router = createRouter({ history: createWebHistory(), routes })

function roleAllowed(to, auth) {
  if (to.meta.public) return { ok: true }
  if (!auth.isLoggedIn) return { ok: false, reason: 'login' }
  // 优先用 meta.roles 显式列表
  if (Array.isArray(to.meta.roles) && to.meta.roles.length > 0) {
    if (!to.meta.roles.includes(auth.role)) return { ok: false, reason: 'forbidden' }
  }
  // 否则用 minRole 最低门槛
  if (to.meta.minRole) {
    const need = ROLES[to.meta.minRole] || 0
    const have = ROLES[auth.role] || 0
    if (have < need) return { ok: false, reason: 'forbidden' }
  }
  return { ok: true }
}

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.title) document.title = `${to.meta.title} - 评测平台`
  if (to.name === 'login' && auth.isLoggedIn) return '/dashboard'
  if (to.name === 'register' && auth.isLoggedIn) return '/dashboard'
  const r = roleAllowed(to, auth)
  if (r.ok) return true
  if (r.reason === 'login') {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (r.reason === 'forbidden') {
    ElMessage.warning(`当前角色「${auth.roleLabel || auth.role}」无权限访问该页面`)
    return { path: '/' }
  }
  return true
})

export default router
