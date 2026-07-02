import axios from 'axios'

// 统一保存登录 token 的 localStorage key。
const TOKEN_KEY = 'tovelokno_token'

// 所有前端接口请求都建议复用这个 axios 实例，避免每个页面重复配置 baseURL。
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 8000,
})

// 请求发出前自动带上登录 token。
// 后续业务模块通过 api.get/api.post 调接口时，不需要手动写 Authorization。
api.interceptors.request.use((config) => {
  const token = getAuthToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 如果后端返回 401，说明登录状态失效，前端先清掉本地 token。
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearAuthToken()
    }
    return Promise.reject(error)
  },
)

// 读取本地 token，给路由守卫或页面状态判断复用。
export function getAuthToken() {
  return localStorage.getItem(TOKEN_KEY)
}

// 保存登录接口返回的 token。
export function setAuthToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  }
}

// 清除登录态，退出登录或 token 失效时调用。
export function clearAuthToken() {
  localStorage.removeItem(TOKEN_KEY)
}

// 用户注册。注册成功后仍需要调用登录接口获取 token。
export function registerUser(data) {
  return api.post('/users/register', data)
}

// 用户登录。登录成功后自动保存 token，后续请求会自动带上。
export async function loginUser(data) {
  const response = await api.post('/users/login', data)
  const token = response.data?.data?.token
  if (token) {
    setAuthToken(token)
  }
  return response
}

// 前端退出登录只需要清掉本地 token；当前后端没有服务端 session。
export function logoutUser() {
  clearAuthToken()
}

// 查询当前登录用户资料。
export function getCurrentUserProfile() {
  return api.get('/users/me')
}

// 修改当前登录用户资料。
export function updateCurrentUserProfile(data) {
  return api.put('/users/me/profile', data)
}

// 修改当前登录用户密码。
export function changeCurrentUserPassword(data) {
  return api.put('/users/me/password', data)
}

// 兼容按 userId 查询资料的接口，普通用户只能查自己。
export function getUserProfile(userId) {
  return api.get(`/users/${userId}`)
}

// 兼容按 userId 修改资料的接口，Service 层会做权限校验。
export function updateUserProfile(userId, data) {
  return api.put(`/users/${userId}/profile`, data)
}

// 兼容按 userId 修改密码的接口。
export function changePassword(userId, data) {
  return api.put(`/users/${userId}/password`, data)
}

// 默认导出 axios 实例，后续题库、错题本、计划、统计模块可以直接复用。
export default api
