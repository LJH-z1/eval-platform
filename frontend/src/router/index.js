import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login',    name: 'login',    component: () => import('@/views/auth/Login.vue'),       meta: { public: true, title: '登录' } },
  { path: '/register', name: 'register', component: () => import('@/views/auth/Register.vue'),    meta: { public: true, title: '注册' } },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/Dashboard.vue'),                meta: { title: '首页' } },
      { path: 'profile',   name: 'profile',   component: () => import('@/views/auth/Profile.vue'),             meta: { title: '个人中心' } },
      { path: 'users',     name: 'users',     component: () => import('@/views/auth/UserManagement.vue'),     meta: { title: '用户管理', roles: ['ADMIN'] } },

      // ========== FR-02 模型配置(向锏楠 已实现)==========
      { path: 'model',          name: 'model',         component: () => import('@/views/model/ModelList.vue'),  meta: { title: '模型配置' } },
      { path: 'model/new',      name: 'model-new',     component: () => import('@/views/model/ModelForm.vue'), meta: { title: '新建模型' } },
      { path: 'model/:id/edit', name: 'model-edit',    component: () => import('@/views/model/ModelForm.vue'), meta: { title: '编辑模型' } },
      { path: 'model/:id/test', name: 'model-test',    component: () => import('@/views/model/ModelTest.vue'), meta: { title: '连接测试' } },

      // ========== Arena 对比评测(LMArena 风格)==========
      { path: 'arena',          name: 'arena',         component: () => import('@/views/arena/Arena.vue'), meta: { title: '对比评测' } },

      // ========== FR-03 问题管理(向锏楠 已实现)==========
      { path: 'question',          name: 'question',       component: () => import('@/views/question/QuestionList.vue'),   meta: { title: '问题管理' } },
      { path: 'question/new',      name: 'question-new',   component: () => import('@/views/question/QuestionForm.vue'),  meta: { title: '新建问题' } },
      { path: 'question/:id/edit', name: 'question-edit',  component: () => import('@/views/question/QuestionForm.vue'),  meta: { title: '编辑问题' } },
      { path: 'question/import',   name: 'question-import',component: () => import('@/views/question/QuestionImport.vue'), meta: { title: '批量导入' } },

      // 其它模块路由(待各负责人实现)
      { path: 'evaluation',name: 'evaluation',component: () => import('@/views/evaluation/EvaluationMain.vue'), meta: { title: '评测' } },
      { path: 'score',     name: 'score',     component: () => import('@/views/score/ScoreMain.vue'),       meta: { title: '评分' } },
      { path: 'stats',     name: 'stats',     component: () => import('@/views/stats/StatsMain.vue'),       meta: { title: '一致性分析' } },
      { path: 'billing',   name: 'billing',   component: () => import('@/views/billing/BillingMain.vue'),   meta: { title: '成本统计' } },
      { path: 'export',    name: 'export',    component: () => import('@/views/export/ExportMain.vue'),     meta: { title: '报告导出' } }
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFound.vue'), meta: { public: true } }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.title) document.title = `${to.meta.title} - 评测平台`
  if (to.meta.public) {
    if (to.name === 'login' && auth.isLoggedIn) return '/'
    return true
  }
  if (!auth.isLoggedIn) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.meta.roles && !to.meta.roles.includes(auth.role)) return '/'
  return true
})

export default router
