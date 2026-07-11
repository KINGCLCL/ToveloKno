<template>
  <div class="full-module plan-workspace">
    <aside class="full-module-rail">
      <button class="full-back" type="button" @click="emit('back-home')">
        <span>‹</span>
        <strong>学习控制台</strong>
      </button>
      <section class="full-module-brand">
        <small>PLAN ROUTE</small>
        <h1>学习计划</h1>
        <p>把学习任务拆成可执行路线，用艾宾浩斯间隔安排复习节奏。</p>
      </section>
      <section class="full-stat-stack">
        <article><span>全部计划</span><strong>{{ total }}</strong></article>
        <article><span>今日复习</span><strong>{{ todayReviewCount }}</strong></article>
        <article><span>逾期任务</span><strong>{{ overdueCount }}</strong></article>
        <article><span>完成率</span><strong>{{ completionRate }}%</strong></article>
      </section>
      <section class="full-link-stack">
        <button type="button" @click="emit('open-module', 'wrong')">查看错题本</button>
        <button type="button" @click="emit('open-module', 'resources')">整理学习资料</button>
      </section>
    </aside>

    <main class="full-module-main">
      <header class="full-module-topbar">
        <div>
          <span class="full-kicker">TOVELOKNO / STUDY OPS</span>
          <h2>计划工作台</h2>
        </div>
        <button class="full-primary" type="button" @click="openEditor()">新增计划</button>
      </header>

      <section class="full-filter-strip">
        <label>
          <span>状态</span>
          <select v-model="filters.status" @change="reloadFirstPage">
            <option value="">全部</option>
            <option value="pending">待完成</option>
            <option value="completed">已完成</option>
            <option value="cancelled">已取消</option>
          </select>
        </label>
        <label>
          <span>日期</span>
          <input v-model="filters.planDate" type="date" @change="reloadFirstPage" />
        </label>
        <button type="button" @click="clearFilters">清除筛选</button>
      </section>

      <section class="memory-panel">
        <div>
          <span class="full-kicker">EBBINGHAUS REVIEW</span>
          <h3>记忆曲线复习节奏</h3>
          <p>看曲线安排复习：记忆保持率会快速下滑，每一次复习都会把曲线重新抬高。</p>
        </div>
        <div class="memory-chart-wrap">
          <svg class="memory-chart" viewBox="0 0 720 250" role="img" aria-label="艾宾浩斯遗忘曲线图">
            <defs>
              <linearGradient id="memoryArea" x1="0" x2="0" y1="0" y2="1">
                <stop offset="0%" stop-color="#5f8cff" stop-opacity="0.28" />
                <stop offset="100%" stop-color="#f0a6df" stop-opacity="0.05" />
              </linearGradient>
              <linearGradient id="memoryStroke" x1="0" x2="1" y1="0" y2="0">
                <stop offset="0%" stop-color="#4f88ff" />
                <stop offset="56%" stop-color="#35c5a8" />
                <stop offset="100%" stop-color="#f0a6df" />
              </linearGradient>
            </defs>
            <line x1="56" y1="28" x2="56" y2="210" class="chart-axis" />
            <line x1="56" y1="210" x2="680" y2="210" class="chart-axis" />
            <g class="chart-grid">
              <line v-for="tick in chartTicks" :key="tick.y" x1="56" :y1="tick.y" x2="680" :y2="tick.y" />
            </g>
            <text x="20" y="38" class="chart-label">记忆</text>
            <text x="648" y="236" class="chart-label">时间</text>
            <path :d="memoryAreaPath" class="chart-area" />
            <path :d="memoryCurvePath" class="chart-curve" />
            <g v-for="point in memoryCurvePoints" :key="point.label" class="chart-point">
              <line :x1="point.x" :y1="point.y" :x2="point.x" y2="210" />
              <circle :cx="point.x" :cy="point.y" r="7" />
              <text :x="point.x" :y="point.y - 15">{{ point.label }}</text>
              <text :x="point.x" y="232">{{ point.dayLabel }}</text>
            </g>
          </svg>
          <div class="memory-insights">
            <article>
              <span>今日策略</span>
              <strong>{{ todayPlans.length ? '主动回忆优先' : '建立新周期' }}</strong>
              <small>{{ todayPlans.length ? '先不看答案，闭卷复述 3 个关键词。' : '新增计划时勾选记忆曲线，一次生成复习周期。' }}</small>
            </article>
            <article>
              <span>补救建议</span>
              <strong>{{ overduePlans.length ? '先清逾期' : '节奏健康' }}</strong>
              <small>{{ overduePlans.length ? '逾期任务先做 15 分钟压缩复盘。' : '可以安排 1 个新知识点进入周期。' }}</small>
            </article>
          </div>
        </div>
      </section>

      <section class="memory-steps">
        <article v-for="step in memorySteps" :key="step.label" :class="{ today: step.offsetDays === 0 }">
          <strong>{{ step.label }}</strong>
          <span>{{ step.description }}</span>
        </article>
      </section>

      <section class="review-board">
        <article>
          <span>今天要处理</span>
          <strong>{{ todayPlans.length }}</strong>
          <small>{{ todayPlans.length ? todayPlans.map((plan) => plan.title).slice(0, 2).join(' / ') : '今天没有排程' }}</small>
        </article>
        <article>
          <span>需要补救</span>
          <strong>{{ overduePlans.length }}</strong>
          <small>{{ overduePlans.length ? '优先清掉逾期任务，避免复习断档' : '没有逾期，节奏很好' }}</small>
        </article>
        <article>
          <span>关联题库</span>
          <strong>{{ linkOverview.resourceQuestionCount ?? '-' }}</strong>
          <small>资料摘题和 AI 题会进入题库链路</small>
        </article>
      </section>

      <section v-if="loading" class="full-empty">计划加载中...</section>
      <section v-else-if="!plans.length" class="full-empty">暂无学习计划，先新增一条路线。</section>

      <section v-else class="full-card-grid plan-grid">
        <article v-for="plan in plans" :key="plan.id" class="full-card plan-card-full" :class="plan.status">
          <div class="plan-date-block">
            <span>{{ dateMonth(plan.planDate) }}</span>
            <strong>{{ dateDay(plan.planDate) }}</strong>
          </div>
          <div class="plan-copy">
            <span>{{ statusLabel(plan.status) }}</span>
            <h3>{{ plan.title }}</h3>
            <em v-if="plan.targetTitle" class="plan-target">{{ targetTypeLabel(plan.targetType) }}：{{ plan.targetTitle }}</em>
            <p>{{ plan.content || '暂无详细内容，保持计划队列清晰。' }}</p>
            <small>更新于 {{ formatDateTime(plan.updatedAt) }}</small>
          </div>
          <footer>
            <button type="button" @click="updateStatus(plan, plan.status === 'completed' ? 'pending' : 'completed')">
              {{ plan.status === 'completed' ? '撤回完成' : '完成' }}
            </button>
            <button v-if="plan.status === 'pending'" type="button" @click="updateStatus(plan, 'cancelled')">取消</button>
            <button type="button" @click="openEditor(plan)">编辑</button>
            <button type="button" class="danger" @click="removePlan(plan.id)">删除</button>
          </footer>
        </article>
      </section>

      <footer class="full-pagination">
        <button type="button" :disabled="page <= 1" @click="page -= 1; loadPlans()">上一页</button>
        <span>第 {{ page }} / {{ totalPages }} 页，共 {{ total }} 条</span>
        <button type="button" :disabled="page >= totalPages" @click="page += 1; loadPlans()">下一页</button>
      </footer>
    </main>

    <div v-if="editorOpen" class="full-modal-backdrop" @click.self="closeEditor">
      <section class="full-modal">
        <header>
          <h3>{{ editingPlan ? '编辑计划' : '新增计划' }}</h3>
          <button type="button" @click="closeEditor">×</button>
        </header>
        <form class="full-form" @submit.prevent="submitPlan">
          <label><span>标题</span><input v-model.trim="form.title" required maxlength="150" placeholder="例如：复习第三章" /></label>
          <label><span>日期</span><input v-model="form.planDate" required type="date" /></label>
          <section v-if="!editingPlan" class="memory-generator">
            <label>
              <input v-model="form.useMemoryCurve" type="checkbox" />
              <span>按艾宾浩斯遗忘曲线生成复习周期</span>
            </label>
            <p>会从开始日期起生成 5 分钟、30 分钟、1 天、2 天、4 天、7 天、15 天的复习计划。</p>
          </section>
          <div class="plan-target-grid">
            <label>
              <span>关联类型</span>
              <select v-model="form.targetType">
                <option value="">不关联</option>
                <option value="RESOURCE">学习资料</option>
                <option value="QUESTION">题库题目</option>
                <option value="WRONG_QUESTION">错题复盘</option>
              </select>
            </label>
            <label>
              <span>关联 ID</span>
              <input v-model.number="form.targetId" type="number" min="1" placeholder="可选" />
            </label>
          </div>
          <label><span>关联标题</span><input v-model.trim="form.targetTitle" maxlength="180" placeholder="例如：第三章 PDF / 第 12 题" /></label>
          <label><span>内容</span><textarea v-model.trim="form.content" maxlength="2000" rows="5" placeholder="记录具体学习内容" /></label>
          <p v-if="error" class="full-error">{{ error }}</p>
          <footer>
            <button type="button" @click="closeEditor">取消</button>
            <button class="full-primary" type="submit" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
          </footer>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createStudyPlan,
  deleteStudyPlan,
  fetchStudyLinkOverview,
  listStudyPlans,
  updateStudyPlan,
  updateStudyPlanStatus,
} from '../../api'

