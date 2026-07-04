import axios from 'axios'

// 统一保存登录 token 的 localStorage key。
const TOKEN_KEY = 'tovelokno_token'

// 业务接口统一复用这个 axios 实例，避免每个模块重复配置 baseURL。
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 8000,
})

// 请求发出前自动带上登录 token。
api.interceptors.request.use((config) => {
  const token = getAuthToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 后端返回 401 时清掉本地登录态。
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearAuthToken()
    }
    return Promise.reject(error)
  },
)

export function getAuthToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setAuthToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  }
}

export function clearAuthToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function registerUser(data) {
  return api.post('/users/register', data)
}

export async function loginUser(data) {
  const response = await api.post('/users/login', data)
  const token = response.data?.data?.token
  if (token) {
    setAuthToken(token)
  }
  return response
}

export function logoutUser() {
  clearAuthToken()
}

export function getCurrentUserProfile() {
  return api.get('/users/me')
}

export function updateCurrentUserProfile(data) {
  return api.put('/users/me/profile', data)
}

export function changeCurrentUserPassword(data) {
  return api.put('/users/me/password', data)
}

export function getUserProfile(userId) {
  return api.get(`/users/${userId}`)
}

export function updateUserProfile(userId, data) {
  return api.put(`/users/${userId}/profile`, data)
}

export function changePassword(userId, data) {
  return api.put(`/users/${userId}/password`, data)
}

export function listCategories(params = {}) {
  return api.get('/categories', { params })
}

export function getCategory(categoryId) {
  return api.get(`/categories/${categoryId}`)
}

export function createCategory(data) {
  return api.post('/categories', data)
}

export function updateCategory(categoryId, data) {
  return api.put(`/categories/${categoryId}`, data)
}

export function deleteCategory(categoryId) {
  return api.delete(`/categories/${categoryId}`)
}

export function listTags(params = {}) {
  return api.get('/tags', { params })
}

export function getTag(tagId) {
  return api.get(`/tags/${tagId}`)
}

export function createTag(data) {
  return api.post('/tags', data)
}

export function updateTag(tagId, data) {
  return api.put(`/tags/${tagId}`, data)
}

export function deleteTag(tagId) {
  return api.delete(`/tags/${tagId}`)
}

export default api
