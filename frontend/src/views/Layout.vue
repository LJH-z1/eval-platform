<script setup>
/**
 * 主布局 - LMArena 风格
 * <p>
 * 顶部简洁导航 + 干净的主区域。
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
    <!-- 顶部导航 -->
    <header class="topbar">
      <div class="topbar-inner">
        <div class="logo" @click="go('dashboard')">
          <span class="logo-mark">⚡</span>
          <span class="logo-text">EvalArena</span>
          <span class="logo-sub">多模型评测平台</span>
        </div>

        <nav class="nav">
          <a
            v-for="item in visibleNavItems"
            :key="item.name"
            class="nav-item"
            :class="{ active: route.name === item.name || route.matched.some(r => r.name === item.name) }"
            @click="go(item.name)"
          >
            <span class="icon">{{ item.icon }}</span>
            <span>{{ item.label }}</span>
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
            <span>{{ item.label }}</span>
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
            <span>{{ item.label }}</span>
          </a>
        </nav>

        <div class="user-area">
          <template v-if="auth.isLoggedIn">
            <el-dropdown trigger="click">
              <span class="user-chip">
                <el-avatar :size="28" style="margin-right:8px">
                  {{ auth.user?.username?.[0]?.toUpperCase() || '?' }}
                </el-avatar>
                <span>{{ auth.user?.username || '未登录' }}</span>
                <el-tag v-if="auth.isAdmin" type="danger" size="small" style="margin-left:6px">管理员</el-tag>
                <el-tag v-else-if="auth.role === 'ORGANIZER'" type="warning" size="small" style="margin-left:6px">组织者</el-tag>
                <el-tag v-else-if="auth.role === 'SCORER'" type="success" size="small" style="margin-left:6px">评分员</el-tag>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item divided @click="onLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" size="small" @click="router.push('/login')">登录</el-button>
            <el-button size="small" @click="router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- 主区域 -->
    <main class="main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 页脚 -->
    <footer class="foot">
      <div class="foot-inner">
        <span>© 2025 EvalArena · 多模型对比评测平台</span>
        <span class="foot-links">
          <a>服务条款</a> · <a>隐私政策</a> · <a>联系我们</a>
        </span>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-soft);
}

.topbar {
  background: var(--bg);
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(8px);
}
.topbar-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}
.logo-mark {
  font-size: 24px;
  filter: drop-shadow(0 2px 4px rgba(37, 99, 235, .3));
}
.logo-text {
  font-size: 19px;
  font-weight: 800;
  letter-spacing: -0.02em;
  background: linear-gradient(90deg, #2563eb 0%, #7c3aed 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.logo-sub {
  font-size: 12px;
  color: var(--text-mute);
  margin-left: 6px;
  padding-left: 8px;
  border-left: 1px solid var(--border);
}

.nav {
  display: flex;
  gap: 4px;
  flex: 1;
  overflow-x: auto;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-soft);
  border-radius: var(--radius-sm);
  cursor: pointer;
  white-space: nowrap;
  transition: all .15s;
}
.nav-item:hover {
  background: var(--bg-soft);
  color: var(--text);
}
.nav-item.active {
  background: var(--primary-soft);
  color: var(--primary-text);
}
.nav-item .icon {
  font-size: 16px;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 8px;
}
.user-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text);
}
.user-chip:hover {
  background: var(--bg-soft);
}

.main {
  flex: 1;
}

.foot {
  background: var(--bg);
  border-top: 1px solid var(--border);
  padding: 24px 0;
  margin-top: 48px;
}
.foot-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: var(--text-mute);
}
.foot-links a {
  margin: 0 4px;
  cursor: pointer;
}
.foot-links a:hover { color: var(--primary); }

.fade-enter-active, .fade-leave-active {
  transition: opacity .15s;
}
.fade-enter-from, .fade-leave-to { opacity: 0; }

@media (max-width: 900px) {
  .nav-item span:not(.icon) { display: none; }
  .logo-sub { display: none; }
}
</style>
