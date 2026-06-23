<script setup>
/**
 * 个人中心 — 完整版
 * <p>
 * - 展示当前用户信息
 * - 修改密码
 * - 退出登录
 */
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const profile = ref({})
const loading = ref(false)

const pwdForm = reactive({
  oldPwd: '',
  newPwd: '',
  confirm: ''
})
const pwdLoading = ref(false)
const pwdRules = {
  oldPwd: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPwd: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需 6-20 位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, cb) => {
        if (value !== pwdForm.newPwd) cb(new Error('两次密码不一致'))
        else cb()
      },
      trigger: 'blur'
    }
  ]
}

const roleLabel = { ADMIN: '系统管理员', ORGANIZER: '组织者', SCORER: '评分员', VISITOR: '访客' }
const roleColor = { ADMIN: 'danger', ORGANIZER: 'warning', SCORER: 'success', VISITOR: 'info' }

async function loadProfile() {
  loading.value = true
  try {
    const r = await request.get('/auth/me')
    profile.value = r || {}
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function onChangePassword() {
  if (pwdForm.newPwd !== pwdForm.confirm) {
    ElMessage.error('两次新密码不一致')
    return
  }
  pwdLoading.value = true
  try {
    await request.post('/auth/change-password', {
      oldPassword: pwdForm.oldPwd,
      newPassword: pwdForm.newPwd
    })
    ElMessage.success('密码修改成功,请重新登录')
    pwdForm.oldPwd = ''
    pwdForm.newPwd = ''
    pwdForm.confirm = ''
    setTimeout(() => onLogout(), 1000)
  } catch (e) {
    // 拦截器已提示
  } finally {
    pwdLoading.value = false
  }
}

async function onLogout() {
  try {
    await ElMessageBox.confirm('确认退出登录吗?', '提示', { type: 'warning' })
  } catch (_) { return }
  try { await request.post('/auth/logout') } catch (_) {}
  auth.clear()
  ElMessage.success('已退出登录')
  router.push('/login')
}

onMounted(loadProfile)
</script>

<template>
  <div class="page-wrap-narrow">
    <h2 class="page-title">👤 个人中心</h2>
    <p class="page-subtitle">查看账号信息、修改密码、退出登录</p>

    <el-row :gutter="16">
      <!-- 用户信息 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span style="font-size:16px;font-weight:600">📋 账号信息</span>
          </template>
          <div v-loading="loading">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="用户名">
                <strong>{{ profile.username || '-' }}</strong>
              </el-descriptions-item>
              <el-descriptions-item label="用户 ID">{{ profile.id || '-' }}</el-descriptions-item>
              <el-descriptions-item label="角色">
                <el-tag :type="roleColor[profile.role]" size="small">
                  {{ roleLabel[profile.role] || profile.role }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ profile.email || '(未设置)' }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag v-if="profile.status === 1" type="success" size="small">正常</el-tag>
                <el-tag v-else type="danger" size="small">已禁用</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="注册时间">{{ profile.createdAt || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>

      <!-- 快捷操作 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <span style="font-size:16px;font-weight:600">⚡ 快捷操作</span>
          </template>
          <el-space direction="vertical" style="width:100%">
            <el-button type="danger" plain size="large" style="width:100%" @click="onLogout">
              🚪 退出登录
            </el-button>
            <el-divider style="margin:8px 0" />
            <div style="font-size:12px;color:#94a3b8;line-height:1.6">
              <p>· 退出后跳转登录页</p>
              <p>· 修改密码后会自动退出,需重新登录</p>
              <p>· 账号被禁用请联系管理员</p>
            </div>
          </el-space>
        </el-card>
      </el-col>
    </el-row>

    <!-- 修改密码 -->
    <el-card shadow="never" style="margin-top:16px">
      <template #header>
        <span style="font-size:16px;font-weight:600">🔒 修改密码</span>
      </template>
      <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="100px" style="max-width:480px">
        <el-form-item label="当前密码" prop="oldPwd">
          <el-input v-model="pwdForm.oldPwd" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPwd">
          <el-input v-model="pwdForm.newPwd" type="password" show-password placeholder="6-20 位,字母+数字" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirm">
          <el-input v-model="pwdForm.confirm" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="pwdLoading" @click="onChangePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.el-col { margin-bottom: 16px; }
</style>
