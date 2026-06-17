<script setup>
/**
 * 登录页 — 骨架
 * <p>
 * 由【靳磊 前端 + 刘家豪 后端】联调
 * <p>
 * 后端契约:POST /api/auth/login {username, password} → {token, userInfo}
 */
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ModulePlaceholder from '@/components/ModulePlaceholder.vue'
import request from '@/utils/request'

const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function onSubmit() {
  loading.value = true
  try {
    // TODO 由靳磊实现:调用 /api/auth/login,成功后 Pinia store.login,跳转 /
    const data = await request.post('/auth/login', form)
    ElMessage.success(`欢迎 ${data.userInfo.username}`)
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-bg">
    <el-card class="login-card" shadow="always">
      <div class="login-title">多模型回答对比与评测平台</div>
      <ModulePlaceholder
        module="FR-01"
        owner="刘家豪(后端) + 靳磊(前端)"
        title="登录页骨架"
        :features="[
          '用户名 + 密码登录(刘家豪后端 /api/auth/login)',
          '登录后存 JWT 到 localStorage',
          '跳转 /dashboard',
          '注册入口跳到 /register',
          '错误信息由 axios 拦截器统一 ElMessage.error',
          '测试账号:admin / admin123(初始化在 SQL 脚本里)'
        ]"
      />
      <el-form @submit.prevent="onSubmit" label-position="top" style="margin-top: 16px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="onSubmit" style="width: 100%">
          登录(TODO 接通 API)
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>
