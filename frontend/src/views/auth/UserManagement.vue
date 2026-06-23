<script setup>
/**
 * 用户管理 — 完整版(仅 ADMIN 可见)
 * <p>
 * - 分页查询用户(GET /api/users)
 * - 新建用户(POST /api/auth/register,需要 ADMIN 登录)
 * - 禁用用户(POST /api/users/{id}/disable)
 */
import { reactive, ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  role: '',
  keyword: ''
})

const createDialog = ref(false)
const createForm = reactive({
  username: '',
  password: '',
  email: '',
  role: 'SCORER'
})
const createLoading = ref(false)

const roleOptions = [
  { value: 'SCORER',    label: '评分员' },
  { value: 'ORGANIZER', label: '组织者' },
  { value: 'VISITOR',   label: '访客' }
]
const roleLabel = { ADMIN: '系统管理员', ORGANIZER: '组织者', SCORER: '评分员', VISITOR: '访客' }
const roleColor = { ADMIN: 'danger', ORGANIZER: 'warning', SCORER: 'success', VISITOR: 'info' }

const roleFilter = [
  { value: '', label: '全部' },
  { value: 'ADMIN', label: '管理员' },
  { value: 'ORGANIZER', label: '组织者' },
  { value: 'SCORER', label: '评分员' },
  { value: 'VISITOR', label: '访客' }
]

async function loadList() {
  loading.value = true
  try {
    const r = await request.get('/users', { params: query })
    list.value = r.list || r.records || []
    total.value = r.total || 0
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.pageNum = 1
  loadList()
}

function onReset() {
  query.role = ''
  query.keyword = ''
  query.pageNum = 1
  loadList()
}

function onPageChange(p) {
  query.pageNum = p
  loadList()
}

function onCreate() {
  createForm.username = ''
  createForm.password = ''
  createForm.email = ''
  createForm.role = 'SCORER'
  createDialog.value = true
}

async function onCreateSubmit() {
  if (!createForm.username || !createForm.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  if (createForm.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  createLoading.value = true
  try {
    await request.post('/auth/register', {
      username: createForm.username,
      password: createForm.password,
      email: createForm.email,
      role: createForm.role
    })
    ElMessage.success(`用户 ${createForm.username} 创建成功`)
    createDialog.value = false
    loadList()
  } catch (e) {
    // 拦截器已提示
  } finally {
    createLoading.value = false
  }
}

async function onDisable(row) {
  try {
    await ElMessageBox.confirm(
      `确认禁用用户 "${row.username}" 吗?禁用后该用户将无法登录。`,
      '提示',
      { type: 'warning' }
    )
  } catch (_) { return }
  try {
    await request.post(`/users/${row.id}/disable`)
    ElMessage.success(`已禁用 ${row.username}`)
    loadList()
  } catch (e) {
    // 拦截器已提示
  }
}

async function onEnable(row) {
  try {
    await request.post(`/users/${row.id}/enable`)
    ElMessage.success(`已启用 ${row.username}`)
    loadList()
  } catch (e) {
    // 拦截器已提示
  }
}

onMounted(loadList)
</script>

<template>
  <div class="page-wrap-narrow">
    <h2 class="page-title">👥 用户管理</h2>
    <p class="page-subtitle">创建用户、禁用/启用账号、角色管理(仅管理员)</p>

    <el-card shadow="never">
      <!-- 工具栏 -->
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;gap:8px;flex-wrap:wrap">
        <el-space>
          <el-select v-model="query.role" placeholder="角色" clearable style="width:120px" @change="onSearch">
            <el-option v-for="r in roleFilter" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
          <el-input
            v-model="query.keyword"
            placeholder="搜索用户名/邮箱"
            style="width:200px"
            clearable
            @keyup.enter="onSearch"
            @clear="onSearch"
          />
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-space>
        <el-button type="primary" @click="onCreate" icon="Plus">新建用户</el-button>
      </div>

      <!-- 表格 -->
      <el-table v-loading="loading" :data="list" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="用户名" min-width="120">
          <template #default="{ row }">
            <strong>{{ row.username }}</strong>
            <el-tag v-if="row.id === 1" size="small" type="info" style="margin-left:6px">内置</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="roleColor[row.role]" size="small">{{ roleLabel[row.role] || row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180">
          <template #default="{ row }">
            <span v-if="row.email">{{ row.email }}</span>
            <span v-else style="color:#94a3b8">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" size="small">正常</el-tag>
            <el-tag v-else type="danger" size="small">禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" min-width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" size="small" type="danger" :disabled="row.id === 1" @click="onDisable(row)">
              禁用
            </el-button>
            <el-button v-else size="small" type="success" @click="onEnable(row)">
              启用
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div style="display:flex;justify-content:flex-end;margin-top:16px">
        <el-pagination
          v-model:current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next, jumper"
          @current-change="onPageChange"
        />
      </div>
    </el-card>

    <!-- 新建用户对话框 -->
    <el-dialog v-model="createDialog" title="新建用户" width="480px" :close-on-click-modal="false">
      <el-form :model="createForm" label-width="80px" size="default">
        <el-form-item label="用户名" required>
          <el-input v-model="createForm.username" placeholder="3-20 位字母/数字/下划线" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="createForm.password" type="password" show-password placeholder="6-20 位" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createForm.email" placeholder="可选" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-radio-group v-model="createForm.role">
            <el-radio-button v-for="r in roleOptions" :key="r.value" :value="r.value">
              {{ r.label }}
            </el-radio-button>
          </el-radio-group>
          <div style="margin-top:6px;font-size:12px;color:#94a3b8">
            ADMIN 角色只能由数据库直接修改,不允许通过此界面创建
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="onCreateSubmit">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>
