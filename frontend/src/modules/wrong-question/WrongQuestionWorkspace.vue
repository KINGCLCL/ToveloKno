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
        <button type="button" :disabled="reviewing || !reviewPool.length" @click="startWrongReview">
          {{ reviewing ? '复习中' : '开始错题复习' }}
        </button>
        <button type="button" @click="emit('open-module', 'practice')">进入题库做题</button>
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

      <section class="wrong-review-board">
        <article class="wrong-review-player">
          <header>
            <div>
              <span>WRONG QUESTION REVIEW</span>
              <h3>{{ currentReviewQuestion ? `第 ${reviewIndex + 1} / ${reviewQuestions.length} 题` : '错题复习台' }}</h3>
            </div>
            <button type="button" :disabled="!reviewPool.length || reviewing" @click="startWrongReview">重新组卷</button>
          </header>
          <div v-if="reviewQuestions.length" class="wrong-review-progress"><i><b :style="{ width: `${reviewProgress}%` }" /></i><span>{{ reviewProgress }}%</span></div>
          <form v-if="currentReviewQuestion" class="wrong-review-form" @submit.prevent="revealReviewAnswer">
            <div class="wrong-review-meta">
              <span>{{ questionTypeLabel(currentReviewQuestion.questionType) }}</span>
              <span>错 {{ currentReviewQuestion.wrongCount }} 次</span>
              <span>难度 {{ currentReviewQuestion.difficulty || '-' }}</span>
            </div>
            <h4>{{ questionText(currentReviewQuestion.content) }}</h4>
            <img v-if="questionImageUrl(currentReviewQuestion.content)" :src="assetUrl(questionImageUrl(currentReviewQuestion.content))" alt="题目图片" />
            <div v-if="currentReviewQuestion.options?.length" class="wrong-review-options">
              <button
                v-for="(option, index) in currentReviewQuestion.options"
                :key="`${currentReviewQuestion.id}-${index}`"
                type="button"
                :disabled="reviewAnswerRevealed"
                :class="{ selected: isReviewOptionSelected(index) }"
                @click="toggleReviewOption(index)"
              >
                <b>{{ String.fromCharCode(65 + index) }}</b><span>{{ option }}</span>
              </button>
            </div>
            <label>
              <span>{{ currentReviewQuestion.options?.length ? '作答备注' : '你的答案' }}</span>
              <textarea
                v-if="currentReviewQuestion.options?.length"
                v-model.trim="reviewNote"
                :disabled="reviewAnswerRevealed"
                rows="3"
                maxlength="1000"
                placeholder="可选：记录你这次为什么这样选"
              />
              <textarea
                v-else
                v-model.trim="reviewAnswer"
                :disabled="reviewAnswerRevealed"
                rows="4"
                maxlength="1000"
                placeholder="重新写一遍答案，也可以直接查看答案后自评"
              />
            </label>
            <section v-if="reviewAnswerRevealed" class="wrong-review-result" :class="{ correct: reviewResult?.correct }">
              <strong>{{ reviewResult ? (reviewResult.correct ? '已标记掌握' : '仍需复盘，已继续留在错题本') : '对照答案后自评' }}</strong>
              <p>你的作答：{{ reviewDisplayAnswer }}</p>
              <p>正确答案：{{ currentReviewQuestion.correctAnswer || '暂无答案' }}</p>
              <p>解析：{{ currentReviewQuestion.analysis || '暂无解析' }}</p>
            </section>
            <p v-if="reviewError" class="wrong-message">{{ reviewError }}</p>
            <footer>
              <button v-if="!reviewAnswerRevealed" class="full-primary" type="submit">查看答案</button>
              <template v-else-if="!reviewResult">
                <button class="wrong-judge-correct" type="button" :disabled="answerSubmitting" @click="judgeReviewAnswer(true)">我答对了</button>
                <button class="wrong-judge-wrong" type="button" :disabled="answerSubmitting" @click="judgeReviewAnswer(false)">我答错了</button>
              </template>
              <button v-else class="full-primary" type="button" @click="nextReviewQuestion">{{ reviewIndex + 1 >= reviewQuestions.length ? '完成复习' : '下一题' }}</button>
            </footer>
          </form>
          <section v-else class="wrong-review-empty">
            <strong>先从未掌握错题开始</strong>
            <span>做错会保留在错题库；答对后系统会把这道题标记为已掌握。</span>
          </section>
        </article>
      </section>

      <section v-if="loading" class="full-empty">错题加载中...</section>
      <section v-else-if="!wrongQuestions.length" class="full-empty">当前筛选下暂无错题。</section>

      <section v-else class="full-card-grid wrong-grid">
        <article v-for="item in wrongQuestions" :key="item.id" class="full-card wrong-card-full" :class="{ mastered: item.mastered }">
          <header>
            <span>{{ questionTypeLabel(item.questionType) }}</span>
            <strong>错误 {{ item.wrongCount }} 次</strong>
          </header>
          <h3>{{ questionText(item.content) }}</h3>
          <img v-if="questionImageUrl(item.content)" class="wrong-card-image" :src="assetUrl(questionImageUrl(item.content))" alt="题目图片" />
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
  resolveAssetUrl,
  submitQuestionBankPracticeAnswer,
} from '../../api'

