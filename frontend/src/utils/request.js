import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * Axios 实例 + 拦截器 — 由【靳磊】维护
 * <p>
 * - Request 拦截器:自动加 JWT
 * - Response 拦截器:统一处理 401/403/429/500
 * - 业务码 ≠ 200 → 自动 ElMessage.error
 */
const request = axios.create({ baseURL: '/api', timeout: 30000 })

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('eval_token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
  },
  (err) => Promise.reject(err)
)

request.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) return body.data
      ElMessage.error(body.message || '操作失败')
      return Promise.reject(new Error(body.message || 'Error'))
    }
    return body
  },
  (err) => {
    const status = err.response?.status
    const message = err.response?.data?.message
    if (status === 401) { ElMessage.error(message || '登录已过期'); localStorage.clear(); location.href = '/login' }
    else if (status === 403) ElMessage.error(message || '无权限访问')
    else if (status === 429) ElMessage.error('操作过于频繁')
    else if (status >= 500) ElMessage.error(message || '服务器开了小差')
    else ElMessage.error(message || '请求失败')
    return Promise.reject(err)
  }
)

export default request
