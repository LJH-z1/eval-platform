import request from '@/utils/request'

/**
 * 登录 — 对齐后端 POST /api/auth/login
 * @returns {Promise<{token,tokenType,expiresIn,userInfo}>}
 */
export function login(data) {
  return request.post('/auth/login', data)
}

/**
 * 注销
 */
export function logout() {
  return request.post('/auth/logout')
}

/**
 * 获取当前用户
 */
export function me() {
  return request.get('/auth/me')
}

/**
 * 修改密码
 */
export function changePassword(data) {
  return request.post('/auth/change-password', data)
}

/**
 * 用户分页查询(管理员)
 */
export function pageUsers(params) {
  return request.get('/users', { params })
}

/**
 * 禁用用户(管理员)
 */
export function disableUser(id) {
  return request.post(`/users/${id}/disable`)
}
