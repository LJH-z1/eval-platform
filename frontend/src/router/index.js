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
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/Dashboard.vue'),                          meta: { title: '首页' } },
      { path: 'profile',   name: 'profile',   component: () => import('@/views/auth/Profile.vue'),                       meta: { title: '个人中心' } },
      { path: 'users',     name: 'users',     component: () => import('@/views/auth/UserManagement.vue'),               meta: { title: '用户管理', roles: ['ADMIN'] } },
      { path: 'model',     name: 'model',     component: () => import('@/views/model/ModelList.vue'),                    meta: { title: '模型配置' } },
      { path: 'question',  name: 'question',  component: () => import('@/views/question/QuestionList.vue'),              meta: { title: '问题管理' } },
      { path: 'evaluation',name: 'evaluation',component: () => import('@/views/evaluation/EvaluationMain.vue'),          meta: { title: '评测' } },
      { path: 'score',     name: 'score',     component: () => import('@/views/score/ScoreMain.vue'),                    meta: { title: '评分' } },
      { path: 'stats',     name: 'stats',     component: () => import('@/views/stats/StatsMain.vue'),                    meta: { title: '一致性分析' } },
      { path: 'billing',   name: 'billing',   component: () => import('@/views/billing/BillingMain.vue'),                meta: { title: '成本统计' } },
      { path: 'export',    name: 'export',    component: () => import('@/views/export/ExportMain.vue'),                  meta: { title: '报告导出' } }
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
