<template>
  <main class="app-shell">
    <section v-if="!isLoggedIn" class="auth-layout">
      <div class="brand-panel">
        <p class="eyebrow">ToveloKno</p>
        <h1>把学习资料、计划和复习节奏收回到自己手里。</h1>
        <p class="brand-copy">
          先完成登录闭环，后续资源、题库、错题本和统计模块都可以直接接入当前用户状态。
        </p>

        <div class="signal-grid">
          <article>
            <span>01</span>
            <strong>统一身份</strong>
            <p>登录后接口自动携带 token。</p>
          </article>
          <article>
            <span>02</span>
            <strong>个人主页</strong>
            <p>展示账号信息和基础入口。</p>
          </article>
          <article>
            <span>03</span>
            <strong>模块承接</strong>
            <p>给后续功能预留清晰位置。</p>
          </article>
        </div>
      </div>

      <section class="auth-card">
        <div class="auth-tabs">
          <button type="button" :class="{ active: authMode === 'login' }" @click="switchAuthMode('login')">
            登录
          </button>
          <button type="button" :class="{ active: authMode === 'register' }" @click="switchAuthMode('register')">
            注册
          </button>
        </div>

        <form class="auth-form" @submit.prevent="submitAuth">
          <label>
            <span>用户名</span>
            <input v-model.trim="authForm.username" type="text" autocomplete="username" placeholder="请输入用户名" />
          </label>

          <label>
            <span>密码</span>
            <input
              v-model="authForm.password"
              type="password"
              :autocomplete="authMode === 'login' ? 'current-password' : 'new-password'"
              placeholder="请输入密码"
            />
          </label>

          <label v-if="authMode === 'register'">
            <span>邮箱</span>
            <input v-model.trim="authForm.email" type="email" autocomplete="email" placeholder="可选" />
          </label>

          <p v-if="message.text" class="message-line" :class="message.type">{{ message.text }}</p>

          <button class="primary-action" type="submit" :disabled="loading">
            {{ loading ? '处理中...' : authMode === 'login' ? '进入个人主页' : '创建账号' }}
          </button>
        </form>
      </section>
    </section>

    <section v-else class="home-layout">
      <aside class="side-nav">
        <div class="mini-brand">
          <span>TK</span>
          <div>
            <strong>ToveloKno</strong>
            <p>个人学习中心</p>
          </div>
        </div>

        <nav>
          <button
            v-for="item in navItems"
            :key="item.id"
            type="button"
            :class="{ active: activePanel === item.id }"
            @click="activePanel = item.id"
          >
            <span>{{ item.icon }}</span>
            {{ item.label }}
          </button>
        </nav>

        <button class="ghost-action" type="button" @click="handleLogout">退出登录</button>
      </aside>

      <section class="home-main">
        <header class="home-header">
          <div>
            <p class="eyebrow">欢迎回来</p>
            <h2>{{ currentUser?.username || '学习者' }}</h2>
          </div>
          <button class="refresh-action" type="button" :disabled="loading" @click="loadCurrentUser">
            {{ loading ? '刷新中' : '刷新资料' }}
          </button>
        </header>

        <section v-if="activePanel === 'dashboard'" class="dashboard-grid">
          <article class="profile-card">
            <div class="avatar-badge">{{ userInitial }}</div>
            <div>
              <p>当前账号</p>
              <h3>{{ currentUser.username }}</h3>
              <span>{{ currentUser.email || '暂未填写邮箱' }}</span>
            </div>
          </article>

          <article v-for="item in overviewCards" :key="item.label" class="metric-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <p>{{ item.note }}</p>
          </article>
        </section>

        <section v-if="activePanel === 'profile'" class="panel-card">
          <header>
            <h3>个人资料</h3>
            <p>这里先接入最基础的邮箱和头像地址修改。</p>
          </header>

          <form class="settings-form" @submit.prevent="submitProfile">
            <label>
              <span>用户名</span>
              <input :value="currentUser.username" type="text" disabled />
            </label>

            <label>
              <span>邮箱</span>
              <input v-model.trim="profileForm.email" type="email" placeholder="例如 user@example.com" />
            </label>

            <label>
              <span>头像地址</span>
              <input v-model.trim="profileForm.avatar" type="text" placeholder="图片 URL，可选" />
            </label>

            <button class="primary-action narrow" type="submit" :disabled="loading">
              保存资料
            </button>
          </form>
        </section>

        <section v-if="activePanel === 'security'" class="panel-card">
          <header>
            <h3>账号安全</h3>
            <p>修改密码后请使用新密码重新登录。</p>
          </header>

          <form class="settings-form" @submit.prevent="submitPassword">
            <label>
              <span>旧密码</span>
              <input v-model="passwordForm.oldPassword" type="password" autocomplete="current-password" />
            </label>

            <label>
              <span>新密码</span>
              <input v-model="passwordForm.newPassword" type="password" autocomplete="new-password" />
            </label>

            <label>
              <span>确认新密码</span>
              <input v-model="passwordForm.confirmPassword" type="password" autocomplete="new-password" />
            </label>

            <button class="primary-action narrow" type="submit" :disabled="loading">
              修改密码
            </button>
          </form>
        </section>

        <section v-if="activePanel === 'modules'" class="panel-card">
          <header>
            <h3>模块入口</h3>
            <p>这里先放最基本入口，等组员模块完成后再替换成真实页面。</p>
          </header>

          <div class="module-list">
            <article v-for="module in moduleCards" :key="module.title">
              <span>{{ module.index }}</span>
              <div>
                <strong>{{ module.title }}</strong>
                <p>{{ module.description }}</p>
              </div>
            </article>
          </div>
        </section>

        <p v-if="message.text" class="message-line floating" :class="message.type">{{ message.text }}</p>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  changeCurrentUserPassword,
  clearAuthToken,
  getAuthToken,
  getCurrentUserProfile,
  loginUser,
  registerUser,
  updateCurrentUserProfile,
} from './api'

