<template>
  <div class="ai-workspace">
    <aside class="ai-rail">
      <button class="ai-back" type="button" @click="emit('back-home')">
        <ChevronLeft :size="18" />
        <strong>学习控制台</strong>
      </button>

      <section class="ai-brand">
        <small>AI / STUDY COPILOT</small>
        <h1>AI 辅助</h1>
        <p>读取资料文字、整理题目草稿、提炼知识要点，并生成可执行的学习计划。</p>
      </section>

      <section class="ai-status" :class="{ ready: status.configured }">
        <span>{{ status.configured ? '已接入' : '未接入' }}</span>
        <strong>{{ status.provider || 'DeepSeek' }}</strong>
        <small>{{ status.model || 'deepseek-v4-flash' }}</small>
      </section>
    </aside>

    <main class="ai-main">
      <header class="ai-topbar">
        <div>
          <span>DEEPSEEK ASSISTANT</span>
          <h2>资料到题库、知识点与计划</h2>
        </div>
        <button type="button" @click="loadInitialData">
          <RefreshCw :size="17" />
          <span>刷新</span>
        </button>
      </header>

      <section class="ai-grid">
        <section class="ai-panel ai-input">
          <header>
            <span>INPUT</span>
            <h3>分析来源</h3>
          </header>
          <label>
            <span>选择学习资料</span>
            <select v-model="form.resourceId">
              <option :value="null">不选择，使用粘贴文本</option>
              <option v-for="resource in resources" :key="resource.id" :value="resource.id">
                {{ resource.name }}
              </option>
            </select>
          </label>
          <label>
            <span>粘贴资料文字</span>
            <textarea v-model.trim="form.text" rows="8" maxlength="20000" placeholder="可粘贴教材、讲义、题目截图 OCR 后的文字，留空则读取所选资料。" />
          </label>
          <div class="ai-form-row">
            <label>
              <span>科目</span>
              <input v-model.trim="form.subject" maxlength="80" placeholder="例如：高等数学" />
            </label>
            <label>
              <span>知识点</span>
              <input v-model.trim="form.knowledgePoint" maxlength="80" placeholder="例如：区间估计" />
            </label>
          </div>
          <div class="ai-form-row compact">
            <label>
              <span>题目数</span>
              <input v-model.number="form.questionLimit" type="number" min="1" max="20" />
            </label>
            <label>
              <span>计划天数</span>
              <input v-model.number="form.planDays" type="number" min="1" max="14" />
            </label>
            <label>
              <span>开始日期</span>
              <input v-model="form.planStartDate" type="date" />
            </label>
          </div>
          <button class="ai-primary" type="button" :disabled="analyzing" @click="runAnalysis">
            <Sparkles :size="18" />
            <span>{{ analyzing ? '分析中...' : '开始 AI 分析' }}</span>
          </button>
          <p v-if="error" class="ai-error">{{ error }}</p>
        </section>

        <section class="ai-panel ai-results">
          <header>
            <span>QUESTION DRAFTS</span>
            <h3>题目草稿</h3>
          </header>
          <p v-if="!analysis.questions?.length" class="ai-empty">AI 分析后会在这里生成可入库题目。</p>
          <article v-for="(question, index) in analysis.questions || []" :key="`${question.content}-${index}`" class="ai-question-card">
            <div>
              <strong>{{ questionTypeLabel(question.questionType) }}</strong>
              <span>{{ question.difficulty || 3 }} 星</span>
            </div>
            <p>{{ question.content }}</p>
            <ul v-if="question.options?.length">
              <li v-for="option in question.options" :key="option">{{ option }}</li>
            </ul>
            <small>答案：{{ question.correctAnswer || '待补充' }}</small>
            <button type="button" :disabled="savingQuestionIndex === index" @click="saveQuestion(question, index)">
              <PlusCircle :size="16" />
              <span>{{ savingQuestionIndex === index ? '保存中...' : '加入题库' }}</span>
            </button>
          </article>
        </section>

        <section class="ai-panel">
          <header>
            <span>KNOWLEDGE</span>
            <h3>知识要点</h3>
          </header>
          <p v-if="!analysis.knowledgePoints?.length" class="ai-empty">这里会展示关键概念、易错点和优先级。</p>
          <article v-for="point in analysis.knowledgePoints || []" :key="point.title" class="ai-knowledge-card">
            <div>
              <strong>{{ point.title }}</strong>
              <span>P{{ point.priority || 3 }}</span>
            </div>
            <p>{{ point.summary }}</p>
            <ul>
              <li v-for="item in point.keyItems || []" :key="item">{{ item }}</li>
            </ul>
            <small v-if="point.pitfalls?.length">易错：{{ point.pitfalls.join('；') }}</small>
          </article>
        </section>

        <section class="ai-panel">
          <header>
            <span>PLAN</span>
            <h3>学习计划建议</h3>
          </header>
          <p v-if="!analysis.plans?.length" class="ai-empty">AI 会生成接下来几天的学习安排。</p>
          <article v-for="(plan, index) in analysis.plans || []" :key="`${plan.title}-${index}`" class="ai-plan-card">
            <time>{{ plan.planDate }}</time>
            <strong>{{ plan.title }}</strong>
            <p>{{ plan.content }}</p>
            <button type="button" :disabled="savingPlanIndex === index" @click="savePlan(plan, index)">
              <CalendarPlus :size="16" />
              <span>{{ savingPlanIndex === index ? '保存中...' : '加入计划' }}</span>
            </button>
          </article>
        </section>
      </section>

      <section v-if="analysis.warnings?.length" class="ai-warnings">
        <strong>提示</strong>
        <span v-for="warning in analysis.warnings" :key="warning">{{ warning }}</span>
      </section>
    </main>

    <p v-if="toast" class="ai-toast">{{ toast }}</p>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import {
  CalendarPlus,
  ChevronLeft,
  PlusCircle,
  RefreshCw,
  Sparkles,
} from '@lucide/vue'
import {
  analyzeWithAiAssistant,
  createQuestion,
  createStudyPlan,
  getAiAssistantStatus,
  listLearningResources,
} from '../../api'

