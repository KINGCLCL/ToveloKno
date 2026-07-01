<template>
  <main class="app-shell">
    <section class="hero-panel">
      <div class="brand-mark">TK</div>
      <p class="eyebrow">ToveloKno Demo</p>
      <h1>学习资源管理系统联调台</h1>
      <p class="hero-copy">
        当前页面用于验证 Vue3 前端是否能调用 Spring Boot 后端的用户接口。
      </p>

      <div class="status-card">
        <span class="status-label">当前用户</span>
        <strong>{{ currentUser ? currentUser.username : '未登录' }}</strong>
        <p v-if="currentUser">
          ID：{{ currentUser.id }} ｜ 角色：{{ currentUser.roles.join(', ') }}
        </p>
        <p v-else>请先注册或登录一个测试账号。</p>
      </div>

      <div class="api-result" :class="{ error: !lastResult.success && lastResult.message }">
        <span>接口结果</span>
        <pre>{{ resultText }}</pre>
      </div>
    </section>

    <section class="workspace">
      <div class="panel">
        <h2>注册账号</h2>
        <form @submit.prevent="handleRegister">
          <label>
            用户名
            <input v-model.trim="registerForm.username" placeholder="例如 testuser" />
          </label>
          <label>
            密码
            <input v-model="registerForm.password" type="password" placeholder="至少 6 位" />
          </label>
          <label>
            邮箱
            <input v-model.trim="registerForm.email" placeholder="test@example.com" />
          </label>
          <button type="submit" :disabled="loading">注册</button>
        </form>
      </div>

      <div class="panel">
        <h2>登录账号</h2>
        <form @submit.prevent="handleLogin">
          <label>
            用户名
            <input v-model.trim="loginForm.username" placeholder="输入用户名" />
          </label>
          <label>
            密码
            <input v-model="loginForm.password" type="password" placeholder="输入密码" />
          </label>
          <button type="submit" :disabled="loading">登录</button>
        </form>
      </div>

      <div class="panel">
        <h2>用户资料</h2>
        <div class="button-row">
          <button type="button" class="secondary" :disabled="!currentUser || loading" @click="handleGetProfile">
            查询资料
          </button>
        </div>
        <form @submit.prevent="handleUpdateProfile">
          <label>
            新邮箱
            <input v-model.trim="profileForm.email" placeholder="new@example.com" />
          </label>
          <label>
            头像地址
            <input v-model.trim="profileForm.avatar" placeholder="https://example.com/avatar.png" />
          </label>
          <button type="submit" :disabled="!currentUser || loading">修改资料</button>
        </form>
      </div>

      <div class="panel">
        <h2>修改密码</h2>
        <form @submit.prevent="handleChangePassword">
          <label>
            旧密码
            <input v-model="passwordForm.oldPassword" type="password" placeholder="当前密码" />
          </label>
          <label>
            新密码
            <input v-model="passwordForm.newPassword" type="password" placeholder="至少 6 位" />
          </label>
          <button type="submit" :disabled="!currentUser || loading">修改密码</button>
        </form>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import {
  changePassword,
  getUserProfile,
  loginUser,
  registerUser,
  updateUserProfile,
} from './api'

const loading = ref(false)
const currentUser = ref(null)
const lastResult = reactive({
  success: true,
  message: '等待操作',
  data: null,
})

const registerForm = reactive({
  username: 'testuser',
  password: '123456',
  email: 'testuser@example.com',
})

const loginForm = reactive({
  username: 'testuser',
  password: '123456',
})

const profileForm = reactive({
  email: 'new_testuser@example.com',
  avatar: 'https://example.com/avatar.png',
})

const passwordForm = reactive({
  oldPassword: '123456',
  newPassword: '654321',
})

const resultText = computed(() => JSON.stringify(lastResult, null, 2))

async function runRequest(request) {
  loading.value = true
  try {
    const response = await request()
    lastResult.success = response.data.success
    lastResult.message = response.data.message
    lastResult.data = response.data.data
    return response.data
  } catch (error) {
    lastResult.success = false
    lastResult.message = error.response?.data?.message || error.message || '请求失败'
    lastResult.data = null
    return null
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  const result = await runRequest(() => registerUser(registerForm))
  if (result?.success) {
    currentUser.value = result.data
    loginForm.username = registerForm.username
    loginForm.password = registerForm.password
  }
}

async function handleLogin() {
  const result = await runRequest(() => loginUser(loginForm))
  if (result?.success) {
    currentUser.value = result.data
    profileForm.email = result.data.email || ''
    profileForm.avatar = result.data.avatar || ''
  }
}

async function handleGetProfile() {
  const result = await runRequest(() => getUserProfile(currentUser.value.id))
  if (result?.success) {
    currentUser.value = result.data
  }
}

async function handleUpdateProfile() {
  const result = await runRequest(() => updateUserProfile(currentUser.value.id, profileForm))
  if (result?.success) {
    currentUser.value = result.data
  }
}

async function handleChangePassword() {
  const result = await runRequest(() => changePassword(currentUser.value.id, passwordForm))
  if (result?.success) {
    loginForm.password = passwordForm.newPassword
    passwordForm.oldPassword = passwordForm.newPassword
    passwordForm.newPassword = ''
  }
}
</script>
