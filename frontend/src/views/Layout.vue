<script setup>
/**
 * 主布局 — 骨架
 * <p>
 * 由【靳磊】实现(侧边栏菜单 + 顶部用户信息)
 * <p>
 * 当前为最简占位,保留顶栏和退出按钮
 */
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

async function onLogout() {
  // TODO 由靳磊实现:调用 /api/auth/logout,清空 store,跳 /login
  ElMessage.info('TODO 退出登录(联调后端)')
  auth.clear()
  router.push('/login')
}
</script>

<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background: #001529; color: #fff">
      <div style="padding: 18px 20px; font-size: 16px; font-weight: 600">
        评测平台
      </div>
      <el-menu
        default-active="/dashboard"
        background-color="#001529"
        text-color="#bfcbd9"
        active-text-color="#fff"
        router
      >
        <el-menu-item index="/dashboard">首页</el-menu-item>
        <el-menu-item index="/profile">个人中心</el-menu-item>
        <el-menu-item index="/users" v-if="auth.isAdmin">用户管理</el-menu-item>
        <el-menu-item index="/model">模型配置</el-menu-item>
        <el-menu-item index="/question">问题管理</el-menu-item>
        <el-menu-item index="/evaluation">评测</el-menu-item>
        <el-menu-item index="/score">评分</el-menu-item>
        <el-menu-item index="/stats">一致性分析</el-menu-item>
        <el-menu-item index="/billing">成本统计</el-menu-item>
        <el-menu-item index="/export">报告导出</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background: #fff; display: flex; align-items: center; justify-content: space-between">
        <span>多模型回答对比与评测平台 — 架构骨架版</span>
        <span>
          {{ auth.user?.username || '未登录' }}
          <el-button link @click="onLogout" style="margin-left: 12px">退出</el-button>
        </span>
      </el-header>
      <el-main style="padding: 20px; background: #f5f7fa">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