const emit = defineEmits(['back-home'])

const resources = ref([])
const status = reactive({ configured: false, provider: 'DeepSeek', model: 'deepseek-v4-flash' })
const form = reactive({
  resourceId: null,
  text: '',
  subject: '',
  knowledgePoint: '',
  questionLimit: 6,
  planDays: 3,
  planStartDate: new Date().toISOString().slice(0, 10),
})
const analysis = reactive({
  questions: [],
  knowledgePoints: [],
  plans: [],
  warnings: [],
  sourceTitle: '',
  sourceExcerpt: '',
})
const analyzing = ref(false)
const error = ref('')
const toast = ref('')
const savingQuestionIndex = ref(null)
const savingPlanIndex = ref(null)

onMounted(loadInitialData)

async function loadInitialData() {
  await Promise.all([loadStatus(), loadResources()])
}

async function loadStatus() {
  const response = await getAiAssistantStatus()
  Object.assign(status, response.data?.data || {})
}

async function loadResources() {
  const response = await listLearningResources({ page: 1, size: 100 })
  resources.value = response.data?.data?.records || []
}

async function runAnalysis() {
  error.value = ''
  analyzing.value = true
  try {
    const response = await analyzeWithAiAssistant({
      resourceId: form.text ? null : form.resourceId,
      text: form.text || null,
      subject: form.subject || null,
      knowledgePoint: form.knowledgePoint || null,
      questionLimit: form.questionLimit,
      planDays: form.planDays,
      planStartDate: form.planStartDate,
    })
    Object.assign(analysis, response.data?.data || {})
    showToast('AI 分析完成')
  } catch (exception) {
    error.value = exception.response?.data?.message || 'AI 分析失败'
  } finally {
    analyzing.value = false
  }
}

