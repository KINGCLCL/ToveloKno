<template>
  <div class="qb-app" :class="{ 'is-sidebar-collapsed': sidebarCollapsed }">
    <aside class="qb-sidebar">
      <div class="qb-brand">
        <LineIcon name="book-open" />
        <strong>题库模块</strong>
      </div>

      <nav class="qb-navigation" aria-label="题库功能导航">
        <section v-for="group in navigationGroups" :key="group.label || 'home'" class="qb-nav-group">
          <p v-if="group.label" class="qb-nav-label">{{ group.label }}</p>
          <button
            v-for="item in group.items"
            :key="item.id"
            type="button"
            class="qb-nav-item"
            :class="{ active: activeNav === item.id }"
            :title="sidebarCollapsed ? item.label : undefined"
            @click="selectNav(item.id)"
          >
            <LineIcon :name="item.icon" />
            <span>{{ item.label }}</span>
          </button>
        </section>
      </nav>

      <button type="button" class="qb-collapse-button" @click="sidebarCollapsed = !sidebarCollapsed">
        <span aria-hidden="true">{{ sidebarCollapsed ? '»' : '«' }}</span>
        <span>{{ sidebarCollapsed ? '展开导航' : '收起导航' }}</span>
      </button>
    </aside>

    <div class="qb-page">
      <header class="qb-topbar">
        <button type="button" class="qb-mobile-menu" aria-label="切换导航" @click="sidebarCollapsed = !sidebarCollapsed">
          <LineIcon name="menu" />
        </button>
        <div class="qb-breadcrumb"><strong>首页</strong><span>/</span><span>{{ activeNavLabel }}</span></div>

        <label class="qb-global-search">
          <input v-model.trim="globalKeyword" type="search" placeholder="搜索题目、知识点、题库等..." />
          <LineIcon name="search" />
        </label>

        <button
          type="button"
          class="qb-top-action"
          :class="{ active: activeTopAction === 'message' }"
          @click="activeTopAction = 'message'"
        >
          <LineIcon name="bell" />
          <span>消息</span>
          <i class="qb-notice-dot" />
        </button>
        <button
          type="button"
          class="qb-user-button"
          :class="{ active: activeTopAction === 'user' }"
          @click="activeTopAction = 'user'"
        >
          <span class="qb-avatar">林</span>
          <span>林同学</span>
          <LineIcon name="chevron-down" />
        </button>
      </header>

      <main class="qb-content">
        <section class="qb-panel qb-overview-panel">
          <h1>我的题库概览</h1>
          <div class="qb-stat-grid">
            <article v-for="stat in overviewStats" :key="stat.label" class="qb-stat-card">
              <div>
                <p>{{ stat.label }}</p>
                <strong>{{ stat.value }}<small>{{ stat.unit }}</small></strong>
                <span v-if="stat.note">{{ stat.note }}</span>
              </div>
              <LineIcon :name="stat.icon" />
            </article>
          </div>
        </section>

        <section class="qb-panel qb-quick-panel">
          <h2>快捷入口</h2>
          <div class="qb-quick-grid">
            <button
              v-for="action in quickActions"
              :key="action.id"
              type="button"
              :class="{ active: activeQuickAction === action.id }"
              @click="activeQuickAction = action.id"
            >
              <LineIcon :name="action.icon" />
              <span>{{ action.label }}</span>
            </button>
          </div>
        </section>

        <div class="qb-dashboard-grid">
          <section class="qb-panel qb-question-panel">
            <h2>题目列表</h2>
            <div class="qb-filter-row">
              <select v-model="filters.subject" aria-label="筛选科目">
                <option value="">全部科目</option>
                <option>数据库</option>
                <option>操作系统</option>
                <option>数据结构</option>
                <option>数学</option>
              </select>
              <select v-model="filters.knowledge" aria-label="筛选知识点">
                <option value="">全部知识点</option>
                <option>事务管理</option>
                <option>SQL基础</option>
                <option>进程与线程</option>
                <option>排序算法</option>
              </select>
              <select v-model="filters.type" aria-label="筛选题型">
                <option value="">全部题型</option>
                <option>选择题</option>
                <option>简答题</option>
                <option>填空题</option>
              </select>
              <select v-model="filters.difficulty" aria-label="筛选难度">
                <option value="">全部难度</option>
                <option value="2">二星</option>
                <option value="3">三星</option>
                <option value="4">四星</option>
              </select>
              <select v-model="filters.status" aria-label="筛选状态">
                <option value="">全部状态</option>
                <option>已发布</option>
                <option>草稿</option>
              </select>
              <label class="qb-table-search">
                <input v-model.trim="tableKeyword" type="search" placeholder="搜索题目内容或关键词" />
                <LineIcon name="search" />
              </label>
              <button type="button" class="qb-button primary" :class="{ active: searchPressed }" @click="searchPressed = !searchPressed">
                搜索
              </button>
              <button
                type="button"
                class="qb-button"
                :class="{ active: advancedSelected }"
                @click="advancedSelected = !advancedSelected"
              >
                高级筛选
              </button>
            </div>

            <div v-if="advancedSelected" class="qb-advanced-hint">已开启高级筛选模式，可继续扩展时间、标签等条件。</div>

            <div class="qb-table-scroll">
              <table class="qb-question-table">
                <thead>
                  <tr>
                    <th><input type="checkbox" :checked="allVisibleSelected" aria-label="选择全部题目" @change="toggleAllVisible" /></th>
                    <th>题目内容</th>
                    <th>科目</th>
                    <th>知识点</th>
                    <th>题型</th>
                    <th>难度</th>
                    <th>状态</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="question in filteredQuestions" :key="question.id" :class="{ selected: selectedQuestionIds.includes(question.id) }">
                    <td>
                      <input
                        v-model="selectedQuestionIds"
                        type="checkbox"
                        :value="question.id"
                        :aria-label="`选择题目：${question.content}`"
                      />
                    </td>
                    <td>
                      <div class="qb-question-title">
                        <span class="qb-question-thumb" aria-hidden="true">
                          <LineIcon name="file-text" />
                        </span>
                        <p>{{ question.content }}<small>查看题目详情与答案解析</small></p>
                      </div>
                    </td>
                    <td>{{ question.subject }}</td>
                    <td>{{ question.knowledge }}</td>
                    <td>{{ question.type }}</td>
                    <td><span class="qb-stars">{{ difficultyStars(question.difficulty) }}</span></td>
                    <td><span class="qb-status" :class="{ draft: question.status === '草稿' }">{{ question.status }}</span></td>
                    <td>
                      <div class="qb-row-actions">
                        <button type="button" title="编辑" :class="{ active: activeRowAction === `edit-${question.id}` }" @click="activeRowAction = `edit-${question.id}`"><LineIcon name="edit" /></button>
                        <button type="button" title="复制" :class="{ active: activeRowAction === `copy-${question.id}` }" @click="activeRowAction = `copy-${question.id}`"><LineIcon name="copy" /></button>
                        <button type="button" title="删除" :class="{ active: activeRowAction === `delete-${question.id}` }" @click="activeRowAction = `delete-${question.id}`"><LineIcon name="trash" /></button>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="filteredQuestions.length === 0">
                    <td colspan="8" class="qb-empty">没有找到符合条件的题目</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <footer class="qb-table-footer">
              <span>共 {{ filteredQuestions.length || 0 }} 条（示例数据）</span>
              <div class="qb-pagination" aria-label="分页">
                <button type="button" :disabled="currentPage === 1" @click="currentPage--">‹</button>
                <button
                  v-for="page in [1, 2, 3, 4, 5]"
                  :key="page"
                  type="button"
                  :class="{ active: currentPage === page }"
                  @click="currentPage = page"
                >
                  {{ page }}
                </button>
                <span>…</span>
                <button type="button" :class="{ active: currentPage === 86 }" @click="currentPage = 86">86</button>
              </div>
              <select v-model="pageSize" aria-label="每页数量">
                <option :value="10">10 条/页</option>
                <option :value="20">20 条/页</option>
                <option :value="50">50 条/页</option>
              </select>
            </footer>
          </section>

          <aside class="qb-side-stack">
            <section class="qb-panel qb-practice-panel">
              <h2>练习模式</h2>
              <div class="qb-practice-grid">
                <button
                  v-for="mode in practiceModes"
                  :key="mode.id"
                  type="button"
                  :class="{ active: activePracticeMode === mode.id }"
                  @click="activePracticeMode = mode.id"
                >
                  <LineIcon :name="mode.icon" />
                  <span><strong>{{ mode.label }}</strong><small>{{ mode.description }}</small></span>
                </button>
              </div>
            </section>

            <section class="qb-panel qb-record-panel">
              <header><h2>最近练习记录</h2><button type="button" @click="activeSideLink = 'records'">更多 <LineIcon name="chevron-right" /></button></header>
              <button
                v-for="record in recentRecords"
                :key="record.title"
                type="button"
                class="qb-record"
                :class="{ active: selectedRecord === record.title }"
                @click="selectedRecord = record.title"
              >
                <span class="qb-record-dot" />
                <span><strong>{{ record.title }}<small>{{ record.type }}</small></strong><small>{{ record.progress }}　正确率：{{ record.accuracy }}</small></span>
                <time>{{ record.date }}</time>
                <LineIcon name="chevron-right" />
              </button>
            </section>

            <section class="qb-panel qb-weak-panel">
              <header><h2>薄弱知识点（Top 5）</h2><button type="button" @click="activeSideLink = 'weak'">更多 <LineIcon name="chevron-right" /></button></header>
              <button
                v-for="point in weakPoints"
                :key="point.name"
                type="button"
                :class="{ active: selectedWeakPoint === point.name }"
                @click="selectedWeakPoint = point.name"
              >
                <span>{{ point.name }}</span>
                <i><b :style="{ width: `${point.accuracy}%` }" /></i>
                <small>正确率 {{ point.accuracy }}%</small>
              </button>
            </section>
          </aside>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import LineIcon from '../components/LineIcon.vue'