const emit = defineEmits(['back-home', 'open-module'])

const loading = ref(false)
const saving = ref(false)
const editorOpen = ref(false)
const editingPlan = ref(null)
const error = ref('')
const plans = ref([])
const page = ref(1)
const size = 8
const total = ref(0)
const totalPages = ref(1)
const filters = reactive({ status: '', planDate: '' })
const linkOverview = ref({})
const form = reactive({
  title: '',
  content: '',
  planDate: new Date().toISOString().slice(0, 10),
  targetType: '',
  targetId: null,
  targetTitle: '',
  useMemoryCurve: false,
})

const today = computed(() => new Date().toISOString().slice(0, 10))
const todayPlans = computed(() => plans.value.filter((plan) => plan.status === 'pending' && plan.planDate === today.value))
const overduePlans = computed(() => plans.value.filter((plan) => plan.status === 'pending' && plan.planDate < today.value))
const todayReviewCount = computed(() => todayPlans.value.length)
const overdueCount = computed(() => overduePlans.value.length)
const completionRate = computed(() => {
  if (!plans.value.length) return 0
  return Math.round((plans.value.filter((plan) => plan.status === 'completed').length / plans.value.length) * 100)
})
const memorySteps = [
  { label: '5 分钟', offsetDays: 0, description: '刚学完立刻回看关键词' },
  { label: '30 分钟', offsetDays: 0, description: '补第一轮遗忘缺口' },
  { label: '1 天', offsetDays: 1, description: '隔天主动回忆' },
  { label: '2 天', offsetDays: 2, description: '用题目检查掌握度' },
  { label: '4 天', offsetDays: 4, description: '复盘易错与盲点' },
  { label: '7 天', offsetDays: 7, description: '周复习巩固结构' },
  { label: '15 天', offsetDays: 15, description: '长期记忆再激活' },
]
const memoryCurvePoints = [
  { label: '初学', dayLabel: '0', x: 72, y: 42 },
  { label: '5m', dayLabel: '5分', x: 150, y: 82 },
  { label: '30m', dayLabel: '30分', x: 230, y: 112 },
  { label: '1d', dayLabel: '1天', x: 320, y: 128 },
  { label: '2d', dayLabel: '2天', x: 405, y: 142 },
  { label: '4d', dayLabel: '4天', x: 500, y: 154 },
  { label: '7d', dayLabel: '7天', x: 590, y: 166 },
  { label: '15d', dayLabel: '15天', x: 670, y: 178 },
]
const chartTicks = [
  { y: 60 },
  { y: 100 },
  { y: 140 },
  { y: 180 },
]
const memoryCurvePath = computed(() => {
  const points = memoryCurvePoints
  return [
    `M ${points[0].x} ${points[0].y}`,
    `C 110 46, 114 78, ${points[1].x} ${points[1].y}`,
    `S 190 104, ${points[2].x} ${points[2].y}`,
    `S 282 126, ${points[3].x} ${points[3].y}`,
    `S 372 140, ${points[4].x} ${points[4].y}`,
    `S 464 152, ${points[5].x} ${points[5].y}`,
    `S 554 164, ${points[6].x} ${points[6].y}`,
    `S 640 176, ${points[7].x} ${points[7].y}`,
  ].join(' ')
})
const memoryAreaPath = computed(() => `${memoryCurvePath.value} L 670 210 L 72 210 Z`)