async function saveQuestion(question, index) {
  savingQuestionIndex.value = index
  try {
    await createQuestion({
      content: question.content,
      questionType: question.questionType || 'SHORT_ANSWER',
      options: question.options || [],
      correctAnswer: question.correctAnswer || '待补充',
      analysis: question.analysis || null,
      difficulty: question.difficulty || 3,
      subject: question.subject || form.subject || '学习资料',
      knowledgePoint: question.knowledgePoint || form.knowledgePoint || null,
      status: 'DRAFT',
      sourceType: 'AI_ASSISTANT',
      sourceResourceId: form.resourceId || null,
      sourceResourceName: analysis.sourceTitle || null,
      sourceExcerpt: question.sourceExcerpt || analysis.sourceExcerpt || null,
    })
    showToast('题目已加入题库草稿')
  } catch (exception) {
    error.value = exception.response?.data?.message || '题目保存失败'
  } finally {
    savingQuestionIndex.value = null
  }
}

async function savePlan(plan, index) {
  savingPlanIndex.value = index
  try {
    await createStudyPlan({
      title: plan.title,
      content: plan.content || '',
      planDate: plan.planDate || form.planStartDate,
      targetType: plan.targetType || 'RESOURCE',
      targetId: form.resourceId || null,
      targetTitle: plan.targetTitle || analysis.sourceTitle || 'AI 辅助建议',
    })
    showToast('学习计划已创建')
  } catch (exception) {
    error.value = exception.response?.data?.message || '学习计划保存失败'
  } finally {
    savingPlanIndex.value = null
  }
}

function questionTypeLabel(type) {
  return {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '简答题',
  }[type] || '简答题'
}

function showToast(message) {
  toast.value = message
  window.setTimeout(() => {
    if (toast.value === message) toast.value = ''
  }, 2400)
}
</script>

<style scoped>
.ai-workspace {
  --ai-blue: #4f88ff;
  --ai-cyan: #6fd5ff;
  --ai-pink: #f0a6df;
  --ai-ink: #1e2c54;
  --ai-muted: #6f7da3;
  --ai-line: rgba(73, 116, 221, 0.18);
  display: grid;
  grid-template-columns: 210px minmax(0, 1fr);
  min-height: 100vh;
  color: var(--ai-ink);
  background:
    linear-gradient(118deg, rgba(79, 136, 255, 0.08), transparent 34%),
    linear-gradient(240deg, rgba(240, 166, 223, 0.13), transparent 42%),
    url("/images/resource-eva-bg.png") top right / 760px auto no-repeat,
    #f7faff;
}

.ai-rail {
  display: grid;
  align-content: start;
  gap: 18px;
  padding: 18px 12px;
  border-right: 1px solid rgba(73, 116, 221, 0.14);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(242, 247, 255, 0.56));
  backdrop-filter: blur(16px);
}

.ai-back,
.ai-topbar button,
.ai-primary,
.ai-question-card button,
.ai-plan-card button {
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 900;
}

.ai-back,
.ai-topbar button {
  color: var(--ai-blue);
  background: rgba(255, 255, 255, 0.68);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.13);
}

.ai-brand small,
.ai-panel header span,
.ai-topbar span {
  color: var(--ai-muted);
  font-size: 12px;
  font-weight: 900;
}

.ai-brand h1,
.ai-topbar h2,
.ai-panel h3 {
  margin: 0;
  font-weight: 900;
}

.ai-brand h1 {
  margin: 10px 0 8px;
  color: var(--ai-blue);
  font-size: 30px;
}

.ai-brand p {
  color: var(--ai-muted);
  font-size: 13px;
  font-weight: 800;
  line-height: 1.7;
}

.ai-status {
  display: grid;
  gap: 5px;
  padding: 12px;
  border: 1px solid var(--ai-line);
  background: rgba(255, 255, 255, 0.62);
}

.ai-status.ready {
  border-color: rgba(75, 185, 139, 0.35);
  background: rgba(232, 255, 247, 0.72);
}

.ai-status span,
.ai-status small {
  color: var(--ai-muted);
  font-size: 12px;
  font-weight: 900;
}

.ai-main {
  display: grid;
  align-content: start;
  gap: 16px;
  padding: 18px;
}

