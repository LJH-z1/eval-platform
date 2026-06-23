<script setup>
/**
 * 主布局 — 左侧栏 + 浅蓝主页
 * <p>
 * 顶 nav 改成左列;主区域 #eff6ff 浅蓝底与侧栏区分。
 */
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { computed } from 'vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// 角色等级(VISITOR 最低,ADMIN 最高)
const ROLE_RANK = { VISITOR: 1, SCORER: 2, ORGANIZER: 3, ADMIN: 4 }

// 每个菜单项的最低角色门槛(访客 VISITOR 只能看到首页 + 对比评测 + 报告导出)
const navItems = [
  { name: 'dashboard',  label: '首页',      icon: '🏠', minRole: 'VISITOR' },
  { name: 'question',   label: '问题管理',  icon: '📝', minRole: 'SCORER' },
  { name: 'model',      label: '模型配置',  icon: '🤖', minRole: 'ORGANIZER' },
  { name: 'arena',      label: '对比评测',  icon: '⚔️', minRole: 'VISITOR' },
  { name: 'evaluation', label: '评测任务',  icon: '🚀', minRole: 'ORGANIZER' },
  { name: 'score',      label: '多维评分',  icon: '⭐', minRole: 'SCORER' },
  { name: 'stats',      label: '一致性分析',icon: '📊', minRole: 'SCORER' },
  { name: 'billing',    label: '成本统计',  icon: '💰', minRole: 'ORGANIZER' },
  { name: 'export',     label: '报告导出',  icon: '📥', minRole: 'VISITOR' }
]

const publicNavItems = [
  { name: 'register',   label: '注册',      icon: '✍️' }
]

const adminItems = [
  { name: 'users', label: '用户管理', icon: '👥' }
]

const visibleNavItems = computed(() => {
  if (!auth.isLoggedIn) return []
  const myRank = ROLE_RANK[auth.role] || 0
  return navItems.filter(n => myRank >= (ROLE_RANK[n.minRole] || 0))
})

function isActive(name) {
  return route.name === name || route.matched.some(r => r.name === name)
}

function go(name) {
  router.push({ name })
}

async function onLogout() {
  try {
    await ElMessageBox.confirm('确认退出登录吗?', '提示', { type: 'warning' })
  } catch (_) { return }
  auth.clear()
  ElMessage.success('已退出')
  router.push('/login')
}
</script>

<template>
  <div class="layout">
    <!-- 左侧栏 -->
    <aside class="sidebar">
      <div class="logo" @click="go('dashboard')">
        <span class="logo-mark">⚡</span>
        <div class="logo-text-wrap">
          <span class="logo-text">EvalArena</span>
          <span class="logo-sub">多模型评测平台</span>
        </div>
      </div>

      <nav class="nav">
        <a
          v-for="item in visibleNavItems"
          :key="item.name"
          class="nav-item"
          :class="{ active: isActive(item.name) }"
          @click="go(item.name)"
        >
          <span class="icon">{{ item.icon }}</span>
          <span class="label">{{ item.label }}</span>
        </a>
        <a
          v-for="item in adminItems"
          v-if="auth.isAdmin"
          :key="item.name"
          class="nav-item"
          :class="{ active: route.name === item.name }"
          @click="go(item.name)"
        >
          <span class="icon">{{ item.icon }}</span>
          <span class="label">{{ item.label }}</span>
        </a>
        <a
          v-for="item in publicNavItems"
          v-if="!auth.isLoggedIn"
          :key="item.name"
          class="nav-item public"
          :class="{ active: route.name === item.name }"
          @click="go(item.name)"
        >
          <span class="icon">{{ item.icon }}</span>
          <span class="label">{{ item.label }}</span>
        </a>
      </nav>

      <div class="sidebar-foot">
        <template v-if="auth.isLoggedIn">
          <el-dropdown trigger="click">
            <div class="user-chip">
              <el-avatar :size="32" style="margin-right:10px">
                {{ auth.user?.username?.[0]?.toUpperCase() || '?' }}
              </el-avatar>
              <div class="user-info">
                <div class="user-name">{{ auth.user?.username || '未登录' }}</div>
                <el-tag v-if="auth.isAdmin" type="danger" size="small">管理员</el-tag>
                <el-tag v-else-if="auth.role === 'ORGANIZER'" type="warning" size="small">组织者</el-tag>
                <el-tag v-else-if="auth.role === 'SCORER'" type="success" size="small">评分员</el-tag>
                <el-tag v-else type="info" size="small">访客</el-tag>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item divided @click="onLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" size="small" style="width:100%" @click="router.push('/login')">登录</el-button>
          <el-button size="small" style="width:100%;margin-top:8px" @click="router.push('/register')">注册</el-button>
        </template>
      </div>
    </aside>

    <!-- 主页 — 浅蓝底 -->
    <main class="content">
      <div class="content-inner">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
      <footer class="foot">
        <div class="foot-inner">
          <span>© 2025 EvalArena · 多模型对比评测平台</span>
          <span class="foot-links">
            <a>服务条款</a> · <a>隐私政策</a> · <a>联系我们</a>
          </span>
        </div>
      </footer>
    </main>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  background: var(--bg-soft);
}

