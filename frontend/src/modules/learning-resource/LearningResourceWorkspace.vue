<template>
  <div ref="workspaceRoot" class="resource-workspace" :class="{ fullscreen: readerFullscreen }">
    <aside class="resource-rail">
      <button class="resource-back" type="button" @click="emit('back-home')">
        <ChevronLeft :size="18" />
        <strong>学习控制台</strong>
      </button>

      <section class="resource-brand">
        <small>RESOURCE / NOTE</small>
        <h1>学习资料</h1>
        <p>上传 PDF、Word 与其他学习文件，阅读时保存标注和进度，后续统计直接从数据库汇总。</p>
      </section>

      <section class="resource-folders">
        <button
          v-for="folder in folders"
          :key="folder.type"
          type="button"
          :class="{ active: filters.type === folder.type }"
          @click="setType(folder.type)"
        >
          <span class="folder-icon"><component :is="folder.icon" :size="18" /></span>
          <strong>{{ folder.label }}</strong>
          <b>{{ folder.count }}</b>
        </button>
      </section>

      <label class="resource-upload">
        <input
          type="file"
          accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt,.md,.png,.jpg,.jpeg,.webp,.gif,.mp4,.mp3,.wav"
          @change="handleUpload"
        />
        <UploadCloud :size="20" />
        <span>{{ uploading ? '上传中...' : '上传资料' }}</span>
      </label>
    </aside>

    <main class="resource-main">
      <header class="resource-topbar">
        <div>
          <span class="resource-kicker">LIBRARY</span>
          <h2>{{ selectedResource ? selectedResource.name : '未归档笔记' }}</h2>
        </div>
        <div class="resource-actions">
          <label class="resource-search">
            <Search :size="16" />
            <input v-model.trim="filters.keyword" type="search" placeholder="搜索资料" @keyup.enter="reloadFirstPage" />
          </label>
          <button type="button" title="搜索" @click="reloadFirstPage"><Search :size="17" /><span>搜索</span></button>
          <button type="button" title="刷新" @click="loadResources"><RefreshCw :size="17" /><span>刷新</span></button>
        </div>
      </header>

      <section class="resource-grid-shell">
        <aside class="resource-list">
          <article
            v-for="resource in resources"
            :key="resource.id"
            :class="{ active: selectedResource?.id === resource.id }"
            @click="selectResource(resource)"
          >
            <span :class="['resource-file-mark', toneFor(resource)]">
              <component :is="fileIconFor(resource)" :size="22" />
              <em>{{ iconFor(resource) }}</em>
            </span>
            <div>
              <h3>{{ resource.name }}</h3>
              <p>{{ resource.type }} / {{ resource.sizeText }}</p>
              <i><b :style="{ width: `${displayProgress(resource)}%` }"></b></i>
            </div>
            <div class="resource-row-actions">
              <strong>{{ displayProgress(resource) }}%</strong>
              <button type="button" title="收藏" @click.stop="toggleFavorite(resource)">
                <Star :size="16" :fill="resource.favorite ? 'currentColor' : 'none'" />
              </button>
              <button type="button" title="删除" @click.stop="removeResource(resource)">
                <Trash2 :size="16" />
              </button>
            </div>
          </article>
          <p v-if="!loading && !resources.length" class="resource-empty">暂无资料，先上传一个 PDF 或 Word。</p>
        </aside>

        <section class="reader-panel">
          <div class="reader-toolbar">
            <div class="reader-tool-group">
              <button type="button" title="选择" :class="{ active: tool === 'pan' }" @click="tool = 'pan'"><MousePointer2 :size="17" /></button>
              <button type="button" title="钢笔" :class="{ active: tool === 'pen' }" @click="tool = 'pen'"><PenLine :size="17" /></button>
              <button type="button" title="荧光" :class="{ active: tool === 'highlight' }" @click="tool = 'highlight'"><Highlighter :size="17" /></button>
              <button type="button" title="便签" :class="{ active: tool === 'note' }" @click="tool = 'note'"><StickyNote :size="17" /></button>
              <button type="button" title="橡皮擦" :class="{ active: tool === 'eraser' }" @click="tool = 'eraser'"><Eraser :size="17" /></button>
              <button type="button" title="撤销" @click="undoAnnotation"><Undo2 :size="17" /></button>
              <button type="button" title="保存标注" @click="saveAnnotations"><Save :size="17" /></button>
              <button type="button" title="摘录入题库" :disabled="!selectedResource" @click="openExcerptModal"><FileQuestion :size="17" /></button>
            </div>
            <div class="reader-tool-group">
              <button type="button" title="上一页" :disabled="currentPage <= 1" @click="changePage(-1)"><ChevronLeft :size="18" /></button>
              <span>{{ currentPage }} / {{ pageCount || 1 }}</span>
              <button type="button" title="下一页" :disabled="currentPage >= pageCount" @click="changePage(1)"><ChevronRight :size="18" /></button>
              <ZoomOut :size="16" />
              <input v-model.number="zoom" type="range" min="90" max="240" step="10" @change="renderSelected" />
              <ZoomIn :size="16" />
              <button type="button" :title="readerFullscreen ? '退出全屏' : '阅读全屏'" @click="toggleReaderFullscreen">
                <Minimize2 v-if="readerFullscreen" :size="17" />
                <Maximize2 v-else :size="17" />
              </button>
            </div>
            <div class="reader-colors">
              <button
                v-for="item in colors"
                :key="item"
                type="button"
                :style="{ background: item }"
                :class="{ active: color === item }"
                @click="color = item"
              ></button>
            </div>
          </div>

          <div v-if="loading" class="resource-empty reader-empty">资料加载中...</div>
          <div v-else-if="!selectedResource" class="resource-empty reader-empty">选择或上传资料后开始阅读。</div>

          <div v-else class="reader-stage" :class="`mode-${tool}`">
            <div
              ref="documentSurface"
              class="document-surface"
              :class="{ 'pdf-surface': previewMode === 'pdf' }"
              :style="surfaceStyle"
              @pointerdown="startAnnotation"
              @pointermove="moveAnnotation"
              @pointerup="finishAnnotation"
              @pointercancel="finishAnnotation"
              @pointerleave="finishAnnotation"
            >
              <canvas v-show="previewMode === 'pdf'" ref="pdfCanvas" class="pdf-canvas"></canvas>
              <div v-if="previewError" class="preview-error">
                <strong>PDF 预览失败</strong>
                <p>{{ previewError }}</p>
                <a v-if="selectedResource?.fileUrl" :href="assetUrl(selectedResource.fileUrl)" target="_blank" rel="noreferrer">打开原文件</a>
              </div>
              <div v-show="previewMode === 'docx'" class="docx-page" v-html="docxHtml"></div>
              <img v-if="previewMode === 'image'" class="image-page" :src="assetUrl(selectedResource.fileUrl)" alt="学习资料" />
              <div v-if="previewMode === 'unsupported'" class="unsupported-page">
                <span>{{ iconFor(selectedResource) }}</span>
                <h3>该文件已归档</h3>
                <p>当前类型可上传、记录进度、保存标注数据。需要查看原文时可下载后打开。</p>
                <a :href="assetUrl(selectedResource.fileUrl)" target="_blank" rel="noreferrer">打开文件</a>
              </div>

              <svg class="annotation-layer" :viewBox="`0 0 ${surfaceSize.width} ${surfaceSize.height}`" preserveAspectRatio="none">
                <template v-for="annotation in visibleAnnotations" :key="annotation.id">
                  <polyline
                    v-if="annotation.type === 'pen'"
                    :points="pointsToString(annotation.points)"
                    :stroke="annotation.color"
                    stroke-width="3"
                    fill="none"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                  <rect
                    v-else-if="annotation.type === 'highlight'"
                    :x="annotation.x"
                    :y="annotation.y"
                    :width="annotation.width"
                    :height="annotation.height"
                    :fill="annotation.color"
                    opacity="0.28"
                    rx="2"
                  />
                  <foreignObject
                    v-else-if="annotation.type === 'note'"
                    :x="annotation.x"
                    :y="annotation.y"
                    width="190"
                    height="78"
                  >
                    <div class="annotation-note" :style="{ borderColor: annotation.color }">{{ annotation.text }}</div>
                  </foreignObject>
                </template>
                <polyline
                  v-if="draftAnnotation?.type === 'pen'"
                  :points="pointsToString(draftAnnotation.points)"
                  :stroke="draftAnnotation.color"
                  stroke-width="3"
                  fill="none"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
                <rect
                  v-if="draftAnnotation?.type === 'highlight'"
                  :x="draftAnnotation.x"
                  :y="draftAnnotation.y"
                  :width="draftAnnotation.width"
                  :height="draftAnnotation.height"
                  :fill="draftAnnotation.color"
                  opacity="0.28"
                  rx="2"
                />
              </svg>
            </div>
          </div>
        </section>

        <aside class="resource-inspector">
          <section class="progress-panel">
            <span class="resource-kicker">PROGRESS</span>
            <div class="progress-orbit" :style="progressRingStyle">
              <div>
                <strong>{{ savedProgress }}%</strong>
                <span>已记录</span>
              </div>
            </div>
            <div class="progress-readout">
              <article>
                <MapPinCheck :size="18" />
                <span>当前看到</span>
                <strong>{{ currentPageText }}</strong>
              </article>
              <article>
                <BookOpenCheck :size="18" />
                <span>已学习到</span>
                <strong>{{ savedPageText }}</strong>
              </article>
            </div>
            <div class="progress-live">
              <span>按当前页计算</span>
              <b>{{ currentPageProgress }}%</b>
            </div>
            <label class="progress-session">
              <span>本次分钟</span>
              <input v-model.number="sessionMinutes" type="number" min="0" placeholder="0" />
            </label>
            <button class="resource-primary" type="button" :disabled="!selectedResource" @click="markLearnedHere">
              <CheckCircle2 :size="18" />
              <span>已学习到此</span>
            </button>
            <button class="resource-secondary" type="button" :disabled="!selectedResource" @click="openExcerptModal">
              <FileQuestion :size="18" />
              <span>摘录入题库</span>
            </button>
          </section>

          <section>
            <span class="resource-kicker">DATA</span>
            <dl>
              <div><dt>资料类型</dt><dd>{{ selectedResource?.type || '-' }}</dd></div>
              <div><dt>文件大小</dt><dd>{{ selectedResource?.sizeText || '-' }}</dd></div>
              <div><dt>标注数量</dt><dd>{{ annotations.length }}</dd></div>
              <div><dt>最后学习</dt><dd>{{ formatDateTime(selectedResource?.lastStudiedAt) }}</dd></div>
            </dl>
          </section>

          <section class="annotation-list">
            <span class="resource-kicker">NOTES</span>
            <article v-for="annotation in annotations.slice(-5).reverse()" :key="annotation.id">
              <b :style="{ background: annotation.color }"></b>
              <p>{{ annotation.type === 'note' ? annotation.text : annotationLabel(annotation) }}</p>
            </article>
            <p v-if="!annotations.length">暂无标注。</p>
          </section>
        </aside>
      </section>
    </main>

    <div v-if="excerptModalOpen" class="resource-modal-backdrop" @click.self="closeExcerptModal">
      <section class="resource-modal" role="dialog" aria-modal="true" aria-label="摘录入题库">
        <header>
          <div>
            <span class="resource-kicker">RESOURCE TO QUESTION</span>
            <h3>摘录入题库</h3>
            <p>{{ selectedResource?.name }} · 第 {{ excerptForm.sourcePage || currentPage }} 页</p>
          </div>
          <button type="button" aria-label="关闭" @click="closeExcerptModal">×</button>
        </header>
        <form class="resource-excerpt-form" @submit.prevent="submitExcerptQuestion">
          <label>
            <span>资料摘录</span>
            <textarea v-model.trim="excerptForm.sourceExcerpt" rows="4" maxlength="4000" placeholder="粘贴或整理从资料中截取的原文" />
          </label>
          <label>
            <span>题目内容 *</span>
            <textarea v-model.trim="excerptForm.content" required rows="4" maxlength="10000" placeholder="根据摘录整理题干" />
          </label>
          <div class="resource-form-grid">
            <label>
              <span>题型</span>
              <select v-model="excerptForm.questionType">
                <option value="SHORT_ANSWER">简答题</option>
                <option value="SINGLE_CHOICE">单选题</option>
                <option value="MULTIPLE_CHOICE">多选题</option>
                <option value="TRUE_FALSE">判断题</option>
                <option value="FILL_BLANK">填空题</option>
              </select>
            </label>
            <label>
              <span>难度</span>
              <select v-model.number="excerptForm.difficulty">
                <option v-for="level in 5" :key="level" :value="level">{{ level }} 星</option>
              </select>
            </label>
            <label>
              <span>页码</span>
              <input v-model.number="excerptForm.sourcePage" type="number" min="1" />
            </label>
            <label>
              <span>状态</span>
              <select v-model="excerptForm.status">
                <option value="DRAFT">草稿</option>
                <option value="PUBLISHED">已发布</option>
              </select>
            </label>
          </div>
          <div v-if="excerptIsChoice" class="resource-option-editor">
            <div>
              <span>选项</span>
              <button type="button" :disabled="excerptForm.options.length >= 8" @click="addExcerptOption">添加选项</button>
            </div>
            <label v-for="(option, index) in excerptForm.options" :key="index">
              <span>{{ String.fromCharCode(65 + index) }}</span>
              <input v-model.trim="excerptForm.options[index]" required maxlength="500" :placeholder="`选项 ${String.fromCharCode(65 + index)}`" />
              <button v-if="excerptForm.options.length > 2" type="button" aria-label="删除选项" @click="removeExcerptOption(index)">×</button>
            </label>
          </div>
          <div class="resource-form-grid">
            <label>
              <span>正确答案 *</span>
              <input v-model.trim="excerptForm.correctAnswer" required maxlength="1000" placeholder="例如：A，或简答要点" />
            </label>
            <label>
              <span>知识点</span>
              <input v-model.trim="excerptForm.knowledgePoint" maxlength="80" placeholder="例如：事务管理" />
            </label>
          </div>
          <label>
            <span>答案解析</span>
            <textarea v-model.trim="excerptForm.analysis" rows="3" maxlength="10000" placeholder="补充解题思路或资料依据" />
          </label>
          <div class="resource-plan-row">
            <label>
              <input v-model="excerptForm.createPlan" type="checkbox" />
              <span>同时加入学习计划</span>
            </label>
            <label>
              <span>计划日期</span>
              <input v-model="excerptForm.planDate" type="date" :disabled="!excerptForm.createPlan" />
            </label>
          </div>
          <p v-if="excerptError" class="resource-form-error">{{ excerptError }}</p>
          <footer>
            <button type="button" @click="closeExcerptModal">取消</button>
            <button class="resource-primary inline" type="submit" :disabled="excerptSaving">
              <FileQuestion :size="18" />
              <span>{{ excerptSaving ? '保存中...' : '加入题库' }}</span>
            </button>
          </footer>
        </form>
      </section>
    </div>

    <p v-if="resourceToast" class="resource-toast">{{ resourceToast }}</p>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, shallowRef } from 'vue'
