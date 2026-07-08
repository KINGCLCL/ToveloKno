<template>
  <div class="full-module wrong-workspace">
    <aside class="full-module-rail">
      <button class="full-back" type="button" @click="emit('back-home')">
        <span>‹</span>
        <strong>学习控制台</strong>
      </button>
      <section class="full-module-brand">
        <small>ERROR REVIEW</small>
        <h1>错题本</h1>
        <p>把练习中暴露的问题集中复盘，压低重复错误。</p>
      </section>
      <section class="full-stat-stack">
        <article><span>当前列表</span><strong>{{ wrongQuestions.length }}</strong></article>
        <article><span>未掌握</span><strong>{{ activeCount }}</strong></article>
        <article><span>平均难度</span><strong>{{ averageDifficulty }}</strong></article>
      </section>
      <section class="full-link-stack">
        <button type="button" @click="emit('open-module', 'practice')">进入题库练习</button>
        <button type="button" @click="emit('open-module', 'plan')">安排复盘计划</button>
      </section>
      <p v-if="message" class="wrong-message">{{ message }}</p>
    </aside>

    <main class="full-module-main">
      <header class="full-module-topbar">
        <div>
          <span class="full-kicker">TOVELOKNO / REVIEW OPS</span>
          <h2>错题复盘台</h2>
        </div>
        <button class="full-primary" type="button" @click="loadWrongQuestions">刷新列表</button>
      </header>

      <section class="full-filter-strip">
        <button type="button" :class="{ active: filter === '' }" @click="setFilter('')">全部</button>
        <button type="button" :class="{ active: filter === 'false' }" @click="setFilter('false')">未掌握</button>
        <button type="button" :class="{ active: filter === 'true' }" @click="setFilter('true')">已掌握</button>
      </section>

      <section v-if="loading" class="full-empty">错题加载中...</section>
      <section v-else-if="!wrongQuestions.length" class="full-empty">当前筛选下暂无错题。</section>

      <section v-else class="full-card-grid wrong-grid">
        <article v-for="item in wrongQuestions" :key="item.id" class="full-card wrong-card-full" :class="{ mastered: item.mastered }">
          <header>
            <span>{{ questionTypeLabel(item.questionType) }}</span>
            <strong>错误 {{ item.wrongCount }} 次</strong>
          </header>
          <h3>{{ item.content }}</h3>
          <section v-if="item.sourceResourceName || item.sourceExcerpt" class="wrong-source">
            <strong>来源：{{ item.sourceResourceName || '学习资料' }}{{ item.sourcePage ? ` · P${item.sourcePage}` : '' }}</strong>
            <p v-if="item.sourceExcerpt">{{ item.sourceExcerpt }}</p>
          </section>
          <dl>
            <dt>正确答案</dt>
            <dd>{{ item.correctAnswer }}</dd>
            <dt>解析</dt>
            <dd>{{ item.analysis || '暂无解析' }}</dd>
          </dl>
          <footer>
            <small>难度 {{ item.difficulty || '-' }} / 最近错误 {{ formatDateTime(item.lastWrongAt) }}</small>
            <div>
              <button v-if="!item.mastered" type="button" :disabled="planningId === item.id" @click="planWrongQuestion(item)">
                {{ planningId === item.id ? '安排中' : '加入计划' }}
              </button>
              <button v-if="!item.mastered" type="button" @click="markMastered(item.id)">已掌握</button>
              <button type="button" class="danger" @click="removeWrong(item.id)">移出</button>
            </div>
          </footer>
        </article>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  createWrongQuestionReviewPlan,
  deleteWrongQuestion,
  listWrongQuestions,
  markWrongQuestionMastered,
} from '../../api'

const emit = defineEmits(['back-home', 'open-module'])

const loading = ref(false)
const filter = ref('')
const wrongQuestions = ref([])
const planningId = ref(null)
const message = ref('')

const activeCount = computed(() => wrongQuestions.value.filter((item) => !item.mastered).length)
const averageDifficulty = computed(() => {
  const values = wrongQuestions.value.map((item) => Number(item.difficulty || 0)).filter(Boolean)
  if (!values.length) return '-'
  return (values.reduce((sum, value) => sum + value, 0) / values.length).toFixed(1)
})

onMounted(loadWrongQuestions)

async function loadWrongQuestions() {
  loading.value = true
  try {
    const params = filter.value === '' ? {} : { mastered: filter.value }
    const response = await listWrongQuestions(params)
    wrongQuestions.value = response.data?.data || []
  } finally {
    loading.value = false
  }
}

function setFilter(value) {
  filter.value = value
  loadWrongQuestions()
}

async function markMastered(id) {
  await markWrongQuestionMastered(id)
  await loadWrongQuestions()
}

async function removeWrong(id) {
  await deleteWrongQuestion(id)
  await loadWrongQuestions()
}

async function planWrongQuestion(item) {
  planningId.value = item.id
  message.value = ''
  try {
    await createWrongQuestionReviewPlan(item)
    message.value = '已生成错题复盘计划'
  } catch (error) {
    message.value = error.response?.data?.message || '计划生成失败'
  } finally {
    planningId.value = null
    window.setTimeout(() => {
      message.value = ''
    }, 2200)
  }
}

function questionTypeLabel(type) {
  return {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '简答题',
  }[type] || type || '题目'
}

function formatDateTime(value) {
  if (!value) return '暂无记录'
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.wrong-source {
  display: grid;
  gap: 7px;
  padding: 10px 12px;
  border: 1px solid rgba(60, 94, 210, 0.14);
  border-radius: 6px;
  background: rgba(236, 242, 255, 0.58);
}

.wrong-source strong {
  color: #244995;
  font-size: 13px;
}

.wrong-source p {
  max-height: 96px;
  overflow: auto;
  margin: 0;
  color: #65739b;
  font-size: 13px;
  line-height: 1.65;
}

.wrong-message {
  margin: 0;
  padding: 10px 12px;
  color: #214683;
  border: 1px solid rgba(60, 94, 210, 0.16);
  border-radius: 6px;
  background: rgba(236, 242, 255, 0.7);
  font-size: 13px;
  font-weight: 800;
}
</style>