onMounted(() => {
  loadPlans()
  loadStudyLinks()
})

async function loadStudyLinks() {
  try {
    const response = await fetchStudyLinkOverview()
    linkOverview.value = response.data?.data || {}
  } catch {
    linkOverview.value = {}
  }
}

async function loadPlans() {
  loading.value = true
  try {
    const response = await listStudyPlans({
      page: page.value,
      size,
      status: filters.status || undefined,
      planDate: filters.planDate || undefined,
    })
    const data = response.data?.data || {}
    plans.value = data.records || []
    total.value = data.total || 0
    totalPages.value = data.totalPages || 1
  } finally {
    loading.value = false
  }
}

function reloadFirstPage() {
  page.value = 1
  loadPlans()
}

function clearFilters() {
  filters.status = ''
  filters.planDate = ''
  reloadFirstPage()
}

function openEditor(plan = null) {
  editingPlan.value = plan
  form.title = plan?.title || ''
  form.content = plan?.content || ''
  form.planDate = plan?.planDate || new Date().toISOString().slice(0, 10)
  form.targetType = plan?.targetType || ''
  form.targetId = plan?.targetId || null
  form.targetTitle = plan?.targetTitle || ''
  form.useMemoryCurve = false
  error.value = ''
  editorOpen.value = true
}