import * as pdfjsLib from 'pdfjs-dist/legacy/build/pdf.mjs'
import pdfWorkerUrl from 'pdfjs-dist/legacy/build/pdf.worker.mjs?url'
import mammoth from 'mammoth/mammoth.browser'
import {
  BookOpenCheck,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Eraser,
  FileQuestion,
  FileText,
  FolderOpen,
  Highlighter,
  Image,
  MapPinCheck,
  Maximize2,
  Minimize2,
  MousePointer2,
  PenLine,
  RefreshCw,
  Save,
  Search,
  Star,
  StickyNote,
  Trash2,
  Undo2,
  UploadCloud,
  Video,
  Volume2,
  ZoomIn,
  ZoomOut,
} from '@lucide/vue'
import {
  createStudyPlan,
  createQuestionFromResource,
  deleteLearningResource,
  listLearningResources,
  resolveAssetUrl,
  toggleLearningResourceFavorite,
  updateLearningResourceAnnotations,
  updateLearningResourceProgress,
  uploadLearningResource,
} from '../../api'

pdfjsLib.GlobalWorkerOptions.workerSrc = pdfWorkerUrl

const emit = defineEmits(['back-home'])

const loading = ref(false)
const uploading = ref(false)
const resources = ref([])
const selectedResource = ref(null)
const readerFullscreen = ref(false)
const filters = reactive({ keyword: '', type: '' })
const tool = ref('pan')
const color = ref('#3d7cff')
const colors = ['#3d7cff', '#ff4f7b', '#ffcc3d', '#45c486', '#111827']
const currentPage = ref(1)
const pageCount = ref(1)
const zoom = ref(150)
const previewMode = ref('unsupported')
const previewError = ref('')
const docxHtml = ref('')
const annotations = ref([])
const draftAnnotation = ref(null)
const pdfDocument = shallowRef(null)
const pdfCanvas = ref(null)
const documentSurface = ref(null)
const workspaceRoot = ref(null)
const surfaceSize = reactive({ width: 900, height: 1180 })
const renderedSize = reactive({ width: 900, height: 1180 })
const progressForm = reactive({ progressPercent: 0, currentPage: 1, totalPages: 0, learnedMinutes: 0 })
const sessionMinutes = ref(0)
const excerptModalOpen = ref(false)
const excerptSaving = ref(false)
const excerptError = ref('')
const resourceToast = ref('')
const excerptForm = reactive({
  content: '',
  questionType: 'SHORT_ANSWER',
  options: ['', ''],
  correctAnswer: '',
  analysis: '',
  difficulty: 3,
  subject: '',
  knowledgePoint: '',
  status: 'DRAFT',
  categoryId: null,
  sourcePage: 1,
  sourceExcerpt: '',
  createPlan: true,
  planDate: new Date().toISOString().slice(0, 10),
})

