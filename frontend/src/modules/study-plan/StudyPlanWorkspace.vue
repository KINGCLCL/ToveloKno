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
        <p>把学习任务拆成可执行路线，按日期、状态和完成度推进。</p>
      </section>
      <section class="full-stat-stack">
        <article><span>全部计划</span><strong>{{ total }}</strong></article>
        <article><span>当前页待完成</span><strong>{{ pendingCount }}</strong></article>
        <article><span>当前页完成率</span><strong>{{ completionRate }}%</strong></article>
        <article><span>资料摘录题</span><strong>{{ linkOverview.resourceQuestionCount ?? '-' }}</strong></article>
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
})

const pendingCount = computed(() => plans.value.filter((plan) => plan.status === 'pending').length)
const completionRate = computed(() => {
  if (!plans.value.length) return 0
  return Math.round((plans.value.filter((plan) => plan.status === 'completed').length / plans.value.length) * 100)
})

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

@media (max-width: 720px) {
  .plan-target-grid {
    grid-template-columns: 1fr;
  }
}
</style>