function closeEditor() {
  editorOpen.value = false
  editingPlan.value = null
}

async function submitPlan() {
  if (!form.title || !form.planDate) {
    error.value = '标题和日期不能为空'
    return
  }
  saving.value = true
  error.value = ''
  try {
    const payload = {
      title: form.title,
      content: form.content || null,
      planDate: form.planDate,
      targetType: form.targetType || null,
      targetId: form.targetType ? form.targetId || null : null,
      targetTitle: form.targetType ? form.targetTitle || null : null,
    }
    if (editingPlan.value) {
      await updateStudyPlan(editingPlan.value.id, payload)
    } else if (form.useMemoryCurve) {
      await createMemoryCurvePlans(payload)
    } else {
      await createStudyPlan(payload)
    }
    closeEditor()
    await loadPlans()
    await loadStudyLinks()
  } catch (err) {
    error.value = err.response?.data?.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function createMemoryCurvePlans(basePayload) {
  const startDate = parseDate(basePayload.planDate)
  const title = basePayload.title.trim()
  const content = basePayload.content || '按艾宾浩斯节奏进行主动回忆、错题检查和知识点复述。'
  const reviewPlans = memorySteps.map((step, index) => ({
    ...basePayload,
    title: `${title} · ${step.label}复习`,
    content: [
      content,
      `复习节点：${step.label}`,
      `建议动作：${step.description}`,
    ].join('\n'),
    planDate: formatDate(addDays(startDate, step.offsetDays)),
    targetTitle: basePayload.targetTitle || title,
    targetType: basePayload.targetType || 'REVIEW',
    targetId: basePayload.targetId || null,
  }))
  for (const plan of reviewPlans) {
    await createStudyPlan(plan)
  }
}

async function updateStatus(plan, status) {
  await updateStudyPlanStatus(plan.id, status)
  await loadPlans()
  await loadStudyLinks()
}

async function removePlan(id) {
  await deleteStudyPlan(id)
  if (plans.value.length === 1 && page.value > 1) page.value -= 1
  await loadPlans()
  await loadStudyLinks()
}

function statusLabel(status) {
  return { pending: '待完成', completed: '已完成', cancelled: '已取消' }[status] || status
}

function targetTypeLabel(type) {
  return {
    RESOURCE: '资料',
    QUESTION: '题目',
    WRONG_QUESTION: '错题',
  }[type] || '关联'
}

function dateMonth(date) {
  return date ? date.slice(5, 7) : '--'
}

function dateDay(date) {
  return date ? date.slice(8, 10) : '--'
}

function formatDateTime(value) {
  if (!value) return '暂无记录'
  return value.replace('T', ' ').slice(0, 16)
}

function parseDate(value) {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}

function addDays(date, days) {
  const next = new Date(date)
  next.setDate(next.getDate() + days)
  return next
}

function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
</script>

<style scoped>
.plan-target {
  display: inline-flex;
  width: fit-content;
  max-width: 100%;
  margin: -2px 0 6px;
  padding: 5px 8px;
  overflow: hidden;
  color: #244995;
  border: 1px solid rgba(60, 94, 210, 0.14);
  border-radius: 6px;
  background: rgba(236, 242, 255, 0.62);
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-target-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 150px;
  gap: 12px;
}

.memory-panel,
.review-board {
  display: grid;
  gap: 14px;
  border: 1px solid rgba(73, 116, 221, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.68);
  box-shadow: 0 18px 44px rgba(68, 92, 160, 0.08);
}

.memory-panel {
  grid-template-columns: minmax(220px, 0.55fr) minmax(0, 1.45fr);
  align-items: center;
  padding: 18px;
}

.memory-panel h3,
.memory-panel p {
  margin: 0;
}

.memory-panel h3 {
  color: #25478f;
  font-size: 22px;
  font-weight: 900;
}

.memory-panel p {
  margin-top: 7px;
  color: #6e7fae;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.7;
}

.memory-chart-wrap {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 210px;
  gap: 14px;
  align-items: stretch;
  min-width: 0;
}

.memory-chart {
  width: 100%;
  min-height: 250px;
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.74), rgba(244, 248, 255, 0.86)),
    radial-gradient(circle at 18% 14%, rgba(79, 136, 255, 0.16), transparent 30%);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.12);
}