const folders = computed(() => {
  const all = resources.value.length
  return [
    { type: '', label: '未归档笔记', icon: FolderOpen, count: all },
    { type: '文档', label: '文档资料', icon: FileText, count: countType('文档') },
    { type: '图片', label: '图像素材', icon: Image, count: countType('图片') },
    { type: '视频', label: '课程视频', icon: Video, count: countType('视频') },
    { type: '音频', label: '音频资料', icon: Volume2, count: countType('音频') },
  ]
})

const savedProgress = computed(() => clamp(progressForm.progressPercent, 0, 100))
const currentTotalPages = computed(() => Math.max(1, Number(pageCount.value || progressForm.totalPages || 1)))
const currentPageProgress = computed(() => {
  const page = clamp(currentPage.value || 1, 1, currentTotalPages.value)
  return Math.round((page / currentTotalPages.value) * 100)
})
const progressRingStyle = computed(() => ({ '--progress': `${savedProgress.value}%` }))
const currentPageText = computed(() => `${clamp(currentPage.value || 1, 1, currentTotalPages.value)} / ${currentTotalPages.value}`)
const savedPageText = computed(() => {
  const total = Math.max(1, Number(progressForm.totalPages || pageCount.value || 1))
  return `${clamp(progressForm.currentPage || 1, 1, total)} / ${total}`
})
const surfaceStyle = computed(() => {
  if (previewMode.value !== 'pdf') return {}
  return {
    width: `${Math.ceil(renderedSize.width)}px`,
    minWidth: `${Math.ceil(renderedSize.width)}px`,
    height: `${Math.ceil(renderedSize.height)}px`,
    minHeight: `${Math.ceil(renderedSize.height)}px`,
  }
})
const visibleAnnotations = computed(() =>
  annotations.value.filter((item) => Number(item.page || 1) === Number(currentPage.value)),
)
const excerptIsChoice = computed(() => ['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(excerptForm.questionType))

onMounted(() => {
  loadResources()
  document.addEventListener('fullscreenchange', handleFullscreenChange)
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})

async function toggleReaderFullscreen() {
  readerFullscreen.value = !readerFullscreen.value
  await nextTick()
  if (readerFullscreen.value) {
    try {
      await workspaceRoot.value?.requestFullscreen?.()
    } catch {
      // Browser fullscreen can be blocked; the in-page fullscreen layout still works.
    }
  } else if (document.fullscreenElement === workspaceRoot.value) {
    await document.exitFullscreen?.()
  }
  if (previewMode.value === 'pdf') await renderPdf()
}

function handleFullscreenChange() {
  if (!document.fullscreenElement && readerFullscreen.value) {
    readerFullscreen.value = false
    nextTick(() => {
      if (previewMode.value === 'pdf') renderPdf()
    })
  }
}

async function loadResources() {
  loading.value = true
  try {
    const response = await listLearningResources({
      page: 1,
      size: 80,
      keyword: filters.keyword || undefined,
      type: filters.type || undefined,
    })
    resources.value = response.data?.data?.records || []
    if (!selectedResource.value && resources.value.length) {
      await selectResource(resources.value[0])
    } else if (selectedResource.value) {
      const fresh = resources.value.find((item) => item.id === selectedResource.value.id)
      if (fresh) selectedResource.value = fresh
    }
  } finally {
    loading.value = false
  }
}

function reloadFirstPage() {
  selectedResource.value = null
  loadResources()
}

function setType(type) {
  filters.type = type
  reloadFirstPage()
}

async function handleUpload(event) {
  const [file] = event.target.files || []
  if (!file) return
  uploading.value = true
  try {
    const response = await uploadLearningResource(file, { name: file.name })
    const uploaded = response.data?.data
    await loadResources()
    const target = resources.value.find((item) => item.id === uploaded?.id) || uploaded
    if (target) await selectResource(target)
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

async function selectResource(resource) {
  selectedResource.value = resource
  annotations.value = Array.isArray(resource.annotations) ? [...resource.annotations] : []
  currentPage.value = Math.max(1, resource.currentPage || 1)
  pageCount.value = Math.max(1, resource.totalPages || 1)
  progressForm.progressPercent = resource.progressPercent || 0
  progressForm.currentPage = currentPage.value
  progressForm.totalPages = resource.totalPages || 0
  progressForm.learnedMinutes = resource.learnedMinutes || 0
  sessionMinutes.value = 0
  await renderSelected()
}

async function renderSelected() {
  if (!selectedResource.value) return
  previewError.value = ''
  docxHtml.value = ''
  pdfDocument.value = null
  const filename = (selectedResource.value.originalFilename || '').toLowerCase()
  await nextTick()
  if (filename.endsWith('.pdf')) {
    previewMode.value = 'pdf'
    await renderPdf()
  } else if (filename.endsWith('.docx')) {
    previewMode.value = 'docx'
    await renderDocx()
  } else if (/\.(png|jpg|jpeg|webp|gif)$/.test(filename)) {
    previewMode.value = 'image'
    await nextTick()
    updateSurfaceSize()
  } else {
    previewMode.value = 'unsupported'
    await nextTick()
    updateSurfaceSize()
  }
}

async function renderPdf() {
  try {
    let document = pdfDocument.value
    if (!document) {
      const url = assetUrl(selectedResource.value.fileUrl)
      const response = await fetch(url)
      if (!response.ok) {
        throw new Error(`PDF request failed: ${response.status}`)
      }
      const data = new Uint8Array(await response.arrayBuffer())
      const loadingTask = pdfjsLib.getDocument({ data })
      document = await loadingTask.promise
      pdfDocument.value = document
    }
    pageCount.value = document.numPages
    currentPage.value = Math.min(Math.max(1, currentPage.value), pageCount.value)
    progressForm.totalPages = pageCount.value
    const page = await document.getPage(currentPage.value)
    const baseViewport = page.getViewport({ scale: 1 })
    const viewport = page.getViewport({ scale: zoom.value / 100 })
    const canvas = pdfCanvas.value
    if (!canvas) return
    canvas.width = Math.ceil(viewport.width)
    canvas.height = Math.ceil(viewport.height)
    canvas.style.width = `${Math.ceil(viewport.width)}px`
    canvas.style.height = `${Math.ceil(viewport.height)}px`
    await page.render({ canvas, viewport, background: '#ffffff' }).promise
    surfaceSize.width = baseViewport.width
    surfaceSize.height = baseViewport.height
    renderedSize.width = viewport.width
    renderedSize.height = viewport.height
    normalizeCurrentPageAnnotations(viewport.width / baseViewport.width)
    previewError.value = ''
  } catch (error) {
    console.error(error)
    previewError.value = `文件已读取，但浏览器没有成功绘制页面：${error?.message || '未知错误'}`
    await nextTick()
    updateSurfaceSize()
  }
}

async function renderDocx() {
  const response = await fetch(assetUrl(selectedResource.value.fileUrl))
  const arrayBuffer = await response.arrayBuffer()
  const result = await mammoth.convertToHtml({ arrayBuffer })
  docxHtml.value = result.value || '<p>文档暂无可预览内容。</p>'
  await nextTick()
  updateSurfaceSize()
}

function updateSurfaceSize() {
  const rect = documentSurface.value?.getBoundingClientRect()
  surfaceSize.width = Math.max(1, Math.round(rect?.width || 900))
  surfaceSize.height = Math.max(1, Math.round(rect?.height || 1180))
  renderedSize.width = surfaceSize.width
  renderedSize.height = surfaceSize.height
}

async function changePage(delta) {
  const next = Math.min(Math.max(1, currentPage.value + delta), pageCount.value || 1)
  if (next === currentPage.value) return
  currentPage.value = next
  if (previewMode.value === 'pdf') await renderPdf()
}

function startAnnotation(event) {
  if (tool.value === 'pan' || !selectedResource.value) return
  event.preventDefault()
  const point = pointerPoint(event)
  if (tool.value === 'eraser') {
    eraseAt(point)
    return
  }
  const base = {
    id: `ann-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    type: tool.value,
    color: color.value,
    page: currentPage.value,
    coordinateSpace: 'page',
    zoom: zoom.value,
    createdAt: new Date().toISOString(),
  }
  if (tool.value === 'pen') {
    draftAnnotation.value = { ...base, points: [point] }
  } else if (tool.value === 'highlight') {
    draftAnnotation.value = { ...base, startX: point.x, startY: point.y, x: point.x, y: point.y, width: 1, height: 1 }
  } else if (tool.value === 'note') {
    const text = window.prompt('便签内容')
    if (text) {
      annotations.value.push({ ...base, x: point.x, y: point.y, text })
    }
  }
}

function moveAnnotation(event) {
  if (tool.value === 'eraser' && selectedResource.value) {
    event.preventDefault()
    eraseAt(pointerPoint(event))
    return
  }
  if (!draftAnnotation.value) return
  const point = pointerPoint(event)
  if (draftAnnotation.value.type === 'pen') {
    draftAnnotation.value.points.push(point)
  } else if (draftAnnotation.value.type === 'highlight') {
    const startX = draftAnnotation.value.startX
    const startY = draftAnnotation.value.startY
    draftAnnotation.value.x = Math.min(startX, point.x)
    draftAnnotation.value.y = Math.min(startY, point.y)
    draftAnnotation.value.width = Math.abs(point.x - startX)
    draftAnnotation.value.height = Math.abs(point.y - startY)
  }
}

function finishAnnotation() {
  if (!draftAnnotation.value) return
  const next = { ...draftAnnotation.value }
  delete next.startX
  delete next.startY
  annotations.value.push(next)
  draftAnnotation.value = null
}

function pointerPoint(event) {
  const rect = documentSurface.value.getBoundingClientRect()
  return {
    x: Math.max(0, Math.min(surfaceSize.width, ((event.clientX - rect.left) / rect.width) * surfaceSize.width)),
    y: Math.max(0, Math.min(surfaceSize.height, ((event.clientY - rect.top) / rect.height) * surfaceSize.height)),
  }
}

function normalizeCurrentPageAnnotations(currentScale = 1) {
  const page = Number(currentPage.value || 1)
  annotations.value = annotations.value.map((annotation) => {
    if (Number(annotation.page || 1) !== page || annotation.coordinateSpace === 'page') return annotation
    const ratio = Number(annotation.zoom || 0) ? Number(annotation.zoom || 100) / 100 : currentScale
    if (!ratio || ratio === 1) return { ...annotation, coordinateSpace: 'page' }
    return scaleAnnotation(annotation, 1 / ratio)
  })
}

function scaleAnnotation(annotation, factor) {
  const next = { ...annotation, coordinateSpace: 'page' }
  if (annotation.type === 'pen') {
    next.points = (annotation.points || []).map((point) => ({
      x: Number(point.x || 0) * factor,
      y: Number(point.y || 0) * factor,
    }))
  } else {
    next.x = Number(annotation.x || 0) * factor
    next.y = Number(annotation.y || 0) * factor
    if ('width' in annotation) next.width = Number(annotation.width || 0) * factor
    if ('height' in annotation) next.height = Number(annotation.height || 0) * factor
  }
  return next
}

function undoAnnotation() {
  annotations.value.pop()
}

function eraseAt(point) {
  const radius = 22
  const before = annotations.value.length
  annotations.value = annotations.value.filter((annotation) => {
    if (Number(annotation.page || 1) !== Number(currentPage.value)) return true
    return !hitAnnotation(annotation, point, radius)
  })
  if (before !== annotations.value.length) {
    draftAnnotation.value = null
  }
}

function hitAnnotation(annotation, point, radius) {
  if (annotation.type === 'pen') {
    return (annotation.points || []).some((item) => distance(item, point) <= radius)
  }
  if (annotation.type === 'highlight') {
    return hitRect(annotation, point, radius)
  }
  if (annotation.type === 'note') {
    return hitRect({ x: annotation.x, y: annotation.y, width: 190, height: 78 }, point, radius)
  }
  return false
}

function hitRect(rect, point, radius) {
  const left = Math.min(rect.x, rect.x + rect.width) - radius
  const right = Math.max(rect.x, rect.x + rect.width) + radius
  const top = Math.min(rect.y, rect.y + rect.height) - radius
  const bottom = Math.max(rect.y, rect.y + rect.height) + radius
  return point.x >= left && point.x <= right && point.y >= top && point.y <= bottom
}

function distance(first, second) {
  return Math.hypot(Number(first.x) - Number(second.x), Number(first.y) - Number(second.y))
}

async function saveAnnotations() {
  if (!selectedResource.value) return
  const response = await updateLearningResourceAnnotations(selectedResource.value.id, annotations.value)
  selectedResource.value = response.data?.data || selectedResource.value
}

function openExcerptModal() {
  if (!selectedResource.value) return
  const excerpt = selectedText() || latestNoteText() || ''
  excerptForm.sourcePage = Math.max(1, Number(currentPage.value || 1))
  excerptForm.sourceExcerpt = excerpt
  excerptForm.content = excerpt ? `根据资料摘录回答：\n${excerpt}` : ''
  excerptForm.questionType = 'SHORT_ANSWER'
  excerptForm.options = ['', '']
  excerptForm.correctAnswer = ''
  excerptForm.analysis = ''
  excerptForm.difficulty = 3
  excerptForm.subject = selectedResource.value.type || ''
  excerptForm.knowledgePoint = ''
  excerptForm.status = 'DRAFT'
  excerptForm.categoryId = null
  excerptForm.createPlan = true
  excerptForm.planDate = new Date().toISOString().slice(0, 10)
  excerptError.value = ''
  excerptModalOpen.value = true
}

function closeExcerptModal() {
  if (excerptSaving.value) return
  excerptModalOpen.value = false
  excerptError.value = ''
}

function addExcerptOption() {
  if (excerptForm.options.length < 8) excerptForm.options.push('')
}

function removeExcerptOption(index) {
  if (excerptForm.options.length > 2) excerptForm.options.splice(index, 1)
}

async function submitExcerptQuestion() {
  if (!selectedResource.value) return
  if (excerptIsChoice.value && excerptForm.options.filter(Boolean).length < 2) {
    excerptError.value = '选择题至少需要两个有效选项'
    return
  }
  excerptSaving.value = true
  excerptError.value = ''
  try {
    const payload = {
      content: excerptForm.content,
      questionType: excerptForm.questionType,
      options: excerptIsChoice.value ? excerptForm.options.filter(Boolean) : [],
      correctAnswer: excerptForm.correctAnswer,
      analysis: excerptForm.analysis || null,
      difficulty: excerptForm.difficulty,
      subject: excerptForm.subject || null,
      knowledgePoint: excerptForm.knowledgePoint || null,
      status: excerptForm.status,
      categoryId: excerptForm.categoryId,
      sourcePage: excerptForm.sourcePage || currentPage.value || 1,
      sourceExcerpt: excerptForm.sourceExcerpt || null,
    }
    const response = await createQuestionFromResource(selectedResource.value.id, payload)
    const createdQuestion = response.data?.data
    if (excerptForm.createPlan) {
      await createStudyPlan({
        title: `复习资料题：${excerptForm.content.replace(/\s+/g, ' ').trim().slice(0, 42)}`,
        content: [
          `从资料《${selectedResource.value.name}》第 ${payload.sourcePage} 页生成。`,
          payload.sourceExcerpt ? `资料摘录：${payload.sourceExcerpt}` : '',
          `正确答案：${payload.correctAnswer}`,
        ].filter(Boolean).join('\n'),
        planDate: excerptForm.planDate || new Date().toISOString().slice(0, 10),
        targetType: 'QUESTION',
        targetId: createdQuestion?.id || null,
        targetTitle: createdQuestion?.content?.slice?.(0, 120) || excerptForm.content.slice(0, 120),
      })
    }
    excerptModalOpen.value = false
    showToast(excerptForm.createPlan ? '已加入题库，并同步生成学习计划' : '已加入题库，并保留资料来源')
  } catch (error) {
    excerptError.value = error.response?.data?.message || '题目保存失败'
  } finally {
    excerptSaving.value = false
  }
}

async function markLearnedHere() {
  if (!selectedResource.value) return
  const totalPages = Math.max(0, Number(pageCount.value || progressForm.totalPages || 0))
  const current = totalPages ? clamp(currentPage.value || 1, 1, totalPages) : Math.max(1, currentPage.value || 1)
  const payload = {
    progressPercent: totalPages ? Math.round((current / totalPages) * 100) : 0,
    currentPage: current,
    totalPages,
    learnedMinutes: Math.max(
      0,
      Number(progressForm.learnedMinutes || selectedResource.value.learnedMinutes || 0) + Number(sessionMinutes.value || 0),
    ),
  }
  const response = await updateLearningResourceProgress(selectedResource.value.id, payload)
  selectedResource.value = response.data?.data || selectedResource.value
  progressForm.progressPercent = payload.progressPercent
  progressForm.currentPage = payload.currentPage
  progressForm.totalPages = payload.totalPages
  progressForm.learnedMinutes = payload.learnedMinutes
  sessionMinutes.value = 0
  await loadResources()
}

function selectedText() {
  const text = window.getSelection?.()?.toString?.().trim()
  return text && text.length >= 2 ? text.slice(0, 4000) : ''
}

function latestNoteText() {
  const note = [...annotations.value]
    .reverse()
    .find((item) => item.type === 'note' && Number(item.page || 1) === Number(currentPage.value) && item.text)
  return note?.text?.trim?.().slice(0, 4000) || ''
}

function showToast(message) {
  resourceToast.value = message
  window.setTimeout(() => {
    if (resourceToast.value === message) resourceToast.value = ''
  }, 2600)
}

async function toggleFavorite(resource) {
  const response = await toggleLearningResourceFavorite(resource.id)
  const updated = response.data?.data
  resources.value = resources.value.map((item) => (item.id === updated.id ? updated : item))
  if (selectedResource.value?.id === updated.id) selectedResource.value = updated
}

async function removeResource(resource) {
  await deleteLearningResource(resource.id)
  if (selectedResource.value?.id === resource.id) selectedResource.value = null
  await loadResources()
}

function countType(type) {
  return resources.value.filter((item) => item.type === type).length
}

function assetUrl(url) {
  return resolveAssetUrl(url)
}

function iconFor(resource) {
  const filename = (resource?.originalFilename || resource?.name || '').toLowerCase()
  if (filename.endsWith('.pdf')) return 'PDF'
  if (filename.endsWith('.doc') || filename.endsWith('.docx')) return 'DOC'
  if (resource?.type === '图片') return 'IMG'
  if (resource?.type === '视频') return 'VID'
  if (resource?.type === '音频') return 'AUD'
  return 'FILE'
}

function fileIconFor(resource) {
  if (resource?.type === '图片') return Image
  if (resource?.type === '视频') return Video
  if (resource?.type === '音频') return Volume2
  return FileText
}

function displayProgress(resource) {
  return clamp(resource?.progressPercent || 0, 0, 100)
}

function toneFor(resource) {
  return {
    文档: 'blue',
    图片: 'green',
    视频: 'violet',
    音频: 'pink',
  }[resource?.type] || 'blue'
}

function pointsToString(points = []) {
  return points.map((point) => `${point.x},${point.y}`).join(' ')
}

function annotationLabel(annotation) {
  return annotation.type === 'pen' ? '手写标记' : annotation.type === 'highlight' ? '重点高亮' : '资料标注'
}

function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

function clamp(value, min, max) {
  return Math.min(Math.max(Number(value) || 0, min), max)
}
</script>

<style scoped>
.resource-workspace {
  --resource-blue: #4f88ff;
  --resource-cyan: #6fd5ff;
  --resource-pink: #f0a6df;
  --resource-gold: #edc767;
  --resource-green: #5acb99;
  --resource-ink: #1e2c54;
  --resource-muted: #6f7da3;
  --resource-line: rgba(73, 116, 221, 0.18);
  --resource-glass: rgba(255, 255, 255, 0.7);
  display: grid;
  grid-template-columns: 206px minmax(0, 1fr);
  min-height: 100vh;
  color: var(--resource-ink);
  background:
    linear-gradient(118deg, rgba(79, 136, 255, 0.08), transparent 34%),
    linear-gradient(240deg, rgba(240, 166, 223, 0.13), transparent 42%),
    url("/images/resource-eva-bg.png") top right / 760px auto no-repeat,
    #f7faff;
}

.resource-workspace.fullscreen {
  position: fixed;
  inset: 0;
  z-index: 1000;
  grid-template-columns: 1fr;
  min-height: 100vh;
  overflow: hidden;
  background: #f6f9ff;
}

.resource-workspace.fullscreen .resource-rail,
.resource-workspace.fullscreen .resource-topbar,
.resource-workspace.fullscreen .resource-list,
.resource-workspace.fullscreen .resource-inspector {
  display: none;
}

.resource-workspace.fullscreen .resource-main {
  padding: 0;
}

.resource-workspace.fullscreen .resource-grid-shell {
  grid-template-columns: 1fr;
  height: 100vh;
  min-height: 0;
  gap: 0;
}

.resource-workspace.fullscreen .reader-panel {
  box-shadow: none;
}

.resource-workspace.fullscreen .reader-toolbar {
  position: sticky;
  top: 0;
  z-index: 5;
}

.resource-workspace.fullscreen .reader-stage {
  padding: 24px 40px 48px;
}

.resource-workspace::before {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  content: "";
  background:
    linear-gradient(90deg, rgba(79, 136, 255, 0.07) 1px, transparent 1px),
    linear-gradient(180deg, rgba(79, 136, 255, 0.05) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: linear-gradient(90deg, rgba(0, 0, 0, 0.5), transparent 78%);
}

.resource-rail,
.resource-main {
  position: relative;
  z-index: 1;
}

.resource-rail {
  display: grid;
  align-content: start;
  gap: 18px;
  padding: 18px 12px;
  border-right: 1px solid rgba(73, 116, 221, 0.14);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(242, 247, 255, 0.56));
  backdrop-filter: blur(16px);
}

.resource-back,
.resource-folders button,
.resource-upload,
.resource-actions button,
.reader-toolbar button,
.resource-primary {
  cursor: pointer;
  border: 0;
  border-radius: 6px;
  font-weight: 900;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.resource-back {
  display: flex;
  min-height: 40px;
  align-items: center;
  gap: 10px;
  padding: 0 10px;
  color: var(--resource-blue);
  background: rgba(255, 255, 255, 0.68);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.13);
}

.resource-back:hover,
.resource-actions button:hover,
.reader-toolbar button:hover,
.resource-row-actions button:hover {
  transform: translateY(-1px);
}

.resource-brand small,
.resource-kicker {
  color: var(--resource-muted);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0;
}

.resource-brand {
  padding: 2px 4px 8px;
}

.resource-brand h1 {
  margin: 10px 0 8px;
  color: var(--resource-blue);
  font-size: 34px;
  line-height: 1;
}

.resource-brand p {
  margin: 0;
  color: var(--resource-muted);
  font-size: 13px;
  font-weight: 800;
  line-height: 1.7;
}

.resource-folders {
  display: grid;
  gap: 6px;
}

.resource-folders button {
  position: relative;
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) 30px;
  min-height: 44px;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  overflow: hidden;
  color: var(--resource-muted);
  text-align: left;
  background: transparent;
}

.resource-folders button::before {
  position: absolute;
  inset: 5px 0;
  z-index: -1;
  content: "";
  border-left: 3px solid transparent;
  background: rgba(255, 255, 255, 0.54);
  transform: skewX(-7deg);
}

.resource-folders button.active {
  color: #fff;
}

.resource-folders button.active::before {
  border-left-color: var(--resource-gold);
  background: linear-gradient(100deg, var(--resource-blue), var(--resource-cyan) 56%, var(--resource-pink));
  box-shadow: 0 12px 24px rgba(79, 136, 255, 0.2);
}

.folder-icon {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  background: rgba(79, 136, 255, 0.1);
}

.resource-folders button.active .folder-icon {
  color: var(--resource-blue);
  background: rgba(255, 255, 255, 0.9);
}

.resource-folders b {
  justify-self: end;
  font-size: 12px;
}

.resource-upload {
  display: flex;
  min-height: 52px;
  align-items: center;
  justify-content: center;
  gap: 9px;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(100deg, var(--resource-blue), var(--resource-cyan) 62%, var(--resource-pink));
  box-shadow: 0 16px 30px rgba(79, 136, 255, 0.22);
}

.resource-upload input {
  display: none;
}

.resource-main {
  min-width: 0;
  padding: 20px;
}

.resource-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 16px;
}

.resource-topbar h2 {
  max-width: 760px;
  margin: 4px 0 0;
  overflow: hidden;
  font-size: clamp(26px, 3vw, 40px);
  line-height: 1.1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.resource-search {
  display: flex;
  min-height: 40px;
  width: min(280px, 30vw);
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  color: var(--resource-muted);
  background: rgba(255, 255, 255, 0.78);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.13);
}

.resource-actions input {
  min-width: 0;
  width: 100%;
  color: var(--resource-ink);
  border: 0;
  outline: none;
  background: transparent;
  font-weight: 800;
}

.resource-actions button {
  display: flex;
  min-height: 40px;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  color: var(--resource-blue);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.13);
}

.resource-grid-shell {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr) 270px;
  gap: 14px;
  height: calc(100vh - 116px);
  min-height: 680px;
}

.resource-list,
.reader-panel,
.resource-inspector {
  min-width: 0;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.74), rgba(247, 251, 255, 0.54)),
    repeating-linear-gradient(135deg, rgba(79, 136, 255, 0.025) 0 1px, transparent 1px 11px);
  box-shadow:
    inset 0 0 0 1px rgba(73, 116, 221, 0.13),
    0 20px 46px rgba(74, 96, 155, 0.1);
  backdrop-filter: blur(14px);
}

.resource-list {
  display: grid;
  align-content: start;
  gap: 8px;
  padding: 10px;
  overflow: auto;
}

.resource-list article {
  position: relative;
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) 38px;
  gap: 10px;
  align-items: center;
  min-height: 86px;
  padding: 10px 8px 10px 10px;
  overflow: hidden;
  cursor: pointer;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.76), rgba(255, 255, 255, 0.32));
}

.resource-list article::before {
  position: absolute;
  inset: 8px auto 8px 0;
  width: 3px;
  content: "";
  background: transparent;
}

.resource-list article.active,
.resource-list article:hover {
  background: linear-gradient(90deg, rgba(232, 240, 255, 0.96), rgba(255, 255, 255, 0.46));
  box-shadow: 0 12px 24px rgba(79, 136, 255, 0.1);
}

.resource-list article.active::before,
.resource-list article:hover::before {
  background: linear-gradient(180deg, var(--resource-blue), var(--resource-pink));
}

.resource-file-mark {
  display: grid;
  width: 46px;
  height: 58px;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, var(--resource-blue), var(--resource-cyan) 60%, var(--resource-pink));
  clip-path: polygon(0 0, 74% 0, 100% 23%, 100% 100%, 0 100%);
  box-shadow: 0 12px 24px rgba(79, 136, 255, 0.18);
}

.resource-file-mark em {
  font-size: 9px;
  font-style: normal;
  font-weight: 900;
}

.resource-file-mark.green {
  background: linear-gradient(135deg, var(--resource-green), var(--resource-cyan));
}

.resource-file-mark.violet {
  background: linear-gradient(135deg, #8d80ff, var(--resource-pink));
}

.resource-file-mark.pink {
  background: linear-gradient(135deg, #ff79ad, var(--resource-gold));
}

.resource-list h3 {
  min-width: 0;
  margin: 0 0 6px;
  overflow: hidden;
  color: var(--resource-ink);
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-list p {
  margin: 0 0 8px;
  color: var(--resource-muted);
  font-size: 12px;
  font-weight: 800;
}

.resource-list i {
  display: block;
  height: 4px;
  overflow: hidden;
  background: rgba(79, 136, 255, 0.12);
}

.resource-list i b {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, var(--resource-blue), var(--resource-cyan), var(--resource-gold));
}

.resource-row-actions {
  display: grid;
  justify-items: end;
  gap: 4px;
}

.resource-row-actions strong {
  color: var(--resource-blue);
  font-size: 12px;
  line-height: 1;
}

.resource-row-actions button {
  display: grid;
  width: 28px;
  height: 26px;
  place-items: center;
  padding: 0;
  color: var(--resource-blue);
  background: rgba(255, 255, 255, 0.64);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.12);
}

.reader-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
}

.reader-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  padding: 10px;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.86), rgba(255, 255, 255, 0.52)),
    linear-gradient(90deg, rgba(79, 136, 255, 0.18), transparent 34%);
  box-shadow: inset 0 -1px 0 rgba(73, 116, 221, 0.12);
}

.reader-tool-group,
.reader-colors {
  display: flex;
  align-items: center;
  gap: 6px;
}

.reader-tool-group {
  min-height: 38px;
  padding: 3px;
  background: rgba(255, 255, 255, 0.58);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.1);
}

.reader-toolbar button {
  display: grid;
  width: 34px;
  height: 32px;
  place-items: center;
  padding: 0;
  color: var(--resource-blue);
  background: transparent;
}

.reader-toolbar button:disabled {
  cursor: not-allowed;
  opacity: 0.38;
  transform: none;
}

.reader-toolbar button.active {
  color: #fff;
  background: linear-gradient(100deg, var(--resource-blue), var(--resource-cyan), var(--resource-pink));
  box-shadow: 0 8px 18px rgba(79, 136, 255, 0.18);
}

.reader-toolbar span {
  min-width: 58px;
  color: var(--resource-muted);
  font-size: 12px;
  font-weight: 900;
  text-align: center;
}

.reader-toolbar input[type="range"] {
  width: 96px;
  accent-color: var(--resource-blue);
}

.reader-colors button {
  width: 23px;
  height: 23px;
  min-height: 23px;
  padding: 0;
  border-radius: 50%;
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.9);
}

.reader-colors button.active {
  box-shadow:
    inset 0 0 0 2px rgba(255, 255, 255, 0.95),
    0 0 0 3px rgba(79, 136, 255, 0.17);
}

.reader-stage {
  position: relative;
  min-height: 0;
  overflow: auto;
  padding: 20px;
  background:
    linear-gradient(180deg, rgba(247, 250, 255, 0.62), rgba(238, 244, 255, 0.38)),
    radial-gradient(circle at 50% 0, rgba(111, 213, 255, 0.12), transparent 36%);
}

.document-surface {
  position: relative;
  width: fit-content;
  min-width: min(100%, 860px);
  min-height: 780px;
  margin: 0 auto;
  background: #fff;
  box-shadow:
    0 28px 56px rgba(56, 75, 130, 0.18),
    0 0 0 1px rgba(73, 116, 221, 0.1);
}

.document-surface.pdf-surface {
  width: auto;
  max-width: none;
}

.document-surface::before {
  position: absolute;
  inset: -8px -8px auto auto;
  width: 84px;
  height: 26px;
  pointer-events: none;
  content: "";
  background: linear-gradient(90deg, transparent, rgba(237, 199, 103, 0.9));
  clip-path: polygon(18% 0, 100% 0, 82% 100%, 0 100%);
}

.mode-pen .document-surface,
.mode-highlight .document-surface,
.mode-note .document-surface,
.mode-eraser .document-surface {
  cursor: crosshair;
}

.pdf-canvas,
.image-page {
  display: block;
  max-width: none;
}

.docx-page {
  width: min(760px, 72vw);
  min-height: 960px;
  padding: 56px 64px;
  color: #202a44;
  font-size: 16px;
  line-height: 1.75;
}

.docx-page :deep(p) {
  margin: 0 0 1em;
}

.image-page {
  max-width: 920px;
}

.unsupported-page {
  display: grid;
  min-width: min(680px, 72vw);
  min-height: 520px;
  place-items: center;
  align-content: center;
  gap: 12px;
  padding: 40px;
  text-align: center;
}

.unsupported-page span {
  color: var(--resource-blue);
  font-size: 58px;
  font-weight: 900;
}

.unsupported-page h3,
.unsupported-page p {
  margin: 0;
}

.unsupported-page p {
  max-width: 460px;
  color: var(--resource-muted);
  font-weight: 800;
  line-height: 1.7;
}

.unsupported-page a {
  display: inline-grid;
  min-height: 38px;
  padding: 0 16px;
  place-items: center;
  color: #fff;
  text-decoration: none;
  background: linear-gradient(100deg, var(--resource-blue), var(--resource-cyan), var(--resource-pink));
}

.annotation-layer {
  position: absolute;
  inset: 0;
  z-index: 2;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.annotation-note {
  padding: 8px;
  color: var(--resource-ink);
  border-left: 4px solid;
  background: rgba(255, 255, 255, 0.9);
  font-size: 12px;
  font-weight: 900;
}

.resource-inspector {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 12px;
  overflow: auto;
}

.resource-inspector section {
  display: grid;
  gap: 12px;
  padding: 4px 2px 14px;
  border-bottom: 1px solid rgba(73, 116, 221, 0.12);
}

.resource-inspector section:last-child {
  border-bottom: 0;
}

.progress-panel {
  justify-items: center;
}

.progress-panel > .resource-kicker {
  justify-self: start;
}

.progress-orbit {
  display: grid;
  width: 138px;
  height: 138px;
  place-items: center;
  border-radius: 50%;
  background:
    conic-gradient(var(--resource-blue) var(--progress), rgba(79, 136, 255, 0.12) 0),
    linear-gradient(135deg, rgba(111, 213, 255, 0.45), rgba(240, 166, 223, 0.34));
  box-shadow:
    0 18px 36px rgba(79, 136, 255, 0.18),
    inset 0 0 0 1px rgba(255, 255, 255, 0.72);
}

.progress-orbit > div {
  display: grid;
  width: 104px;
  height: 104px;
  place-items: center;
  align-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.09);
}

.progress-orbit strong {
  color: var(--resource-blue);
  font-size: 36px;
  line-height: 1;
}

.progress-orbit span,
.progress-live span,
.progress-session span,
.progress-readout span {
  color: var(--resource-muted);
  font-size: 12px;
  font-weight: 900;
}

.progress-readout {
  display: grid;
  width: 100%;
  gap: 8px;
}

.progress-readout article {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  color: var(--resource-blue);
  border-bottom: 1px dashed rgba(73, 116, 221, 0.16);
}

.progress-readout strong,
.progress-live b {
  color: var(--resource-ink);
  font-size: 13px;
  line-height: 1;
}

.progress-live,
.progress-session {
  display: flex;
  width: 100%;
  min-height: 38px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 0 10px;
  background: rgba(255, 255, 255, 0.58);
  box-shadow: inset 3px 0 0 var(--resource-gold);
}

.progress-session input {
  width: 86px;
  min-height: 30px;
  padding: 0 8px;
  color: var(--resource-ink);
  border: 0;
  outline: none;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.12);
  font-weight: 900;
  text-align: right;
}

.resource-primary {
  display: flex;
  min-height: 44px;
  width: 100%;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  background: linear-gradient(100deg, var(--resource-blue), var(--resource-cyan), var(--resource-pink));
  box-shadow: 0 14px 28px rgba(79, 136, 255, 0.2);
}

.resource-secondary {
  display: flex;
  min-height: 42px;
  width: 100%;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  color: var(--resource-blue);
  border: 0;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.68);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.14);
  font-weight: 900;
}

.resource-primary:disabled {
  cursor: not-allowed;
  filter: grayscale(0.35);
  opacity: 0.55;
}

.resource-primary.inline {
  width: auto;
  min-width: 132px;
  padding: 0 18px;
}

.resource-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1100;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(20, 34, 70, 0.32);
  backdrop-filter: blur(10px);
}

.resource-modal {
  width: min(760px, 100%);
  max-height: min(92vh, 860px);
  overflow: auto;
  color: var(--resource-ink);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 251, 255, 0.94)),
    repeating-linear-gradient(135deg, rgba(79, 136, 255, 0.028) 0 1px, transparent 1px 11px);
  box-shadow:
    0 26px 70px rgba(38, 58, 118, 0.25),
    inset 0 0 0 1px rgba(73, 116, 221, 0.16);
}

.resource-modal header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px 14px;
  border-bottom: 1px solid rgba(73, 116, 221, 0.12);
}

.resource-modal h3,
.resource-modal p {
  margin: 0;
}

.resource-modal h3 {
  margin-top: 4px;
  font-size: 24px;
}

.resource-modal p {
  margin-top: 6px;
  color: var(--resource-muted);
  font-size: 13px;
  font-weight: 800;
}

.resource-modal header > button,
.resource-option-editor label > button {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  cursor: pointer;
  color: var(--resource-blue);
  border: 0;
  border-radius: 6px;
  background: rgba(79, 136, 255, 0.08);
  font-size: 18px;
  font-weight: 900;
}

.resource-excerpt-form {
  display: grid;
  gap: 14px;
  padding: 18px 22px 22px;
}

.resource-excerpt-form label {
  display: grid;
  gap: 7px;
}

.resource-excerpt-form label > span,
.resource-option-editor > div span {
  color: var(--resource-muted);
  font-size: 12px;
  font-weight: 900;
}

.resource-excerpt-form input,
.resource-excerpt-form select,
.resource-excerpt-form textarea {
  width: 100%;
  min-width: 0;
  border: 0;
  border-radius: 6px;
  outline: none;
  color: var(--resource-ink);
  background: rgba(255, 255, 255, 0.86);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.14);
  font: inherit;
  font-weight: 800;
}

.resource-excerpt-form input,
.resource-excerpt-form select {
  min-height: 40px;
  padding: 0 11px;
}

.resource-excerpt-form textarea {
  resize: vertical;
  padding: 11px;
  line-height: 1.6;
}

.resource-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.resource-plan-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  gap: 12px;
  align-items: end;
  padding: 12px;
  border-radius: 6px;
  background: rgba(79, 136, 255, 0.06);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.1);
}

.resource-plan-row label:first-child {
  grid-template-columns: 18px minmax(0, 1fr);
  align-items: center;
}

.resource-plan-row input[type="checkbox"] {
  width: 18px;
  height: 18px;
  min-height: 18px;
  padding: 0;
  accent-color: var(--resource-blue);
}

.resource-option-editor {
  display: grid;
  gap: 9px;
  padding: 12px;
  border-radius: 6px;
  background: rgba(79, 136, 255, 0.06);
}

.resource-option-editor > div,
.resource-excerpt-form footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.resource-option-editor > div button,
.resource-excerpt-form footer > button {
  min-height: 38px;
  cursor: pointer;
  border: 0;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.14);
  color: var(--resource-blue);
  font-weight: 900;
}

.resource-excerpt-form footer > .resource-primary {
  color: #fff;
  background: linear-gradient(100deg, var(--resource-blue), var(--resource-cyan), var(--resource-pink));
  box-shadow: 0 14px 28px rgba(79, 136, 255, 0.2);
}

.resource-option-editor > div button {
  padding: 0 12px;
}

.resource-option-editor label {
  grid-template-columns: 24px minmax(0, 1fr) 32px;
  align-items: center;
  gap: 8px;
}

.resource-option-editor label > span {
  color: var(--resource-blue);
  text-align: center;
}

.resource-form-error {
  margin: 0;
  color: #d9415f;
  font-size: 13px;
  font-weight: 900;
}

.resource-toast {
  position: fixed;
  right: 22px;
  bottom: 22px;
  z-index: 1200;
  margin: 0;
  padding: 12px 16px;
  color: #fff;
  border-radius: 6px;
  background: linear-gradient(100deg, var(--resource-blue), var(--resource-cyan), var(--resource-pink));
  box-shadow: 0 18px 38px rgba(79, 136, 255, 0.24);
  font-weight: 900;
}

.resource-inspector dl {
  display: grid;
  gap: 8px;
  margin: 0;
}

.resource-inspector dl div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(73, 116, 221, 0.1);
}

.resource-inspector dt,
.resource-inspector dd {
  margin: 0;
  color: var(--resource-muted);
  font-size: 12px;
  font-weight: 900;
}

.resource-inspector dd {
  color: var(--resource-ink);
  text-align: right;
}

.annotation-list article {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr);
  gap: 8px;
  align-items: start;
  padding: 6px 0;
}

.annotation-list b {
  width: 10px;
  height: 10px;
  margin-top: 4px;
}

.annotation-list p {
  margin: 0;
  color: var(--resource-muted);
  font-size: 12px;
  font-weight: 800;
  line-height: 1.55;
}

.resource-empty {
  margin: 0;
  padding: 18px;
  color: var(--resource-muted);
  border: 1px dashed var(--resource-line);
  background: rgba(255, 255, 255, 0.46);
  font-weight: 900;
}

.reader-empty {
  margin: 24px;
}

@media (max-width: 1320px) {
  .resource-grid-shell {
    grid-template-columns: 246px minmax(0, 1fr);
  }

  .resource-inspector {
    grid-column: 1 / -1;
    grid-template-columns: 1.1fr 1fr 1fr;
  }
}

@media (max-width: 900px) {
  .resource-workspace,
  .resource-grid-shell {
    grid-template-columns: 1fr;
    height: auto;
  }

  .resource-rail {
    position: static;
  }

  .resource-topbar,
  .resource-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .resource-search {
    width: 100%;
  }

  .resource-inspector {
    grid-template-columns: 1fr;
  }

  .resource-form-grid {
    grid-template-columns: 1fr;
  }

  .resource-plan-row {
    grid-template-columns: 1fr;
  }

  .resource-modal-backdrop {
    padding: 12px;
  }
}
</style>
