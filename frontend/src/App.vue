<template>
  <main class="app-shell">
    <section v-if="!isLoggedIn" class="login-scene">
      <div class="login-book">
        <section class="login-art" aria-label="ToveloKno">
          <div class="brand-mark">
            <span class="hexagon"></span>
            <strong>ToveloKno</strong>
          </div>
        </section>

        <section class="login-panel">
          <form class="auth-form" @submit.prevent="submitAuth">
            <label>
              <span>用户名</span>
              <div class="soft-input">
                <span>U</span>
                <input v-model.trim="authForm.username" type="text" autocomplete="username" placeholder="请输入用户名" />
              </div>
            </label>
            <label>
              <span>密码</span>
              <div class="soft-input">
                <span>L</span>
                <input v-model="authForm.password" type="password" autocomplete="current-password" placeholder="请输入密码" />
              </div>
            </label>
            <label>
              <span>邮箱</span>
              <div class="soft-input">
                <span>M</span>
                <input v-model.trim="authForm.email" type="email" autocomplete="email" placeholder="登录可不填邮箱" />
              </div>
            </label>
            <p v-if="message.text" class="message-line" :class="message.type">{{ message.text }}</p>
            <button class="login-submit" type="submit" :disabled="loading">
              <span>{{ loading ? '处理中...' : '登 录' }}</span>
              <b>-></b>
            </button>
          </form>
        </section>
      </div>
    </section>

    <section v-else class="workspace">
      <aside class="sidebar">
        <div class="side-brand">
          <span class="hexagon"></span>
          <strong>ToveloKno</strong>
        </div>

        <nav class="module-nav">
          <button
            v-for="(item, index) in navItems"
            :key="item.id"
            type="button"
            :class="{ active: activePanel === item.id }"
            @click="activePanel = item.id"
          >
            <span class="nav-icon">{{ item.icon }}</span>
            <span>{{ item.label }}</span>
            <em>{{ String(index + 1).padStart(2, '0') }}</em>
          </button>
        </nav>

        <button class="logout-button" type="button" @click="handleLogout">退出登录</button>
      </aside>

      <section class="content-frame">
        <section v-if="activePanel === 'home'" class="module-board ark-board home-board">
          <div class="workspace-hero">
            <p>ARK-LIGHT STUDY DESK</p>
            <h2>{{ profile.nickname }}，今天把知识排成战术队列。</h2>
          </div>
          <div class="ops-grid">
            <article
              v-for="item in homeCards"
              :key="item.id"
              class="ops-card"
              :class="{ selected: activePanel === item.id }"
              @click="activePanel = item.id"
            >
              <span>{{ item.code }}</span>
              <h4>{{ item.title }}</h4>
              <p>{{ item.text }}</p>
            </article>
          </div>
          <div class="timeline">
            <article v-for="task in todayFocus" :key="task.title" :class="{ done: task.done }">
              <time>{{ task.time }}</time>
              <div>
                <h4>{{ task.title }}</h4>
                <p>{{ task.desc }}</p>
              </div>
            </article>
          </div>
        </section>

        <section v-if="activePanel === 'resources'" class="module-board ark-board">
          <div class="board-toolbar">
            <div>
              <span class="ark-kicker">RESOURCE / INDEX</span>
              <h3>学习资料</h3>
              <div class="tabs">
                <button
                  v-for="tab in resourceTabs"
                  :key="tab"
                  type="button"
                  :class="{ active: activeResourceTab === tab }"
                  @click="activeResourceTab = tab"
                >
                  {{ tab }}
                </button>
              </div>
            </div>
            <div class="tool-actions">
              <label class="search-box">
                <input v-model.trim="resourceKeyword" type="search" placeholder="搜索资源" />
                <span>Q</span>
              </label>
              <button type="button" @click="resourceView = resourceView === 'table' ? 'grid' : 'table'">
                {{ resourceView === 'table' ? '网格' : '列表' }}
              </button>
              <button type="button" @click="showResourceForm = !showResourceForm">新增</button>
            </div>
          </div>

          <form v-if="showResourceForm" class="ark-form inline-form" @submit.prevent="addResource">
            <input v-model.trim="resourceDraft.name" placeholder="资料名称" />
            <select v-model="resourceDraft.type">
              <option v-for="type in resourceTabs.slice(1)" :key="type">{{ type }}</option>
            </select>
            <input v-model.trim="resourceDraft.size" placeholder="大小，如 2.4 MB" />
            <button class="primary-button" type="submit">加入资料库</button>
          </form>

          <div v-if="resourceView === 'table'" class="resource-table">
            <div class="table-head">
              <span>名称</span>
              <span>类型</span>
              <span>来源</span>
              <span>修改时间</span>
              <span>大小</span>
              <span>操作</span>
            </div>
            <div
              v-for="resource in filteredResources"
              :key="resource.id"
              class="table-row"
              :class="{ selected: selectedResourceId === resource.id }"
              @click="selectResource(resource)"
            >
              <span><i :class="['file-icon', resource.tone]">{{ resource.icon }}</i>{{ resource.name }}</span>
              <span>{{ resource.type }}</span>
              <span>{{ resource.source }}</span>
              <span>{{ resource.time }}</span>
              <span>{{ resource.size }}</span>
              <span class="row-actions">
                <button type="button" @click.stop="toggleFavorite(resource)">{{ resource.favorite ? '已标' : '星标' }}</button>
                <button type="button" @click.stop="removeResource(resource.id)">删除</button>
              </span>
            </div>
          </div>

          <div v-else class="resource-grid">
            <article
              v-for="resource in filteredResources"
              :key="resource.id"
              class="ops-card"
              :class="{ selected: selectedResourceId === resource.id }"
              @click="selectResource(resource)"
            >
              <span>{{ resource.type }}</span>
              <h4>{{ resource.name }}</h4>
              <p>{{ resource.description }}</p>
              <div class="card-actions">
                <button type="button" @click.stop="toggleFavorite(resource)">{{ resource.favorite ? '取消星标' : '星标' }}</button>
                <button type="button" @click.stop="removeResource(resource.id)">删除</button>
              </div>
            </article>
          </div>
          <p class="table-count">共 {{ filteredResources.length }} 项</p>
        </section>

        <section v-if="activePanel === 'cards'" class="module-board ark-board">
          <div class="simple-head">
            <div>
              <span class="ark-kicker">MEMORY / CARD</span>
              <h3>知识卡片</h3>
            </div>
            <button type="button" class="outline-button" @click="showCardForm = !showCardForm">新建卡片</button>
          </div>

          <form v-if="showCardForm" class="ark-form card-editor" @submit.prevent="addCard">
            <input v-model.trim="cardDraft.title" placeholder="卡片标题" />
            <input v-model.trim="cardDraft.tag" placeholder="标签" />
            <textarea v-model.trim="cardDraft.content" placeholder="写下定义、公式或易错点"></textarea>
            <button class="primary-button" type="submit">保存卡片</button>
          </form>

          <div class="card-grid">
            <article v-for="card in cards" :key="card.id" class="study-card">
              <span>{{ card.tag }}</span>
              <h4>{{ card.title }}</h4>
              <p>{{ card.content }}</p>
              <div class="card-actions">
                <button type="button" @click="reviewCard(card)">复习 +1</button>
                <button type="button" @click="removeCard(card.id)">删除</button>
              </div>
              <small>已复习 {{ card.reviewCount }} 次</small>
            </article>
          </div>
        </section>

        <section v-if="activePanel === 'practice'" class="module-board ark-board">
          <div class="simple-head">
            <div>
              <span class="ark-kicker">DRILL / TRAINING</span>
              <h3>题目练习</h3>
            </div>
            <p class="score-chip">正确 {{ practiceScore.correct }} / {{ practiceScore.total }}</p>
          </div>

          <article class="quiz-panel">
            <span>第 {{ currentQuestionIndex + 1 }} 题</span>
            <h4>{{ currentQuestion.title }}</h4>
            <div class="answer-list">
              <button
                v-for="option in currentQuestion.options"
                :key="option"
                type="button"
                :class="answerClass(option)"
                @click="chooseAnswer(option)"
              >
                {{ option }}
              </button>
            </div>
            <p v-if="selectedAnswer" class="analysis-line">{{ currentQuestion.analysis }}</p>
            <div class="card-actions">
              <button type="button" @click="prevQuestion">上一题</button>
              <button type="button" @click="nextQuestion">下一题</button>
              <button type="button" @click="collectWrongQuestion">加入错题本</button>
            </div>
          </article>
        </section>

        <section v-if="activePanel === 'wrong'" class="module-board ark-board">
          <div class="simple-head">
            <div>
              <span class="ark-kicker">ERROR / REVIEW</span>
              <h3>错题本</h3>
            </div>
            <div class="tabs compact-tabs">
              <button type="button" :class="{ active: wrongFilter === '全部' }" @click="wrongFilter = '全部'">全部</button>
              <button type="button" :class="{ active: wrongFilter === '未掌握' }" @click="wrongFilter = '未掌握'">未掌握</button>
              <button type="button" :class="{ active: wrongFilter === '已掌握' }" @click="wrongFilter = '已掌握'">已掌握</button>
            </div>
          </div>

          <div class="wrong-list">
            <article v-for="item in filteredWrongQuestions" :key="item.id" :class="{ mastered: item.mastered }">
              <span>{{ item.subject }}</span>
              <h4>{{ item.title }}</h4>
              <p>{{ item.note }}</p>
              <div class="card-actions">
                <button type="button" @click="item.mastered = !item.mastered">{{ item.mastered ? '标为未掌握' : '已掌握' }}</button>
                <button type="button" @click="removeWrongQuestion(item.id)">删除</button>
              </div>
            </article>
          </div>
        </section>

        <section v-if="activePanel === 'plan'" class="module-board ark-board">
          <div class="simple-head">
            <div>
              <span class="ark-kicker">PLAN / ROUTE</span>
              <h3>学习计划</h3>
            </div>
            <strong class="score-chip">{{ planProgress }}%</strong>
          </div>

          <form class="ark-form inline-form" @submit.prevent="addPlan">
            <input v-model.trim="planDraft.title" placeholder="计划名称" />
            <input v-model.trim="planDraft.time" placeholder="时间，如 21:00" />
            <button class="primary-button" type="submit">添加计划</button>
          </form>

          <div class="timeline plan-list">
            <article v-for="item in plans" :key="item.id" :class="{ done: item.done }">
              <time>{{ item.time }}</time>
              <div>
                <h4>{{ item.title }}</h4>
                <p>{{ item.done ? '已完成，进入复盘区。' : '待执行，保持队列。' }}</p>
              </div>
              <div class="card-actions">
                <button type="button" @click="item.done = !item.done">{{ item.done ? '撤回' : '完成' }}</button>
                <button type="button" @click="removePlan(item.id)">删除</button>
              </div>
            </article>
          </div>
        </section>

        <section v-if="activePanel === 'stats'" class="module-board ark-board">
          <div class="simple-head">
            <div>
              <span class="ark-kicker">DATA / OVERVIEW</span>
              <h3>学习统计</h3>
            </div>
            <p class="score-chip">本地实时统计</p>
          </div>

          <div class="stats-grid">
            <article v-for="stat in statsCards" :key="stat.label">
              <span>{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
              <p>{{ stat.desc }}</p>
            </article>
          </div>
          <div class="chart-panel">
            <article v-for="bar in chartBars" :key="bar.label">
              <span>{{ bar.label }}</span>
              <div><b :style="{ width: bar.value + '%' }"></b></div>
              <em>{{ bar.value }}%</em>
            </article>
          </div>
        </section>

        <section v-if="activePanel === 'profile'" class="module-board ark-board">
          <div class="simple-head">
            <div>
              <span class="ark-kicker">PROFILE / HOME</span>
              <h3>个人主页</h3>
            </div>
            <button type="button" class="outline-button" @click="loadProfile">同步资料</button>
          </div>

          <div class="profile-home">
            <article class="avatar-card">
              <span class="avatar-ring">{{ profile.nickname.slice(0, 1).toUpperCase() }}</span>
              <h4>{{ profile.nickname }}</h4>
              <p>{{ profile.bio }}</p>
              <div class="profile-rank">
                <b>LV.{{ level }}</b>
                <small>{{ profile.email || '未绑定邮箱' }}</small>
              </div>
            </article>
            <article v-for="item in profileModules" :key="item.title" @click="activePanel = item.target">
              <span>{{ item.code }}</span>
              <h4>{{ item.title }}</h4>
              <p>{{ item.text }}</p>
            </article>
          </div>
        </section>

        <section v-if="activePanel === 'settings'" class="module-board ark-board">
          <div class="simple-head">
            <div>
              <span class="ark-kicker">SYSTEM / SETTINGS</span>
              <h3>设置</h3>
            </div>
            <p v-if="profileMessage" class="message-line success">{{ profileMessage }}</p>
          </div>

          <div class="settings-grid">
            <form class="ark-form settings-form" @submit.prevent="saveProfile">
              <h4>个人资料</h4>
              <input v-model.trim="profileForm.nickname" placeholder="昵称" />
              <input v-model.trim="profileForm.email" type="email" placeholder="邮箱" />
              <textarea v-model.trim="profileForm.bio" placeholder="个人简介"></textarea>
              <button class="primary-button" type="submit">保存资料</button>
            </form>

            <form class="ark-form settings-form" @submit.prevent="savePassword">
              <h4>修改密码</h4>
              <input v-model="passwordForm.oldPassword" type="password" placeholder="旧密码" />
              <input v-model="passwordForm.newPassword" type="password" placeholder="新密码" />
              <input v-model="passwordForm.confirmPassword" type="password" placeholder="确认新密码" />
              <button class="primary-button" type="submit">更新密码</button>
            </form>
          </div>
        </section>
      </section>

      <aside class="detail-panel">
        <div class="detail-tabs">
          <button type="button">详情</button>
          <button type="button">标签</button>
        </div>

        <section class="selected-file">
          <i :class="['file-icon', selectedResource.tone]">{{ selectedResource.icon }}</i>
          <div>
            <h4>{{ selectedResource.name }}</h4>
            <p>{{ selectedResource.type }} - {{ selectedResource.size }}</p>
          </div>
          <button type="button" @click="toggleFavorite(selectedResource)">{{ selectedResource.favorite ? '★' : '☆' }}</button>
        </section>

        <dl class="file-meta">
          <dt>位置</dt>
          <dd>{{ selectedResource.path }}</dd>
          <dt>来源</dt>
          <dd>{{ selectedResource.source }}</dd>
          <dt>修改时间</dt>
          <dd>{{ selectedResource.time }}</dd>
          <dt>描述</dt>
          <dd>{{ selectedResource.description }}</dd>
        </dl>

        <section class="mini-stat">
          <header>
            <strong>统计</strong>
            <button type="button" @click="activePanel = 'stats'">更多</button>
          </header>
          <div>
            <span><b>{{ resources.length }}</b>文件数</span>
            <span><b>{{ cards.length }}</b>卡片</span>
            <span><b>{{ wrongQuestions.length }}</b>错题</span>
            <span><b>{{ planProgress }}%</b>进度</span>
          </div>
        </section>

        <section class="recent-list">
          <header>
            <strong>最近学习</strong>
            <button type="button" @click="activePanel = 'resources'">更多</button>
          </header>
          <p v-for="item in recentStudy" :key="item.name">
            <span>{{ item.name }}</span>
            <em>{{ item.time }}</em>
          </p>
        </section>
      </aside>
    </section>
  </main>
</template>

<script>
import {
  changeCurrentUserPassword,
  clearAuthToken,
  getAuthToken,
  getCurrentUserProfile,
  loginUser,
  updateCurrentUserProfile,
} from './api'

const nowTime = () =>
  new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date())