const emit = defineEmits(['back-home', 'open-module'])

const loading = ref(false)
const filter = ref('')
const wrongQuestions = ref([])
const planningId = ref(null)
const message = ref('')
const reviewQuestions = ref([])
const reviewIndex = ref(0)
const reviewAnswer = ref('')
const reviewNote = ref('')
const reviewAnswerRevealed = ref(false)
const reviewResult = ref(null)
const reviewError = ref('')
const reviewing = ref(false)
const answerSubmitting = ref(false)

const activeCount = computed(() => wrongQuestions.value.filter((item) => !item.mastered).length)
const reviewPool = computed(() => wrongQuestions.value.filter((item) => !item.mastered))
const currentReviewQuestion = computed(() => reviewQuestions.value[reviewIndex.value] || null)
const reviewProgress = computed(() => {
  if (!reviewQuestions.value.length) return 0
  return Math.round(((reviewIndex.value + (reviewResult.value ? 1 : 0)) / reviewQuestions.value.length) * 100)
})
const reviewDisplayAnswer = computed(() => {
  if (reviewAnswer.value) return reviewAnswer.value
  if (reviewNote.value) return reviewNote.value
  return '未填写'
})
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

function startWrongReview() {
  reviewQuestions.value = reviewPool.value.slice(0, 20)
  reviewIndex.value = 0
  reviewAnswer.value = ''
  reviewNote.value = ''
  reviewAnswerRevealed.value = false
  reviewResult.value = null
  reviewError.value = ''
  reviewing.value = Boolean(reviewQuestions.value.length)
}

function reviewOptionValue(index) {
  if (currentReviewQuestion.value?.questionType === 'TRUE_FALSE') {
    return currentReviewQuestion.value.options[index]
  }
  return String.fromCharCode(65 + index)
}

function isReviewOptionSelected(index) {
  return reviewAnswer.value.split(',').includes(reviewOptionValue(index))
}

function toggleReviewOption(index) {
  if (reviewAnswerRevealed.value) return
  const value = reviewOptionValue(index)
  if (currentReviewQuestion.value.questionType !== 'MULTIPLE_CHOICE') {
    reviewAnswer.value = value
    return
  }
  const selected = new Set(reviewAnswer.value ? reviewAnswer.value.split(',') : [])
  if (selected.has(value)) selected.delete(value)
  else selected.add(value)
  reviewAnswer.value = [...selected].sort().join(',')
}

function revealReviewAnswer() {
  if (!currentReviewQuestion.value) return
  reviewAnswerRevealed.value = true
  reviewError.value = ''
}

async function judgeReviewAnswer(selfCorrect) {
  if (!currentReviewQuestion.value || !reviewAnswerRevealed.value || reviewResult.value) return
  answerSubmitting.value = true
  reviewError.value = ''
  try {
    const response = await submitQuestionBankPracticeAnswer({
      questionId: currentReviewQuestion.value.questionId,
      userAnswer: reviewDisplayAnswer.value,
      mode: 'wrong',
      selfCorrect,
    })
    reviewResult.value = response.data?.data
    await loadWrongQuestions()
  } catch (error) {
    reviewError.value = error.response?.data?.message || '记录复习结果失败'
  } finally {
    answerSubmitting.value = false
  }
}

function nextReviewQuestion() {
  if (reviewIndex.value + 1 >= reviewQuestions.value.length) {
    reviewing.value = false
    reviewQuestions.value = []
    reviewIndex.value = 0
    reviewAnswer.value = ''
    reviewNote.value = ''
    reviewAnswerRevealed.value = false
    reviewResult.value = null
    message.value = '本轮错题复习完成'
    window.setTimeout(() => {
      if (message.value === '本轮错题复习完成') message.value = ''
    }, 2200)
    return
  }
  reviewIndex.value += 1
  reviewAnswer.value = ''
  reviewNote.value = ''
  reviewAnswerRevealed.value = false
  reviewResult.value = null
  reviewError.value = ''
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

function questionImageUrl(content = '') {
  const match = String(content).match(/!\[[^\]]*]\(([^)]+)\)/)
  return match?.[1] || ''
}

function questionText(content = '') {
  return String(content).replace(/!\[[^\]]*]\([^)]+\)/g, '').trim() || '图片题'
}

