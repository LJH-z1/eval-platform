<script setup>
/**
 * 登录页 - LMArena 风格
 * <p>
 * 调用后端 POST /api/auth/login,成功后存 JWT + userInfo 到 Pinia + localStorage,跳转 /dashboard
 */
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login as apiLogin } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = reactive({ username: 'admin', password: 'admin123' })
const loading = ref(false)

async function onSubmit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await apiLogin(form)
    // 后端返回 {token, tokenType, expiresIn, userInfo: {id, username, role, email}}
    auth.token = data.token
    auth.user  = data.userInfo
    localStorage.setItem('eval_token', data.token)
    localStorage.setItem('eval_user', JSON.stringify(data.userInfo))
    ElMessage.success(`欢迎回来,${data.userInfo.username}`)
    const redirect = route.query.redirect || '/dashboard'
    router.push(redirect)
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-bg">
    <div class="auth-card">
      <div class="auth-logo">
        <span class="grad">⚡ EvalArena</span>
      </div>
      <div class="auth-sub">登录开始多模型盲测对比</div>

      <el-form @submit.prevent="onSubmit" label-position="top" size="large">
        <el-form-item label="用户名">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="'User'"
            autocomplete="username"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            :prefix-icon="'Lock'"
            autocomplete="current-password"
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-button
          type="primary"
          :loading="loading"
          @click="onSubmit"
          style="width:100%;height:44px;font-size:15px"
        >
          登录
        </el-button>
      </el-form>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-top:20px;font-size:12px"
      >
        <template #title>测试账号</template>
        admin / admin123(管理员)<br/>
        org1 / admin123(组织者)
      </el-alert>

      <div class="auth-foot">
        还没有账号?<router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>