const createId = (prefix) => `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`

export default {
  name: 'App',
  data() {
    return {
      loading: false,
      isLoggedIn: Boolean(getAuthToken()),
      message: { type: '', text: '' },
      authForm: {
        username: 'ADMIN',
        password: '',
        email: '',
      },
      activePanel: 'home',
      navItems: [
        { id: 'home', label: '个人主页', icon: 'HM' },
        { id: 'resources', label: '学习资料', icon: 'RS' },
        { id: 'cards', label: '知识卡片', icon: 'CD' },
        { id: 'practice', label: '题目练习', icon: 'TR' },
        { id: 'wrong', label: '错题本', icon: 'ER' },
        { id: 'plan', label: '学习计划', icon: 'PL' },
        { id: 'stats', label: '统计', icon: 'DT' },
        { id: 'settings', label: '设置', icon: 'ST' },
      ],
      resourceTabs: ['全部', '文档', '图片', '视频', '音频', '其他'],
      activeResourceTab: '全部',
      resourceView: 'table',
      resourceKeyword: '',
      showResourceForm: false,
      selectedResourceId: 1,
      resourceDraft: { name: '', type: '文档', size: '' },
      resources: [
        {
          id: 1,
          name: '高等数学（上）笔记',
          type: '文档',
          source: '本地',
          time: '2024-05-20 21:30',
          size: '12 项',
          path: '/我的资源/高等数学（上）笔记',
          description: '课程上课学习资料整理，包含极限、连续与导数。',
          icon: 'W',
          tone: 'blue',
          favorite: true,
        },
        {
          id: 2,
          name: '第三章 极限与连续.docx',
          type: '文档',
          source: '本地',
          time: '2024-05-20 20:15',
          size: '2.4 MB',
          path: '/我的资源/第三章 极限与连续.docx',
          description: '极限计算、夹逼准则与连续性判断。',
          icon: 'W',
          tone: 'blue',
          favorite: false,
        },
        {
          id: 3,
          name: '典型公式汇总.pdf',
          type: '文档',
          source: '本地',
          time: '2024-05-19 18:40',
          size: '1.8 MB',
          path: '/我的资源/典型公式汇总.pdf',
          description: '高频公式与错题复盘入口。',
          icon: 'P',
          tone: 'red',
          favorite: false,
        },
        {
          id: 4,
          name: '极限的定义与性质.mp4',
          type: '视频',
          source: '网盘',
          time: '2024-05-18 16:20',
          size: '128 MB',
          path: '/我的资源/极限的定义与性质.mp4',
          description: '课堂录屏，适合二轮复习。',
          icon: 'V',
          tone: 'violet',
          favorite: true,
        },
        {
          id: 5,
          name: '重要公式思维导图.png',
          type: '图片',
          source: '本地',
          time: '2024-05-17 14:10',
          size: '2.1 MB',
          path: '/我的资源/重要公式思维导图.png',
          description: '把分散知识点按章节聚合。',
          icon: 'I',
          tone: 'blue',
          favorite: false,
        },
      ],
      showCardForm: false,
      cardDraft: { title: '', tag: '', content: '' },
      cards: [
        { id: 1, title: '夹逼准则', tag: '极限', content: '当两侧函数趋向同一极限时，中间函数也趋向该极限。', reviewCount: 2 },
        { id: 2, title: '连续判定', tag: '函数', content: '函数值存在、极限存在且二者相等。', reviewCount: 1 },
        { id: 3, title: '导数定义', tag: '微分', content: '本质是函数在一点附近的平均变化率极限。', reviewCount: 0 },
      ],
      currentQuestionIndex: 0,
      selectedAnswer: '',
      answers: {},
      questions: [
        {
          id: 1,
          title: '若 lim f(x)=A 且 lim g(x)=A，h(x) 被二者夹住，则 h(x) 的极限是？',
          options: ['A', '0', '不存在', '无法判断'],
          answer: 'A',
          analysis: '夹逼准则要求上下界趋于同一值，中间函数也趋于同一值。',
        },
        {
          id: 2,
          title: '函数在 x0 连续，以下哪项必须成立？',
          options: ['函数值等于极限', '导数存在', '单调递增', '二阶导存在'],
          answer: '函数值等于极限',
          analysis: '连续的核心条件是函数值存在、极限存在且相等。',
        },
      ],
      wrongFilter: '全部',
      wrongQuestions: [
        { id: 1, subject: '高数', title: '无穷小比较误判', note: '需要先同阶替换，再判断主项。', mastered: false },
        { id: 2, subject: '英语', title: '定语从句关系词', note: '先判断先行词，再看从句缺少成分。', mastered: true },
      ],
      planDraft: { title: '', time: '' },
      plans: [
        { id: 1, title: '整理第三章公式', time: '09:30', done: true },
        { id: 2, title: '完成 20 道极限题', time: '15:00', done: false },
        { id: 3, title: '复盘错题本', time: '21:00', done: false },
      ],
      profile: {
        nickname: 'ToveloKno',
        email: '',
        bio: '把资料、卡片、练习与复盘串成自己的学习控制台。',
      },
      profileForm: {
        nickname: 'ToveloKno',
        email: '',
        bio: '把资料、卡片、练习与复盘串成自己的学习控制台。',
      },
      passwordForm: { oldPassword: '', newPassword: '', confirmPassword: '' },
      profileMessage: '',
    }
  },
  computed: {
    filteredResources() {
      const keyword = this.resourceKeyword.toLowerCase()
      return this.resources.filter((item) => {
        const matchTab = this.activeResourceTab === '全部' || item.type === this.activeResourceTab
        const matchKeyword = !keyword || `${item.name}${item.type}${item.description}`.toLowerCase().includes(keyword)
        return matchTab && matchKeyword
      })
    },
    selectedResource() {
      return this.resources.find((item) => item.id === this.selectedResourceId) || this.resources[0]
    },
    currentQuestion() {
      return this.questions[this.currentQuestionIndex]
    },
    practiceScore() {
      const checked = Object.entries(this.answers)
      const correct = checked.filter(([id, answer]) => {
        const question = this.questions.find((item) => String(item.id) === id)
        return question?.answer === answer
      }).length
      return { correct, total: checked.length }
    },
    filteredWrongQuestions() {
      if (this.wrongFilter === '已掌握') return this.wrongQuestions.filter((item) => item.mastered)
      if (this.wrongFilter === '未掌握') return this.wrongQuestions.filter((item) => !item.mastered)
      return this.wrongQuestions
    },
    planProgress() {
      if (!this.plans.length) return 0
      return Math.round((this.plans.filter((item) => item.done).length / this.plans.length) * 100)
    },
    statsCards() {
      return [
        { label: '文件数', value: this.resources.length, desc: '资料库当前条目' },
        { label: '卡片', value: this.cards.length, desc: '可复习知识卡' },
        { label: '错题', value: this.wrongQuestions.length, desc: '需要持续压低' },
        { label: '计划完成', value: `${this.planProgress}%`, desc: '今日执行进度' },
      ]
    },
    chartBars() {
      return [
        { label: '资料整理', value: Math.min(100, this.resources.length * 14) },
        { label: '卡片复习', value: Math.min(100, this.cards.reduce((sum, item) => sum + item.reviewCount, 0) * 12) },
        { label: '练习正确', value: this.practiceScore.total ? Math.round((this.practiceScore.correct / this.practiceScore.total) * 100) : 0 },
        { label: '计划推进', value: this.planProgress },
      ]
    },
    homeCards() {
      return [
        { id: 'resources', code: '01', title: '资源中枢', text: '整理文件、星标重点、快速定位最近学习。' },
        { id: 'cards', code: '02', title: '记忆卡组', text: '把散乱概念压缩成可复习的卡片。' },
        { id: 'practice', code: '03', title: '训练队列', text: '即时答题、判断正确性、沉淀错题。' },
        { id: 'plan', code: '04', title: '今日路线', text: '用简洁计划把学习节奏固定下来。' },
      ]
    },
    todayFocus() {
      return this.plans.slice(0, 3).map((item) => ({
        time: item.time,
        title: item.title,
        desc: item.done ? '已完成' : '等待执行',
        done: item.done,
      }))
    },
    recentStudy() {
      return this.resources.slice(0, 3).map((item, index) => ({
        name: item.name,
        time: index === 0 ? '刚刚' : `${index * 20} 分钟前`,
      }))
    },
    profileModules() {
      return [
        { code: 'RS', title: '资料库', text: `${this.resources.length} 个资源正在归档`, target: 'resources' },
        { code: 'CD', title: '知识卡片', text: `${this.cards.length} 张卡片可复习`, target: 'cards' },
        { code: 'ER', title: '错题压制', text: `${this.filteredWrongQuestions.length} 条记录可处理`, target: 'wrong' },
        { code: 'ST', title: '账户设置', text: '修改资料与密码', target: 'settings' },
      ]
    },
    level() {
      return Math.max(1, Math.ceil((this.cards.length + this.resources.length + this.plans.filter((item) => item.done).length) / 3))
    },
  },
  mounted() {
    if (this.isLoggedIn) {
      this.loadProfile()
    }
  },
  methods: {
    async submitAuth() {
      if (!this.authForm.username || !this.authForm.password) {
        this.message = { type: 'error', text: '请输入用户名和密码' }
        return
      }
      this.loading = true
      this.message = { type: '', text: '' }
      try {
        await loginUser({
          username: this.authForm.username,
          password: this.authForm.password,
          email: this.authForm.email || undefined,
        })
        this.isLoggedIn = true
        this.message = { type: 'success', text: '' }
        await this.loadProfile()
      } catch (error) {
        this.message = { type: 'error', text: error.response?.data?.message || '登录失败' }
      } finally {
        this.loading = false
      }
    },
    handleLogout() {
      clearAuthToken()
      this.isLoggedIn = false
      this.activePanel = 'home'
    },
    async loadProfile() {
      try {
        const response = await getCurrentUserProfile()
        const data = response.data?.data || response.data || {}
        this.profile = {
          nickname: data.nickname || data.username || this.profile.nickname,
          email: data.email || this.profile.email,
          bio: data.bio || data.description || this.profile.bio,
        }
        this.profileForm = { ...this.profile }
      } catch {
        this.profileForm = { ...this.profile }
      }
    },
    selectResource(resource) {
      this.selectedResourceId = resource.id
    },
    addResource() {
      if (!this.resourceDraft.name) return
      const resource = {
        id: createId('resource'),
        name: this.resourceDraft.name,
        type: this.resourceDraft.type,
        source: '本地',
        time: nowTime(),
        size: this.resourceDraft.size || '未标注',
        path: `/我的资源/${this.resourceDraft.name}`,
        description: '新加入的学习资料，可继续补充描述与标签。',
        icon: this.resourceDraft.type.slice(0, 1).toUpperCase(),
        tone: this.resourceDraft.type === '文档' ? 'blue' : this.resourceDraft.type === '视频' ? 'violet' : 'red',
        favorite: false,
      }
      this.resources.unshift(resource)
      this.selectedResourceId = resource.id
      this.resourceDraft = { name: '', type: '文档', size: '' }
      this.showResourceForm = false
    },
    removeResource(id) {
      this.resources = this.resources.filter((item) => item.id !== id)
      if (this.selectedResourceId === id && this.resources.length) {
        this.selectedResourceId = this.resources[0].id
      }
    },
    toggleFavorite(resource) {
      const target = this.resources.find((item) => item.id === resource.id)
      if (target) target.favorite = !target.favorite
    },
    addCard() {
      if (!this.cardDraft.title || !this.cardDraft.content) return
      this.cards.unshift({
        id: createId('card'),
        title: this.cardDraft.title,
        tag: this.cardDraft.tag || '未分类',
        content: this.cardDraft.content,
        reviewCount: 0,
      })
      this.cardDraft = { title: '', tag: '', content: '' }
      this.showCardForm = false
    },
    reviewCard(card) {
      card.reviewCount += 1
    },
    removeCard(id) {
      this.cards = this.cards.filter((item) => item.id !== id)
    },
    chooseAnswer(option) {
      this.selectedAnswer = option
      this.answers = { ...this.answers, [this.currentQuestion.id]: option }
    },
    answerClass(option) {
      if (!this.selectedAnswer) return ''
      if (option === this.currentQuestion.answer) return 'correct'
      if (option === this.selectedAnswer) return 'wrong'
      return ''
    },
    prevQuestion() {
      this.currentQuestionIndex = (this.currentQuestionIndex + this.questions.length - 1) % this.questions.length
      this.selectedAnswer = this.answers[this.currentQuestion.id] || ''
    },
    nextQuestion() {
      this.currentQuestionIndex = (this.currentQuestionIndex + 1) % this.questions.length
      this.selectedAnswer = this.answers[this.currentQuestion.id] || ''
    },
    collectWrongQuestion() {
      if (this.wrongQuestions.some((item) => item.title === this.currentQuestion.title)) return
      this.wrongQuestions.unshift({
        id: createId('wrong'),
        subject: '练习',
        title: this.currentQuestion.title,
        note: this.currentQuestion.analysis,
        mastered: false,
      })
      this.activePanel = 'wrong'
    },
    removeWrongQuestion(id) {
      this.wrongQuestions = this.wrongQuestions.filter((item) => item.id !== id)
    },
    addPlan() {
      if (!this.planDraft.title) return
      this.plans.unshift({
        id: createId('plan'),
        title: this.planDraft.title,
        time: this.planDraft.time || nowTime(),
        done: false,
      })
      this.planDraft = { title: '', time: '' }
    },
    removePlan(id) {
      this.plans = this.plans.filter((item) => item.id !== id)
    },
    async saveProfile() {
      try {
        await updateCurrentUserProfile(this.profileForm)
      } catch {
        // 后端未启动或接口未完全实现时，仍保留前端本地预览。
      }
      this.profile = { ...this.profileForm }
      this.profileMessage = '资料已保存'
      setTimeout(() => {
        this.profileMessage = ''
      }, 1800)
    },
    async savePassword() {
      if (!this.passwordForm.newPassword || this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
        this.profileMessage = '两次新密码不一致'
        return
      }
      try {
        await changeCurrentUserPassword(this.passwordForm)
        this.profileMessage = '密码已更新'
      } catch {
        this.profileMessage = '密码已在本地校验，后端连接后可同步'
      }
      this.passwordForm = { oldPassword: '', newPassword: '', confirmPassword: '' }
    },
  },
}
</script>
