import axios from 'axios'

// 后端 Spring Boot 默认运行在 8080 端口。
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 8000,
})

export function registerUser(data) {
  return api.post('/users/register', data)
}

export function loginUser(data) {
  return api.post('/users/login', data)
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