const authMode = ref('login')
const activePanel = ref('dashboard')
const currentUser = ref(null)
const loading = ref(false)
const message = reactive({ type: '', text: '' })

const authForm = reactive({
  username: '',
  password: '',
  email: '',
})

const profileForm = reactive({
  email: '',
  avatar: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const navItems = [
  { id: 'dashboard', label: '主页概览', icon: '⌂' },
  { id: 'profile', label: '个人资料', icon: '◎' },
  { id: 'security', label: '账号安全', icon: '◈' },
  { id: 'modules', label: '模块入口', icon: '▦' },
]

const overviewCards = [
  { label: '学习资料', value: '待接入', note: '资源模块完成后显示数量' },
  { label: '错题本', value: '待接入', note: '错题模块完成后显示趋势' },
  { label: '学习计划', value: '待接入', note: '计划模块完成后显示今日任务' },
]

const moduleCards = [
  { index: '01', title: '统计模块', description: '首页统计、资源数量、练习次数、正确率、错题趋势。' },
  { index: '02', title: '错题本模块', description: '错题列表、标记掌握、移出错题本。' },
  { index: '03', title: '学习计划模块', description: '计划新增、查询、编辑、删除、完成状态。' },
  { index: '04', title: '题库模块', description: '题目新增、查询、编辑、删除。' },
]

const isLoggedIn = computed(() => Boolean(currentUser.value))
const userInitial = computed(() => currentUser.value?.username?.slice(0, 1)?.toUpperCase() || 'T')

onMounted(() => {
  if (getAuthToken()) {
    loadCurrentUser()
  }
})

function switchAuthMode(mode) {
  authMode.value = mode
  clearMessage()
}

async function submitAuth() {
  clearMessage()
  if (!authForm.username || !authForm.password) {
    setMessage('error', '请填写用户名和密码')
    return
  }

  loading.value = true
  try {
    if (authMode.value === 'register') {
      await registerUser({
        username: authForm.username,
        password: authForm.password,
        email: authForm.email || null,
      })
      setMessage('success', '注册成功，请登录')
      authMode.value = 'login'
      authForm.password = ''
      return
    }

    await loginUser({
      username: authForm.username,
      password: authForm.password,
    })
    await loadCurrentUser()
    setMessage('success', '登录成功')
  } catch (error) {
    setMessage('error', readErrorMessage(error, authMode.value === 'login' ? '登录失败' : '注册失败'))
  } finally {
    loading.value = false
  }
}

async function loadCurrentUser() {
  loading.value = true
  try {
    const response = await getCurrentUserProfile()
    currentUser.value = response.data.data
    syncProfileForm()
  } catch (error) {
    clearAuthToken()
    currentUser.value = null
    setMessage('error', readErrorMessage(error, '登录状态已失效'))
  } finally {
    loading.value = false
  }
}

async function submitProfile() {
  clearMessage()
  loading.value = true
  try {
    const response = await updateCurrentUserProfile({
      email: profileForm.email || null,
      avatar: profileForm.avatar || null,
    })
    currentUser.value = response.data.data
    syncProfileForm()
    setMessage('success', '资料已保存')
  } catch (error) {
    setMessage('error', readErrorMessage(error, '资料保存失败'))
  } finally {
    loading.value = false
  }
}

async function submitPassword() {
  clearMessage()
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    setMessage('error', '请填写旧密码和新密码')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    setMessage('error', '两次输入的新密码不一致')
    return
  }

  loading.value = true
  try {
    await changeCurrentUserPassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    setMessage('success', '密码修改成功')
  } catch (error) {
    setMessage('error', readErrorMessage(error, '密码修改失败'))
  } finally {
    loading.value = false
  }
}

function handleLogout() {
  clearAuthToken()
  currentUser.value = null
  activePanel.value = 'dashboard'
  setMessage('success', '已退出登录')
}

function syncProfileForm() {
  profileForm.email = currentUser.value?.email || ''
  profileForm.avatar = currentUser.value?.avatar || ''
}

function setMessage(type, text) {
  message.type = type
  message.text = text
}

function clearMessage() {
  message.type = ''
  message.text = ''
}

function readErrorMessage(error, fallback) {
  return error.response?.data?.message || fallback
}
</script>
