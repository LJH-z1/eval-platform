<script setup>
/**
 * 注册页 - LMArena 风格
 * <p>
 * 对齐后端 POST /api/auth/register(需 ADMIN 登录后),权限不足时给提示
 */
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({
  username: '',
  password: '',
  confirm: '',
  email: '',
  role: 'SCORER'
})
const loading = ref(false)

const roleOptions = [
  { value: 'SCORER',    label: '评分员',   desc: '参与多维评分' },
  { value: 'ORGANIZER', label: '组织者',   desc: '创建评测、配置模型' },
  { value: 'VISITOR',   label: '访客',     desc: '仅查看' }
]

async function onSubmit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  if (form.password !== form.confirm) {
    ElMessage.error('两次密码不一致')
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  loading.value = true
  try {
    await request.post('/auth/register', {
      username: form.username,
      password: form.password,
      email: form.email,
      role: form.role
    })
    ElMessage.success('注册成功,请登录')
    router.push('/login')
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
      <div class="auth-sub">创建你的评测账号</div>

      <el-alert
        v-if="!auth.isLoggedIn"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom:16px;font-size:12px"
      >
        <template #title>需要管理员协助</template>
        当前注册需要管理员身份调用 API。请联系管理员创建账号,或先用 admin/admin123 登录。
      </el-alert>

      <el-form @submit.prevent="onSubmit" label-position="top" size="large">
        <el-form-item label="用户名(3-20 位)">
          <el-input v-model="form.username" placeholder="字母 / 数字 / 下划线" :prefix-icon="'User'" />
        </el-form-item>
        <el-form-item label="密码(6-20 位,字母+数字)">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" :prefix-icon="'Lock'" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="form.confirm" type="password" show-password placeholder="再次输入密码" :prefix-icon="'Lock'" />
        </el-form-item>
        <el-form-item label="邮箱(可选)">
          <el-input v-model="form.email" placeholder="用于找回密码" :prefix-icon="'Message'" />
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="form.role">
            <el-radio-button v-for="r in roleOptions" :key="r.value" :value="r.value">
              {{ r.label }}
            </el-radio-button>
          </el-radio-group>
          <div style="margin-top:6px;font-size:12px;color:#94a3b8">
            {{ roleOptions.find(r => r.value === form.role)?.desc }}
          </div>
        </el-form-item>
        <el-button
          type="primary"
          :loading="loading"
          @click="onSubmit"
          :disabled="!auth.isLoggedIn"
          style="width:100%;height:44px;font-size:15px"
        >
          注册
        </el-button>
      </el-form>

      <div class="auth-foot">
        已有账号?<router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>
