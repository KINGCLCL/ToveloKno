import axios from 'axios'

const TOKEN_KEY = 'tovelokno_token'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 8000,
})

api.interceptors.request.use((config) => {
  const token = getAuthToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

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

export default api