function assetUrl(url) {
  return resolveAssetUrl(url)
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

.wrong-review-board {
  margin-bottom: 18px;
}

.wrong-review-player {
  display: grid;
  gap: 14px;
  padding: 20px;
  border: 1px solid rgba(60, 94, 210, 0.14);
  background:
    linear-gradient(110deg, rgba(236, 242, 255, 0.84), rgba(255, 255, 255, 0.72)),
    repeating-linear-gradient(135deg, rgba(60, 94, 210, 0.025) 0 1px, transparent 1px 12px);
}

.wrong-review-player header,
.wrong-review-form footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.wrong-review-player header span,
.wrong-review-meta span,
.wrong-review-form label span {
  color: #6f7da3;
  font-size: 12px;
  font-weight: 900;
}

.wrong-review-player h3,
.wrong-review-form h4 {
  margin: 0;
  color: #173f91;
  font-weight: 900;
}

.wrong-review-player header button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid rgba(60, 94, 210, 0.18);
  color: #3159b7;
  background: rgba(255, 255, 255, 0.74);
  font-weight: 900;
}

.wrong-review-progress {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #6f7da3;
  font-size: 12px;
  font-weight: 900;
}

.wrong-review-progress i {
  display: block;
  flex: 1;
  height: 8px;
  overflow: hidden;
  background: rgba(60, 94, 210, 0.1);
}

.wrong-review-progress b {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #5d7fe0, #e0b8ef);
}

.wrong-review-form {
  display: grid;
  gap: 16px;
}

.wrong-review-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.wrong-review-meta span {
  padding: 5px 8px;
  border: 1px solid rgba(60, 94, 210, 0.13);
  background: rgba(255, 255, 255, 0.66);
}

.wrong-review-form h4 {
  padding: 16px;
  border: 1px solid rgba(60, 94, 210, 0.14);
  background: rgba(255, 255, 255, 0.66);
  color: #123985;
  font-size: clamp(18px, 2vw, 26px);
  line-height: 1.75;
}

.wrong-review-form img {
  max-width: 100%;
  max-height: 56vh;
  object-fit: contain;
  border: 1px solid rgba(60, 94, 210, 0.13);
  background: #fff;
}

.wrong-review-options {
  display: grid;
  gap: 9px;
}

.wrong-review-options button {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 44px;
  padding: 8px 12px;
  border: 1px solid rgba(60, 94, 210, 0.14);
  color: #405b9f;
  background: rgba(255, 255, 255, 0.68);
  text-align: left;
  font-weight: 800;
}

.wrong-review-options button.selected,
.wrong-review-options button:hover {
  border-color: rgba(50, 108, 255, 0.4);
  background: rgba(236, 242, 255, 0.92);
}

.wrong-review-options button:disabled,
.wrong-review-form textarea:disabled {
  cursor: default;
  opacity: 0.86;
}

.wrong-review-options b {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  color: #3159b7;
  background: rgba(93, 127, 224, 0.13);
}

.wrong-review-form label {
  display: grid;
  gap: 8px;
}

.wrong-review-form textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid rgba(60, 94, 210, 0.16);
  color: #334f86;
  background: rgba(255, 255, 255, 0.72);
  font: inherit;
  line-height: 1.6;
  resize: vertical;
}

.wrong-review-result {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid rgba(60, 94, 210, 0.18);
  color: #284987;
  background:
    linear-gradient(180deg, rgba(236, 242, 255, 0.9), rgba(255, 255, 255, 0.76)),
    repeating-linear-gradient(135deg, rgba(60, 94, 210, 0.022) 0 1px, transparent 1px 12px);
}

.wrong-review-result.correct {
  border-color: rgba(47, 128, 93, 0.22);
  color: #2f805d;
  background: rgba(240, 250, 245, 0.82);
}

.wrong-review-result p {
  margin: 0;
  line-height: 1.65;
}

.wrong-review-form footer {
  flex-wrap: wrap;
}

.wrong-judge-correct,
.wrong-judge-wrong {
  min-height: 40px;
  padding: 0 14px;
  border-radius: 4px;
  font-weight: 900;
}

.wrong-judge-correct {
  color: #2f805d;
  border: 1px solid rgba(47, 128, 93, 0.22);
  background: rgba(240, 250, 245, 0.88);
}

.wrong-judge-wrong {
  color: #b14556;
  border: 1px solid rgba(201, 82, 98, 0.24);
  background: rgba(255, 245, 246, 0.9);
}

.wrong-card-full h3 {
  overflow-wrap: anywhere;
}

.wrong-card-image {
  display: block;
  width: 100%;
  max-height: 220px;
  object-fit: contain;
  border: 1px solid rgba(60, 94, 210, 0.14);
  background: rgba(255, 255, 255, 0.78);
}

.wrong-review-empty {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 160px;
  color: #7482a6;
  text-align: center;
}

.wrong-review-empty strong {
  color: #173f91;
  font-size: 18px;
}

.full-link-stack button:disabled,
.wrong-review-player header button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}
</style>