.chart-axis {
  stroke: rgba(54, 81, 151, 0.32);
  stroke-width: 2;
}

.chart-grid line {
  stroke: rgba(54, 81, 151, 0.1);
  stroke-dasharray: 5 8;
}

.chart-area {
  fill: url(#memoryArea);
}

.chart-curve {
  fill: none;
  stroke: url(#memoryStroke);
  stroke-linecap: round;
  stroke-width: 5;
}

.chart-point line {
  stroke: rgba(73, 116, 221, 0.14);
  stroke-width: 1.5;
}

.chart-point circle {
  fill: #fff;
  stroke: #5b8cff;
  stroke-width: 4;
}

.chart-point text,
.chart-label {
  fill: #6075ad;
  font-size: 14px;
  font-weight: 900;
  text-anchor: middle;
}

.chart-label {
  text-anchor: start;
}

.memory-insights {
  display: grid;
  gap: 10px;
}

.memory-insights article {
  display: grid;
  align-content: center;
  gap: 7px;
  padding: 14px;
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(79, 136, 255, 0.14), rgba(240, 166, 223, 0.16));
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.12);
}

.memory-insights span,
.memory-insights small {
  color: #7180aa;
  font-size: 12px;
  font-weight: 900;
}

.memory-insights strong {
  color: #2954bd;
  font-size: 18px;
  font-weight: 900;
}

.memory-insights small {
  line-height: 1.55;
}

.memory-steps {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
}

.memory-steps article {
  display: grid;
  min-height: 86px;
  align-content: start;
  gap: 8px;
  padding: 12px;
  border: 1px solid rgba(73, 116, 221, 0.14);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(245, 249, 255, 0.92), rgba(255, 255, 255, 0.72));
}

.memory-steps article.today {
  border-color: rgba(79, 136, 255, 0.34);
  background: linear-gradient(135deg, rgba(79, 136, 255, 0.18), rgba(240, 166, 223, 0.2));
}

.memory-steps strong {
  color: #3469df;
  font-size: 15px;
  font-weight: 900;
}

.memory-steps span {
  color: #7380a7;
  font-size: 11px;
  font-weight: 800;
  line-height: 1.45;
}

.review-board {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  padding: 14px;
}

.review-board article {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 14px;
  border-radius: 8px;
  background: rgba(246, 249, 255, 0.76);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.1);
}

.review-board span,
.review-board small {
  color: #7483ac;
  font-weight: 900;
}

.review-board strong {
  color: #315fe0;
  font-size: 28px;
  font-weight: 900;
}

.review-board small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.memory-generator {
  display: grid;
  gap: 8px;
  padding: 12px;
  border-radius: 8px;
  background: rgba(238, 244, 255, 0.7);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.12);
}

.memory-generator label {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #2e56b0;
  font-weight: 900;
}

.memory-generator input {
  width: 18px;
  height: 18px;
}

.memory-generator p {
  margin: 0;
  color: #7583aa;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.6;
}

@media (max-width: 720px) {
  .plan-target-grid {
    grid-template-columns: 1fr;
  }

  .memory-panel,
  .review-board {
    grid-template-columns: 1fr;
  }

  .memory-chart-wrap {
    grid-template-columns: 1fr;
  }

  .memory-steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