/* ===== 左侧栏(浅蓝) ===== */
.sidebar {
  width: 220px;
  flex-shrink: 0;
  background: #eff6ff;
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
  height: 100vh;
  z-index: 10;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 18px;
  cursor: pointer;
  user-select: none;
  border-bottom: 1px solid var(--border);
}
.logo-mark {
  font-size: 26px;
  filter: drop-shadow(0 2px 4px rgba(37, 99, 235, .3));
}
.logo-text-wrap { display: flex; flex-direction: column; line-height: 1.1; }
.logo-text {
  font-size: 17px;
  font-weight: 800;
  letter-spacing: -0.02em;
  background: linear-gradient(90deg, #2563eb 0%, #7c3aed 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.logo-sub {
  font-size: 11px;
  color: var(--text-mute);
  margin-top: 3px;
}

.nav {
  flex: 1;
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-soft);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all .15s;
}
.nav-item:hover {
  background: var(--bg-soft);
  color: var(--text);
}
.nav-item.active {
  background: var(--primary-soft);
  color: var(--primary-text);
  font-weight: 600;
}
.nav-item .icon {
  font-size: 17px;
  width: 22px;
  text-align: center;
}
.nav-item .label { flex: 1; }

.sidebar-foot {
  padding: 12px 14px;
  border-top: 1px solid var(--border);
  background: #e0ecff;   /* 稍深一点,跟主区分割 */
}
.user-chip {
  display: flex;
  align-items: center;
  padding: 6px 4px;
  border-radius: 8px;
  cursor: pointer;
  transition: background .15s;
}
.user-chip:hover { background: #eef5ff; }
.user-info { display: flex; flex-direction: column; gap: 3px; flex: 1; }
.user-name { font-size: 13px; font-weight: 600; color: var(--text); }

/* ===== 主页(白底) ===== */
.content {
  flex: 1;
  min-width: 0;            /* 防止 flex 子项溢出 */
  background: #ffffff;
  display: flex;
  flex-direction: column;
}
.content-inner {
  flex: 1;
  padding: 24px 32px;
}

.foot {
  background: #ffffff;
  border-top: 1px solid var(--border);
  padding: 18px 0;
  margin-top: 32px;
}
.foot-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--text-mute);
}
.foot-links a {
  margin: 0 4px;
  cursor: pointer;
}
.foot-links a:hover { color: var(--primary); }

.fade-enter-active, .fade-leave-active { transition: opacity .15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ===== 响应式:窄屏收起侧栏 ===== */
@media (max-width: 900px) {
  .sidebar { width: 60px; }
  .logo-text-wrap, .nav-item .label, .user-info { display: none; }
  .nav-item { justify-content: center; padding: 12px 0; }
  .logo { justify-content: center; padding: 20px 0; }
  .content-inner { padding: 16px 20px; }
}
</style>
