import axios from 'axios'

// 统一保存登录 token 的 localStorage key。
const TOKEN_KEY = 'tovelokno_token'

// 所有前端接口请求都建议复用这个 axios 实例，避免每个页面重复配置 baseURL。
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 8000,
})

const API_BASE_URL = api.defaults.baseURL || ''
const ASSET_BASE_URL = API_BASE_URL.replace(/\/api\/?$/, '')

export function resolveAssetUrl(url) {
  if (!url || /^(https?:)?\/\//.test(url) || url.startsWith('data:') || url.startsWith('blob:')) {
    return url
  }
  if (url.startsWith('/')) {
    return `${ASSET_BASE_URL}${url}`
  }
  return url
}

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

// 上传当前用户头像或个人主页背景图，type 为 avatar/background。
export function uploadCurrentUserProfileImage(file, type = 'avatar') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', type)
  return api.post('/users/me/profile-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
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

// 查询分类列表，keyword 可选。
export function listCategories(params = {}) {
  return api.get('/categories', { params })
}

// 查询单个分类详情。
export function getCategory(categoryId) {
  return api.get(`/categories/${categoryId}`)
}

// 新增分类。
export function createCategory(data) {
  return api.post('/categories', data)
}

// 修改分类。
export function updateCategory(categoryId, data) {
  return api.put(`/categories/${categoryId}`, data)
}

// 删除分类。
export function deleteCategory(categoryId) {
  return api.delete(`/categories/${categoryId}`)
}

// 查询标签列表，keyword 可选。
export function listTags(params = {}) {
  return api.get('/tags', { params })
}

// 查询单个标签详情。
export function getTag(tagId) {
  return api.get(`/tags/${tagId}`)
}

// 新增标签。
export function createTag(data) {
  return api.post('/tags', data)
}

// 修改标签。
export function updateTag(tagId, data) {
  return api.put(`/tags/${tagId}`, data)
}

// 删除标签。
export function deleteTag(tagId) {
  return api.delete(`/tags/${tagId}`)
}

// 查询学习计划列表，支持 status、planDate、page、size 参数。
export function listStudyPlans(params = {}) {
  return api.get('/study-plans', { params })
}

// 查询单条学习计划详情。
export function getStudyPlan(planId) {
  return api.get(`/study-plans/${planId}`)
}

// 新增学习计划。
export function createStudyPlan(data) {
  return api.post('/study-plans', data)
}

// 编辑学习计划。
export function updateStudyPlan(planId, data) {
  return api.put(`/study-plans/${planId}`, data)
}

// 删除学习计划。
export function deleteStudyPlan(planId) {
  return api.delete(`/study-plans/${planId}`)
}

// 修改学习计划状态（pending / completed / cancelled）。
export function updateStudyPlanStatus(planId, status) {
  return api.put(`/study-plans/${planId}/status`, { status })
}

// 默认导出 axios 实例，后续题库、错题本、计划、统计模块可以直接复用。
export default api