import {
  navigationGroups,
  overviewStats,
  practiceModes,
  questions,
  quickActions,
  recentRecords,
  weakPoints,
} from '../data/questionBankMock'

const activeNav = ref('overview')
const sidebarCollapsed = ref(false)
const activeQuickAction = ref('')
const activePracticeMode = ref('')
const activeTopAction = ref('')
const activeRowAction = ref('')
const activeSideLink = ref('')
const selectedRecord = ref('')
const selectedWeakPoint = ref('')
const selectedQuestionIds = ref([])
const globalKeyword = ref('')
const tableKeyword = ref('')
const advancedSelected = ref(false)
const searchPressed = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)

const filters = reactive({
  subject: '',
  knowledge: '',
  type: '',
  difficulty: '',
  status: '',
})

const allNavigationItems = navigationGroups.flatMap((group) => group.items)
const activeNavLabel = computed(
  () => allNavigationItems.find((item) => item.id === activeNav.value)?.label ?? '题库概览',
)

const filteredQuestions = computed(() => {
  const keyword = (tableKeyword.value || globalKeyword.value).toLowerCase()
  return questions.filter((question) => {
    const searchable = `${question.content}${question.subject}${question.knowledge}${question.type}`.toLowerCase()
    return (
      (!filters.subject || question.subject === filters.subject) &&
      (!filters.knowledge || question.knowledge === filters.knowledge) &&
      (!filters.type || question.type === filters.type) &&
      (!filters.difficulty || question.difficulty === Number(filters.difficulty)) &&
      (!filters.status || question.status === filters.status) &&
      (!keyword || searchable.includes(keyword))
    )
  })
})

const allVisibleSelected = computed(
  () =>
    filteredQuestions.value.length > 0 &&
    filteredQuestions.value.every((question) => selectedQuestionIds.value.includes(question.id)),
)

function selectNav(id) {
  activeNav.value = id
}

function difficultyStars(level) {
  return `${'★'.repeat(level)}${'☆'.repeat(5 - level)}`
}

function toggleAllVisible(event) {
  const visibleIds = filteredQuestions.value.map((question) => question.id)
  if (event.target.checked) {
    selectedQuestionIds.value = [...new Set([...selectedQuestionIds.value, ...visibleIds])]
    return
  }
  selectedQuestionIds.value = selectedQuestionIds.value.filter((id) => !visibleIds.includes(id))
}
</script>