.ai-topbar {
  display: flex;
  min-height: 72px;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--ai-line);
}

.ai-grid {
  display: grid;
  grid-template-columns: minmax(280px, 0.9fr) minmax(360px, 1.2fr) minmax(280px, 0.9fr) minmax(280px, 0.9fr);
  gap: 14px;
  align-items: start;
}

.ai-panel {
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--ai-line);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 18px 42px rgba(56, 75, 130, 0.08);
}

.ai-panel header {
  display: grid;
  gap: 4px;
}

.ai-input label,
.ai-form-row label {
  display: grid;
  gap: 7px;
  min-width: 0;
  color: var(--ai-muted);
  font-size: 12px;
  font-weight: 900;
}

.ai-input input,
.ai-input select,
.ai-input textarea {
  width: 100%;
  min-width: 0;
  border: 1px solid var(--ai-line);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.75);
  color: var(--ai-ink);
  font: inherit;
  font-size: 13px;
  font-weight: 800;
}

.ai-input input,
.ai-input select {
  min-height: 38px;
  padding: 0 10px;
}

.ai-input textarea {
  resize: vertical;
  padding: 10px;
  line-height: 1.6;
}

.ai-form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.ai-form-row.compact {
  grid-template-columns: 0.7fr 0.7fr 1fr;
}

.ai-primary {
  color: #fff;
  background: linear-gradient(100deg, var(--ai-blue), var(--ai-cyan), var(--ai-pink));
}

.ai-primary:disabled,
.ai-question-card button:disabled,
.ai-plan-card button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.ai-question-card,
.ai-knowledge-card,
.ai-plan-card {
  display: grid;
  gap: 9px;
  padding: 12px;
  border: 1px solid rgba(73, 116, 221, 0.13);
  background: rgba(248, 251, 255, 0.74);
}

.ai-question-card div,
.ai-knowledge-card div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.ai-question-card strong,
.ai-knowledge-card strong,
.ai-plan-card strong {
  font-weight: 900;
}

.ai-question-card span,
.ai-knowledge-card span,
.ai-question-card small,
.ai-knowledge-card small,
.ai-plan-card time {
  color: var(--ai-muted);
  font-size: 12px;
  font-weight: 900;
}

.ai-question-card p,
.ai-knowledge-card p,
.ai-plan-card p {
  margin: 0;
  color: var(--ai-ink);
  font-size: 13px;
  font-weight: 800;
  line-height: 1.6;
}

.ai-question-card ul,
.ai-knowledge-card ul {
  display: grid;
  gap: 4px;
  margin: 0;
  padding-left: 18px;
  color: var(--ai-muted);
  font-size: 12px;
  font-weight: 800;
}

.ai-question-card button,
.ai-plan-card button {
  justify-self: start;
  min-height: 34px;
  padding: 0 12px;
  color: var(--ai-blue);
  background: rgba(235, 242, 255, 0.9);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.14);
}

.ai-empty,
.ai-error {
  margin: 0;
  color: var(--ai-muted);
  font-size: 13px;
  font-weight: 900;
  line-height: 1.7;
}

.ai-error {
  color: #d5576a;
}

.ai-warnings {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  color: var(--ai-muted);
  font-size: 12px;
  font-weight: 900;
}

.ai-warnings span {
  padding: 6px 9px;
  border: 1px solid rgba(237, 199, 103, 0.35);
  background: rgba(255, 249, 228, 0.8);
}

.ai-toast {
  position: fixed;
  right: 22px;
  bottom: 22px;
  z-index: 20;
  margin: 0;
  padding: 12px 16px;
  color: #fff;
  border-radius: 6px;
  background: linear-gradient(100deg, var(--ai-blue), var(--ai-cyan), var(--ai-pink));
  font-weight: 900;
}

@media (max-width: 1320px) {
  .ai-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 900px) {
  .ai-workspace,
  .ai-grid,
  .ai-form-row,
  .ai-form-row.compact {
    grid-template-columns: 1fr;
  }
}
</style>
