<script setup>
/**
 * 主布局 — 骨架
 * <p>
 * 由【靳磊】最终负责 UI 美化,本分支向锏楠提供了 question 模块的导航
 */
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

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
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background: #001529; color: #fff">
      <div style="padding: 18px 20px; font-size: 16px; font-weight: 600; color: #fff">
        评测平台
      </div>
      <el-menu
        default-active="/question"
        background-color="#001529"
        text-color="#bfcbd9"
        active-text-color="#fff"
        router
      >
        <el-menu-item index="/dashboard" :icon="'House'">首页</el-menu-item>
        <el-menu-item index="/profile"   :icon="'User'">个人中心</el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/users" :icon="'UserFilled'">用户管理</el-menu-item>

        <!-- FR-03 问题管理 -->
        <el-sub-menu index="/question">
          <template #title>
            <el-icon><Notebook /></el-icon>
            <span>问题管理</span>
          </template>
          <el-menu-item index="/question">题目列表</el-menu-item>
          <el-menu-item index="/question/new">新建问题</el-menu-item>
          <el-menu-item index="/question/import">批量导入</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/model"     :icon="'Cpu'">模型配置</el-menu-item>
        <el-menu-item index="/evaluation" :icon="'Operation'">评测</el-menu-item>
        <el-menu-item index="/score"     :icon="'EditPen'">评分</el-menu-item>
        <el-menu-item index="/stats"     :icon="'DataAnalysis'">一致性分析</el-menu-item>
        <el-menu-item index="/billing"   :icon="'Money'">成本统计</el-menu-item>
        <el-menu-item index="/export"    :icon="'Download'">报告导出</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background: #fff; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #ebeef5">
        <span style="font-size: 18px; font-weight: 600">{{ $route.meta.title || '评测平台' }}</span>
        <el-dropdown>
          <span style="cursor: pointer; display: flex; align-items: center">
            <el-avatar :size="32" style="margin-right: 10px">
              {{ auth.user?.username?.[0]?.toUpperCase() || '?' }}
            </el-avatar>
            <span>
              {{ auth.user?.username || '未登录' }}
              <el-tag v-if="auth.isAdmin" type="danger" size="small" style="margin-left: 6px">管理员</el-tag>
              <el-tag v-else-if="auth.role === 'ORGANIZER'" type="warning" size="small" style="margin-left: 6px">组织者</el-tag>
              <el-tag v-else-if="auth.role === 'SCORER'" type="success" size="small" style="margin-left: 6px">评分员</el-tag>
            </span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
              <el-dropdown-item divided @click="onLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main style="padding: 20px; background: #f5f7fa">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
