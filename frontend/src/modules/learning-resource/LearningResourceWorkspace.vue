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
              <button type="button" title="截图摘录入题库" :disabled="!canCaptureQuestion" :class="{ active: captureMode && captureTarget === 'question' }" @click="beginCaptureQuestion"><FileQuestion :size="17" /></button>
              <button type="button" title="截图加入知识卡片" :disabled="!canCaptureQuestion" :class="{ active: captureMode && captureTarget === 'card' }" @click="beginCaptureCard"><StickyNote :size="17" /></button>
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
            <p v-if="captureMode" class="capture-hint">{{ captureHintText }}</p>
            <div
              ref="documentSurface"
              class="document-surface"
              :class="{ 'pdf-surface': previewMode === 'pdf', capturing: captureMode }"
              :style="surfaceStyle"
              @pointerdown="handleSurfacePointerDown"
              @pointermove="handleSurfacePointerMove"
              @pointerup="handleSurfacePointerUp"
              @pointercancel="handleSurfacePointerCancel"
              @pointerleave="handleSurfacePointerLeave"
            >
              <canvas v-show="previewMode === 'pdf'" ref="pdfCanvas" class="pdf-canvas"></canvas>
              <div v-if="previewError" class="preview-error">
                <strong>PDF 预览失败</strong>
                <p>{{ previewError }}</p>
                <a v-if="selectedResource?.fileUrl" :href="assetUrl(selectedResource.fileUrl)" target="_blank" rel="noreferrer">打开原文件</a>
              </div>
              <div v-show="previewMode === 'docx'" class="docx-page" v-html="docxHtml"></div>
              <img
                v-if="previewMode === 'image'"
                class="image-page"
                :src="assetUrl(selectedResource.fileUrl)"
                alt="学习资料"
                @load="updateSurfaceSize"
              />
              <video
                v-if="previewMode === 'video'"
                class="media-page video-page"
                :src="assetUrl(selectedResource.fileUrl)"
                controls
                preload="metadata"
                @pointerdown.stop
                @pointermove.stop
                @pointerup.stop
                @click.stop
                @loadedmetadata="updateSurfaceSize"
              ></video>
              <audio
                v-if="previewMode === 'audio'"
                class="media-page audio-page"
                :src="assetUrl(selectedResource.fileUrl)"
                controls
                preload="metadata"
                @pointerdown.stop
                @pointermove.stop
                @pointerup.stop
                @click.stop
                @loadedmetadata="updateSurfaceSize"
              ></audio>
              <div v-if="previewMode === 'unsupported'" class="unsupported-page">
                <span>{{ iconFor(selectedResource) }}</span>
                <h3>该文件已归档</h3>
                <p>当前类型可上传、记录进度、保存标注数据。需要查看原文时可下载后打开。</p>
                <a :href="assetUrl(selectedResource.fileUrl)" target="_blank" rel="noreferrer">打开文件</a>
              </div>

              <svg v-if="canAnnotatePreview" class="annotation-layer" :viewBox="`0 0 ${surfaceSize.width} ${surfaceSize.height}`" preserveAspectRatio="none">
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
              <div v-if="canAnnotatePreview" class="annotation-note-layer">
                <article
                  v-for="annotation in visibleNoteAnnotations"
                  :key="annotation.id"
                  class="annotation-note-card"
                  :style="noteCardStyle(annotation)"
                  @pointerdown.stop
                  @pointermove.stop
                  @pointerup.stop
                  @click.stop
                >
                  <textarea
                    :data-note-id="annotation.id"
                    :value="annotation.text || ''"
                    maxlength="1200"
                    placeholder="输入笔记"
                    @input="updateNoteText(annotation.id, $event.target.value)"
                    @keydown.stop
                  ></textarea>
                  <button type="button" title="删除便签" @click="removeAnnotation(annotation.id)">×</button>
                </article>
              </div>
              <div v-if="captureMode" class="capture-layer" aria-hidden="true">
                <div v-if="captureDraft" class="capture-selection" :style="captureSelectionStyle"></div>
              </div>
            </div>
          </div>
        </section>

        <aside class="resource-inspector">
          <section class="resource-progress-panel">
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
            <button class="resource-secondary" type="button" :disabled="!canCaptureQuestion" @click="beginCaptureQuestion">
              <FileQuestion :size="18" />
              <span>截图摘录入题库</span>
            </button>
            <button class="resource-secondary card-action" type="button" :disabled="!canCaptureQuestion" @click="beginCaptureCard">
              <StickyNote :size="18" />
              <span>截图加入知识卡片</span>
            </button>
            <p v-if="extractError" class="resource-form-error">{{ extractError }}</p>
            <div v-if="extractResult" class="ai-extract-result">
              <strong>{{ extractResult.createdCount ? `已入库 ${extractResult.createdCount} 题` : '图片题已保存' }}</strong>
              <span>{{ extractResult.message || '截图题已保存到题库。' }}</span>
              <p v-for="warning in extractResult.warnings || []" :key="warning">{{ warning }}</p>
            </div>
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
            <span class="resource-kicker">SCREENSHOT TO QUESTION</span>
            <h3>摘录入题库</h3>
            <p>{{ selectedResource?.name }} · 第 {{ excerptForm.sourcePage || currentPage }} 页</p>
          </div>
          <button type="button" aria-label="关闭" @click="closeExcerptModal">×</button>
        </header>
        <form class="resource-excerpt-form" @submit.prevent="submitExcerptQuestion">
          <div class="capture-preview">
            <img v-if="excerptForm.imageDataUrl" :src="excerptForm.imageDataUrl" alt="截取的题目图片" />
            <span v-else>先在文档上框选题目区域</span>
          </div>
          <div class="resource-form-grid">
            <label>
              <span>正确答案 *</span>
              <input v-model.trim="excerptForm.correctAnswer" required maxlength="1000" placeholder="例如：A，或简答要点" />
            </label>
            <label>
              <span>归档文件夹</span>
              <select v-model="excerptForm.categoryId">
                <option :value="null">未分类</option>
                <option v-for="category in categoryOptions" :key="category.id" :value="category.id">{{ category.name }}</option>
              </select>
            </label>
          </div>
          <p v-if="excerptError" class="resource-form-error">{{ excerptError }}</p>
          <footer>
            <button type="button" @click="closeExcerptModal">取消</button>
            <button class="resource-primary inline" type="submit" :disabled="excerptSaving">
              <FileQuestion :size="18" />
              <span>{{ excerptSaving ? '保存中...' : '保存截图题' }}</span>
            </button>
          </footer>
        </form>
      </section>
    </div>

    <div v-if="cardModalOpen" class="resource-modal-backdrop" @click.self="closeCardModal">
      <section class="resource-modal card-capture-modal" role="dialog" aria-modal="true" aria-label="截图加入知识卡片">
        <header>
          <div>
            <span class="resource-kicker">SCREENSHOT TO CARD</span>
            <h3>加入知识卡片</h3>
            <p>{{ selectedResource?.name }} · 第 {{ cardForm.sourcePage || currentPage }} 页</p>
          </div>
          <button type="button" aria-label="关闭" @click="closeCardModal">×</button>
        </header>
        <form class="resource-excerpt-form" @submit.prevent="submitCaptureCard">
          <div class="capture-preview card-preview">
            <img v-if="cardForm.imageDataUrl" :src="cardForm.imageDataUrl" alt="截取的知识卡片图片" />
            <span v-else>先在资料里框选知识点区域</span>
          </div>
          <div class="resource-form-grid">
            <label>
              <span>卡片标题 *</span>
              <input v-model.trim="cardForm.title" required maxlength="80" placeholder="例如：极限夹逼准则" />
            </label>
            <label>
              <span>标签</span>
              <input v-model.trim="cardForm.tag" maxlength="40" placeholder="例如：高数 / 错题 / 公式" />
            </label>
          </div>
          <label>
            <span>卡片内容 *</span>
            <textarea v-model.trim="cardForm.content" required maxlength="1600" rows="5" placeholder="写下定义、公式、推导步骤或容易忘的提醒"></textarea>
          </label>
          <p v-if="cardError" class="resource-form-error">{{ cardError }}</p>
          <footer>
            <button type="button" @click="closeCardModal">取消</button>
            <button class="resource-primary inline" type="submit">
              <StickyNote :size="18" />
              <span>保存到知识卡片</span>
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
import html2canvas from 'html2canvas'
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
  createImageQuestionFromResource,
  deleteLearningResource,
  fetchClassificationOverview,
  listLearningResources,
  resolveAssetUrl,
  toggleLearningResourceFavorite,
  updateLearningResourceAnnotations,
  updateLearningResourceProgress,
  uploadLearningResource,
} from '../../api'

pdfjsLib.GlobalWorkerOptions.workerSrc = pdfWorkerUrl

const emit = defineEmits(['back-home', 'create-card'])

const loading = ref(false)
const uploading = ref(false)
const resources = ref([])
const resourceTypeCounts = reactive({ total: 0, 文档: 0, 图片: 0, 视频: 0, 音频: 0 })
const categoryOptions = ref([])
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
const annotationSaveTimer = ref(null)
const annotationSaving = ref(false)
const captureMode = ref(false)
const captureTarget = ref('question')
const captureDraft = ref(null)
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
const extractError = ref('')
const extractResult = ref(null)
const resourceToast = ref('')
const excerptForm = reactive({
  content: '',
  correctAnswer: '',
  categoryId: null,
  sourcePage: 1,
  imageDataUrl: '',
})
const cardModalOpen = ref(false)
const cardError = ref('')
const cardForm = reactive({
  title: '',
  tag: '',
  content: '',
  sourcePage: 1,
  imageDataUrl: '',
})

const folders = computed(() => {
  return [
    { type: '', label: '未归档笔记', icon: FolderOpen, count: resourceTypeCounts.total },
    { type: '文档', label: '文档资料', icon: FileText, count: resourceTypeCounts.文档 },
    { type: '图片', label: '图像素材', icon: Image, count: resourceTypeCounts.图片 },
    { type: '视频', label: '课程视频', icon: Video, count: resourceTypeCounts.视频 },
    { type: '音频', label: '音频资料', icon: Volume2, count: resourceTypeCounts.音频 },
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
  annotations.value
    .filter((item) => Number(item.page || 1) === Number(currentPage.value) && item.type !== 'note')
    .map(displaySvgAnnotation),
)
const visibleNoteAnnotations = computed(() =>
  annotations.value
    .filter((item) => Number(item.page || 1) === Number(currentPage.value) && item.type === 'note')
    .map(displayAnnotation),
)
const canAnnotatePreview = computed(() => ['pdf', 'docx', 'image'].includes(previewMode.value))
const canCaptureQuestion = computed(() =>
  Boolean(selectedResource.value)
    && !excerptSaving.value
    && canAnnotatePreview.value,
)
const captureHintText = computed(() =>
  captureTarget.value === 'card'
    ? '拖拽框选知识点区域，松开后加入知识卡片。'
    : '拖拽框选题目区域，松开后保存截图题。',
)
const captureSelectionStyle = computed(() => {
  if (!captureDraft.value) return {}
  const rect = normalizedCaptureRect(captureDraft.value)
  return {
    left: `${rect.x}px`,
    top: `${rect.y}px`,
    width: `${rect.width}px`,
    height: `${rect.height}px`,
  }
})

onMounted(() => {
  loadCategories()
  loadResourceTypeCounts()
  loadResources()
  document.addEventListener('fullscreenchange', handleFullscreenChange)
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  if (annotationSaveTimer.value) {
    window.clearTimeout(annotationSaveTimer.value)
    annotationSaveTimer.value = null
  }
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
      loading.value = false
      await nextTick()
      await selectResource(resources.value[0])
    } else if (selectedResource.value) {
      const fresh = resources.value.find((item) => item.id === selectedResource.value.id)
      if (fresh) selectedResource.value = fresh
    }
  } finally {
    loading.value = false
  }
}

async function loadResourceTypeCounts() {
  try {
    const baseParams = {
      page: 1,
      size: 1,
      keyword: filters.keyword || undefined,
    }
    const [all, document, image, video, audio] = await Promise.all([
      listLearningResources(baseParams),
      listLearningResources({ ...baseParams, type: '文档' }),
      listLearningResources({ ...baseParams, type: '图片' }),
      listLearningResources({ ...baseParams, type: '视频' }),
      listLearningResources({ ...baseParams, type: '音频' }),
    ])
    resourceTypeCounts.total = pageTotal(all)
    resourceTypeCounts.文档 = pageTotal(document)
    resourceTypeCounts.图片 = pageTotal(image)
    resourceTypeCounts.视频 = pageTotal(video)
    resourceTypeCounts.音频 = pageTotal(audio)
  } catch {
    resourceTypeCounts.total = resources.value.length
    resourceTypeCounts.文档 = countVisibleType('文档')
    resourceTypeCounts.图片 = countVisibleType('图片')
    resourceTypeCounts.视频 = countVisibleType('视频')
    resourceTypeCounts.音频 = countVisibleType('音频')
  }
}

function pageTotal(response) {
  const total = Number(response.data?.data?.total ?? 0)
  return Number.isFinite(total) ? total : 0
}

function countVisibleType(type) {
  return resources.value.filter((item) => item.type === type).length
}

async function loadCategories() {
  try {
    const response = await fetchClassificationOverview()
    categoryOptions.value = response.data?.data?.categories || []
  } catch {
    categoryOptions.value = []
  }
}

function reloadFirstPage() {
  selectedResource.value = null
  loadResourceTypeCounts()
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
    await loadResourceTypeCounts()
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
  extractError.value = ''
  extractResult.value = null
  annotations.value = normalizeAnnotations(resource.annotations)
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
  const filename = resourceFilename(selectedResource.value)
  const mimeType = (selectedResource.value.mimeType || '').toLowerCase()
  await nextTick()
  if (filename.endsWith('.pdf') || mimeType.includes('pdf')) {
    previewMode.value = 'pdf'
    await renderPdf()
  } else if (/\.(doc|docx)$/.test(filename) || mimeType.includes('wordprocessingml') || mimeType.includes('msword')) {
    previewMode.value = 'docx'
    await renderDocx()
  } else if (/\.(png|jpg|jpeg|webp|gif)$/.test(filename) || mimeType.startsWith('image/')) {
    previewMode.value = 'image'
    await nextTick()
    updateSurfaceSize()
  } else if (/\.(mp4|webm|ogg|mov|m4v)$/.test(filename) || mimeType.startsWith('video/')) {
    previewMode.value = 'video'
    pageCount.value = 1
    currentPage.value = 1
    await nextTick()
    updateSurfaceSize()
  } else if (/\.(mp3|wav|ogg|m4a|aac|flac)$/.test(filename) || mimeType.startsWith('audio/')) {
    previewMode.value = 'audio'
    pageCount.value = 1
    currentPage.value = 1
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
    previewError.value = ''
  } catch (error) {
    console.error(error)
    previewError.value = `文件已读取，但浏览器没有成功绘制页面：${error?.message || '未知错误'}`
    await nextTick()
    updateSurfaceSize()
  }
}

async function renderDocx() {
  try {
    const response = await fetch(assetUrl(selectedResource.value.fileUrl))
    if (!response.ok) {
      throw new Error(`DOCX request failed: ${response.status}`)
    }
    const arrayBuffer = await response.arrayBuffer()
    const result = await mammoth.convertToHtml({ arrayBuffer })
    docxHtml.value = result.value || '<p>文档暂无可预览内容。</p>'
    previewError.value = ''
  } catch (error) {
    console.error(error)
    previewError.value = `文档预览失败：${error?.message || '未知错误'}`
    docxHtml.value = '<p>文档没有成功解析，可先打开原文件查看。</p>'
  }
  await nextTick()
  updateSurfaceSize()
}

function resourceFilename(resource) {
  return [
    resource?.originalFilename,
    resource?.name,
    resource?.fileUrl,
  ].filter(Boolean).join(' ').toLowerCase()
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
  if (!canAnnotatePreview.value) return
  if (tool.value === 'pan' || !selectedResource.value) return
  event.preventDefault()
  const point = pointerPagePoint(event)
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
    pageWidth: surfaceSize.width,
    pageHeight: surfaceSize.height,
    zoom: zoom.value,
    createdAt: new Date().toISOString(),
  }
  if (tool.value === 'pen') {
    draftAnnotation.value = { ...base, points: [point] }
  } else if (tool.value === 'highlight') {
    draftAnnotation.value = { ...base, startX: point.x, startY: point.y, x: point.x, y: point.y, width: 1, height: 1 }
  } else if (tool.value === 'note') {
    const annotation = { ...base, x: point.x, y: point.y, width: notePageSize().width, height: notePageSize().height, text: '' }
    annotations.value.push(annotation)
    scheduleAnnotationSave()
    nextTick(() => focusNote(annotation.id))
  }
}

function moveAnnotation(event) {
  if (!canAnnotatePreview.value) return
  if (tool.value === 'eraser' && selectedResource.value) {
    event.preventDefault()
    eraseAt(pointerPagePoint(event))
    return
  }
  if (!draftAnnotation.value) return
  const point = pointerPagePoint(event)
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
  scheduleAnnotationSave()
}

function pointerPagePoint(event) {
  const rect = documentSurface.value.getBoundingClientRect()
  return {
    x: Math.max(0, Math.min(surfaceSize.width, ((event.clientX - rect.left) / rect.width) * surfaceSize.width)),
    y: Math.max(0, Math.min(surfaceSize.height, ((event.clientY - rect.top) / rect.height) * surfaceSize.height)),
  }
}

function undoAnnotation() {
  if (!annotations.value.length) return
  annotations.value.pop()
  scheduleAnnotationSave()
}

function eraseAt(point) {
  const radius = toPageLength(22)
  const before = annotations.value.length
  annotations.value = annotations.value.filter((annotation) => {
    if (Number(annotation.page || 1) !== Number(currentPage.value)) return true
    return !hitAnnotation(annotation, point, radius)
  })
  if (before !== annotations.value.length) {
    draftAnnotation.value = null
    scheduleAnnotationSave()
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
    return hitRect({ x: annotation.x, y: annotation.y, width: annotation.width || notePageSize().width, height: annotation.height || notePageSize().height }, point, radius)
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

async function saveAnnotations(options = {}) {
  if (!selectedResource.value) return
  if (annotationSaveTimer.value) {
    window.clearTimeout(annotationSaveTimer.value)
    annotationSaveTimer.value = null
  }
  annotationSaving.value = true
  try {
    const response = await updateLearningResourceAnnotations(selectedResource.value.id, annotations.value.map(prepareAnnotationForSave))
    const updated = response.data?.data || selectedResource.value
    selectedResource.value = options.silent ? { ...updated, annotations: annotations.value } : updated
    if (!options.silent) {
      annotations.value = normalizeAnnotations(updated.annotations)
    }
    resources.value = resources.value.map((item) => (item.id === updated.id ? updated : item))
    if (!options.silent) showToast('标注已保存到资料')
  } finally {
    annotationSaving.value = false
  }
}

function scheduleAnnotationSave() {
  if (!selectedResource.value) return
  if (annotationSaveTimer.value) window.clearTimeout(annotationSaveTimer.value)
  annotationSaveTimer.value = window.setTimeout(() => {
    annotationSaveTimer.value = null
    saveAnnotations({ silent: true }).catch(() => {
      extractError.value = '标注自动保存失败，请点击保存标注重试'
    })
  }, 700)
}

function updateNoteText(id, text) {
  annotations.value = annotations.value.map((annotation) =>
    annotation.id === id ? { ...annotation, text, updatedAt: new Date().toISOString() } : annotation,
  )
  scheduleAnnotationSave()
}

function removeAnnotation(id) {
  annotations.value = annotations.value.filter((annotation) => annotation.id !== id)
  scheduleAnnotationSave()
}

function focusNote(id) {
  documentSurface.value?.querySelector?.(`[data-note-id="${id}"]`)?.focus?.()
}

function normalizeAnnotations(value) {
  if (!Array.isArray(value)) return []
  return value.map((annotation) => {
    const pageWidth = Number(annotation.pageWidth || 0) || null
    const pageHeight = Number(annotation.pageHeight || 0) || null
    const normalized = {
      ...annotation,
      id: annotation.id || `ann-${Date.now()}-${Math.random().toString(16).slice(2)}`,
      page: Math.max(1, Number(annotation.page || 1)),
      color: annotation.color || '#3d7cff',
      coordinateSpace: annotation.coordinateSpace || 'page',
      pageWidth,
      pageHeight,
      zoom: Number(annotation.zoom || 100),
    }
    if (normalized.type === 'pen') {
      normalized.points = (annotation.points || []).map((point) => ({
        x: Number(point.x || 0),
        y: Number(point.y || 0),
      }))
    } else {
      normalized.x = Number(annotation.x || 0)
      normalized.y = Number(annotation.y || 0)
      normalized.width = Number(annotation.width || (normalized.type === 'note' ? notePageSize().width : 0))
      normalized.height = Number(annotation.height || (normalized.type === 'note' ? notePageSize().height : 0))
    }
    return normalized
  })
}

function prepareAnnotationForSave(annotation) {
  const prepared = {
    id: annotation.id,
    type: annotation.type,
    color: annotation.color,
    coordinateSpace: 'page',
    page: Math.max(1, Number(annotation.page || 1)),
    pageWidth: Number.isFinite(Number(annotation.pageWidth)) ? Number(annotation.pageWidth) : null,
    pageHeight: Number.isFinite(Number(annotation.pageHeight)) ? Number(annotation.pageHeight) : null,
    zoom: Number(annotation.zoom || zoom.value || 100),
    x: Number.isFinite(Number(annotation.x)) ? Number(annotation.x) : null,
    y: Number.isFinite(Number(annotation.y)) ? Number(annotation.y) : null,
    width: Number.isFinite(Number(annotation.width)) ? Number(annotation.width) : null,
    height: Number.isFinite(Number(annotation.height)) ? Number(annotation.height) : null,
    points: Array.isArray(annotation.points)
      ? annotation.points.map((point) => ({ x: Number(point.x || 0), y: Number(point.y || 0) }))
      : null,
    text: annotation.text || null,
    createdAt: annotation.createdAt || new Date().toISOString(),
  }
  return prepared
}

function displaySvgAnnotation(annotation) {
  const scaleX = surfaceSize.width / Math.max(1, Number(annotation.pageWidth || surfaceSize.width || 1))
  const scaleY = surfaceSize.height / Math.max(1, Number(annotation.pageHeight || surfaceSize.height || 1))
  if (annotation.type === 'pen') {
    return {
      ...annotation,
      points: (annotation.points || []).map((point) => ({
        x: Number(point.x || 0) * scaleX,
        y: Number(point.y || 0) * scaleY,
      })),
    }
  }
  return {
    ...annotation,
    x: Number(annotation.x || 0) * scaleX,
    y: Number(annotation.y || 0) * scaleY,
    width: Number(annotation.width || 0) * scaleX,
    height: Number(annotation.height || 0) * scaleY,
  }
}

function displayAnnotation(annotation) {
  const scaleX = renderedSize.width / Math.max(1, Number(annotation.pageWidth || surfaceSize.width || 1))
  const scaleY = renderedSize.height / Math.max(1, Number(annotation.pageHeight || surfaceSize.height || 1))
  if (annotation.type === 'pen') {
    return {
      ...annotation,
      points: (annotation.points || []).map((point) => ({
        x: Number(point.x || 0) * scaleX,
        y: Number(point.y || 0) * scaleY,
      })),
    }
  }
  return {
    ...annotation,
    x: Number(annotation.x || 0) * scaleX,
    y: Number(annotation.y || 0) * scaleY,
    width: Number(annotation.width || 0) * scaleX,
    height: Number(annotation.height || 0) * scaleY,
  }
}

function noteCardStyle(annotation) {
  const width = Math.max(132, annotation.width || 190)
  const height = Math.max(74, annotation.height || 78)
  return {
    left: `${annotation.x}px`,
    top: `${annotation.y}px`,
    width: `${width}px`,
    minHeight: `${height}px`,
    borderColor: annotation.color,
  }
}

function notePageSize() {
  return {
    width: Math.min(190, Math.max(120, surfaceSize.width * 0.22)),
    height: 78,
  }
}

function toPageLength(pixelLength) {
  return (Number(pixelLength) || 0) * (surfaceSize.width / Math.max(1, renderedSize.width || surfaceSize.width))
}

function openExcerptModal() {
  beginCaptureQuestion()
}

function beginCaptureQuestion() {
  if (!canCaptureQuestion.value) return
  captureMode.value = true
  captureTarget.value = 'question'
  captureDraft.value = null
  tool.value = 'pan'
  extractError.value = ''
  extractResult.value = null
  showToast('拖拽框选题目区域')
}

function beginCaptureCard() {
  if (!canCaptureQuestion.value) return
  captureMode.value = true
  captureTarget.value = 'card'
  captureDraft.value = null
  tool.value = 'pan'
  extractError.value = ''
  cardError.value = ''
  extractResult.value = null
  showToast('拖拽框选知识点区域')
}

function stopCaptureQuestion() {
  captureMode.value = false
  captureDraft.value = null
}

function handleSurfacePointerDown(event) {
  if (captureMode.value) {
    startCaptureSelection(event)
    return
  }
  startAnnotation(event)
}

function handleSurfacePointerMove(event) {
  if (captureMode.value) {
    moveCaptureSelection(event)
    return
  }
  moveAnnotation(event)
}

function handleSurfacePointerUp(event) {
  if (captureMode.value) {
    finishCaptureSelection(event)
    return
  }
  finishAnnotation()
}

function handleSurfacePointerCancel() {
  if (captureMode.value) {
    captureDraft.value = null
    return
  }
  finishAnnotation()
}

function handleSurfacePointerLeave(event) {
  if (captureMode.value) {
    if (captureDraft.value) moveCaptureSelection(event)
    return
  }
  finishAnnotation()
}

function startCaptureSelection(event) {
  if (!canCaptureQuestion.value) return
  event.preventDefault()
  documentSurface.value?.setPointerCapture?.(event.pointerId)
  const point = pointerDisplayPoint(event)
  captureDraft.value = {
    startX: point.x,
    startY: point.y,
    endX: point.x,
    endY: point.y,
  }
}

function moveCaptureSelection(event) {
  if (!captureDraft.value) return
  event.preventDefault()
  const point = pointerDisplayPoint(event)
  captureDraft.value.endX = point.x
  captureDraft.value.endY = point.y
}

async function finishCaptureSelection(event) {
  if (!captureDraft.value) return
  event.preventDefault()
  moveCaptureSelection(event)
  const rect = normalizedCaptureRect(captureDraft.value)
  if (rect.width < 36 || rect.height < 36) {
    captureDraft.value = null
    extractError.value = '框选区域太小，请重新拖拽截取题目'
    return
  }
  try {
    const imageDataUrl = await captureSelectionDataUrl(rect)
    const page = Math.max(1, Number(currentPage.value || 1))
    stopCaptureQuestion()
    if (captureTarget.value === 'card') {
      cardForm.imageDataUrl = imageDataUrl
      cardForm.title = defaultCardTitle(page)
      cardForm.tag = selectedResource.value?.type || '资料截图'
      cardForm.content = ''
      cardForm.sourcePage = page
      cardError.value = ''
      cardModalOpen.value = true
    } else {
      excerptForm.imageDataUrl = imageDataUrl
      excerptForm.correctAnswer = ''
      excerptForm.categoryId = null
      excerptForm.sourcePage = page
      excerptError.value = ''
      excerptModalOpen.value = true
    }
  } catch (error) {
    captureDraft.value = null
    extractError.value = error.message || '截图生成失败'
  }
}

function defaultCardTitle(page) {
  const name = selectedResource.value?.name || '学习资料'
  return `${name} · 第 ${page} 页知识点`
}

function pointerDisplayPoint(event) {
  const rect = documentSurface.value.getBoundingClientRect()
  return {
    x: Math.max(0, Math.min(rect.width, event.clientX - rect.left)),
    y: Math.max(0, Math.min(rect.height, event.clientY - rect.top)),
  }
}

function normalizedCaptureRect(draft) {
  const x = Math.min(draft.startX, draft.endX)
  const y = Math.min(draft.startY, draft.endY)
  return {
    x,
    y,
    width: Math.abs(draft.endX - draft.startX),
    height: Math.abs(draft.endY - draft.startY),
  }
}

async function captureSelectionDataUrl(rect) {
  if (previewMode.value === 'pdf') {
    const canvas = pdfCanvas.value
    if (!canvas || !canvas.width || !canvas.height) {
      throw new Error('当前 PDF 页面还没有渲染完成')
    }
    return canvasToCompressedDataUrl(canvas, rect, renderedSize)
  }
  if (previewMode.value === 'image') {
    const image = documentSurface.value?.querySelector?.('.image-page')
    if (!image?.naturalWidth || !image?.naturalHeight) {
      throw new Error('当前图片还没有加载完成')
    }
    const localRect = elementLocalCaptureRect(rect, image)
    ensureCaptureRect(localRect)
    return imageElementToDataUrl(image, localRect)
  }
  if (previewMode.value === 'docx') {
    const page = documentSurface.value?.querySelector?.('.docx-page')
    if (!page) {
      throw new Error('当前 Word 页面还没有渲染完成')
    }
    const localRect = elementLocalCaptureRect(rect, page)
    ensureCaptureRect(localRect)
    return domElementToDataUrl(page, localRect)
  }
  throw new Error('当前资料不能截图入题库')
}

function imageElementToDataUrl(image, rect) {
  const scaleX = image.naturalWidth / Math.max(1, image.getBoundingClientRect().width)
  const scaleY = image.naturalHeight / Math.max(1, image.getBoundingClientRect().height)
  const source = document.createElement('canvas')
  source.width = image.naturalWidth
  source.height = image.naturalHeight
  source.getContext('2d').drawImage(image, 0, 0)
  return canvasToCompressedDataUrl(source, {
    x: rect.x * scaleX,
    y: rect.y * scaleY,
    width: rect.width * scaleX,
    height: rect.height * scaleY,
  }, { width: image.naturalWidth, height: image.naturalHeight })
}

async function domElementToDataUrl(element, rect) {
  const elementRect = element.getBoundingClientRect()
  const canvas = await html2canvas(element, {
    backgroundColor: '#ffffff',
    scale: Math.min(2, window.devicePixelRatio || 1),
    useCORS: true,
    logging: false,
  })
  return canvasToCompressedDataUrl(canvas, rect, {
    width: elementRect.width,
    height: elementRect.height,
  })
}

function elementLocalCaptureRect(rect, element) {
  const surfaceRect = documentSurface.value?.getBoundingClientRect()
  const elementRect = element.getBoundingClientRect()
  if (!surfaceRect || !elementRect.width || !elementRect.height) {
    throw new Error('当前页面尺寸异常，请稍后再试')
  }
  const offsetX = elementRect.left - surfaceRect.left
  const offsetY = elementRect.top - surfaceRect.top
  const left = clamp(rect.x - offsetX, 0, elementRect.width)
  const top = clamp(rect.y - offsetY, 0, elementRect.height)
  const right = clamp(rect.x + rect.width - offsetX, 0, elementRect.width)
  const bottom = clamp(rect.y + rect.height - offsetY, 0, elementRect.height)
  return {
    x: left,
    y: top,
    width: Math.max(0, right - left),
    height: Math.max(0, bottom - top),
  }
}

function ensureCaptureRect(rect) {
  if (rect.width < 24 || rect.height < 24) {
    throw new Error('框选区域没有落在文档内容上，请重新拖拽')
  }
}

function canvasToCompressedDataUrl(canvas, rect, displaySize) {
  const scaleX = canvas.width / Math.max(1, displaySize.width)
  const scaleY = canvas.height / Math.max(1, displaySize.height)
  const sx = clamp(rect.x * scaleX, 0, canvas.width)
  const sy = clamp(rect.y * scaleY, 0, canvas.height)
  const sw = clamp(rect.width * scaleX, 1, canvas.width - sx)
  const sh = clamp(rect.height * scaleY, 1, canvas.height - sy)
  const maxWidth = 1280
  const maxHeight = 1700
  const scale = Math.min(1, maxWidth / Math.max(1, sw), maxHeight / Math.max(1, sh))
  const target = document.createElement('canvas')
  target.width = Math.max(1, Math.round(sw * scale))
  target.height = Math.max(1, Math.round(sh * scale))
  const context = target.getContext('2d')
  context.drawImage(canvas, sx, sy, sw, sh, 0, 0, target.width, target.height)
  return target.toDataURL('image/jpeg', 0.82)
}

function closeExcerptModal() {
  if (excerptSaving.value) return
  excerptModalOpen.value = false
  excerptError.value = ''
}

function closeCardModal() {
  cardModalOpen.value = false
  cardError.value = ''
}

function submitCaptureCard() {
  if (!selectedResource.value) return
  if (!cardForm.imageDataUrl) {
    cardError.value = '请先框选知识点截图'
    return
  }
  if (!cardForm.title.trim() || !cardForm.content.trim()) {
    cardError.value = '请填写卡片标题和内容'
    return
  }
  emit('create-card', {
    title: cardForm.title,
    tag: cardForm.tag || '资料截图',
    content: cardForm.content,
    imageDataUrl: cardForm.imageDataUrl,
    sourceName: selectedResource.value.name,
    sourcePage: cardForm.sourcePage || currentPage.value || 1,
    createdAt: new Date().toISOString(),
  })
  cardModalOpen.value = false
  cardForm.imageDataUrl = ''
  cardForm.title = ''
  cardForm.tag = ''
  cardForm.content = ''
  showToast('截图已加入知识卡片')
}

async function submitExcerptQuestion() {
  if (!selectedResource.value) return
  if (!excerptForm.imageDataUrl) {
    excerptError.value = '请先框选题目截图'
    return
  }
  if (!excerptForm.correctAnswer.trim()) {
    excerptError.value = '请填写正确答案'
    return
  }
  excerptSaving.value = true
  excerptError.value = ''
  try {
    const page = excerptForm.sourcePage || currentPage.value || 1
    await createImageQuestionFromResource(selectedResource.value.id, {
      title: `${selectedResource.value.name} · 第 ${page} 页截图题`,
      imageDataUrl: excerptForm.imageDataUrl,
      correctAnswer: excerptForm.correctAnswer,
      categoryId: normalizeCategoryId(excerptForm.categoryId),
      sourcePage: page,
      status: 'PUBLISHED',
    })
    extractResult.value = {
      createdCount: 1,
      message: '截图题已归档到题库，可直接用于练习。',
    }
    excerptModalOpen.value = false
    excerptForm.imageDataUrl = ''
    showToast('截图题已保存到题库')
  } catch (error) {
    excerptError.value = error.response?.data?.message || error.message || '截图题保存失败'
  } finally {
    excerptSaving.value = false
  }
}

function normalizeCategoryId(value) {
  if (value === null || value === undefined || value === '' || value === 'null') return null
  const id = Number(value)
  return Number.isFinite(id) ? id : null
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
  await loadResourceTypeCounts()
  await loadResources()
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
  --resource-line: rgba(73, 116, 221, 0.14);
  --resource-glass: rgba(255, 255, 255, 0.7);
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr);
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
  padding: 20px 16px;
  border-right: 1px solid rgba(73, 116, 221, 0.14);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.84), rgba(242, 247, 255, 0.62)),
    radial-gradient(circle at 30% 8%, rgba(111, 213, 255, 0.16), transparent 34%);
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
  border-radius: 16px;
  font-weight: 900;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease,
    color 0.18s ease;
}

.resource-back {
  display: flex;
  min-height: 44px;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  color: var(--resource-blue);
  background: rgba(255, 255, 255, 0.68);
  box-shadow:
    inset 0 0 0 1px rgba(73, 116, 221, 0.12),
    0 10px 24px rgba(74, 96, 155, 0.06);
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
  gap: 8px;
  padding-right: 4px;
}

.resource-folders button {
  position: relative;
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) 32px;
  min-height: 50px;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  color: var(--resource-muted);
  text-align: left;
  background: transparent;
  will-change: transform;
}

.resource-folders button::before {
  position: absolute;
  inset: 4px 0;
  z-index: -1;
  content: "";
  border-left: 3px solid transparent;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.62);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.08);
  transition:
    box-shadow 0.2s ease,
    background 0.2s ease,
    border-color 0.2s ease;
}

.resource-folders button:hover {
  color: var(--resource-blue);
  transform: translateX(8px);
}

.resource-folders button:hover::before {
  background:
    linear-gradient(100deg, rgba(79, 136, 255, 0.1), rgba(111, 213, 255, 0.09) 58%, rgba(240, 166, 223, 0.12)),
    rgba(255, 255, 255, 0.82);
  box-shadow:
    inset 0 0 0 1px rgba(73, 116, 221, 0.12),
    0 12px 24px rgba(79, 136, 255, 0.11);
}

.resource-folders button.active {
  color: var(--resource-blue);
  transform: translateX(6px);
}

.resource-folders button.active::before {
  border-left-color: rgba(79, 136, 255, 0.78);
  background:
    linear-gradient(100deg, rgba(79, 136, 255, 0.14), rgba(111, 213, 255, 0.13) 56%, rgba(240, 166, 223, 0.16)),
    rgba(255, 255, 255, 0.72);
  box-shadow: 0 10px 22px rgba(79, 136, 255, 0.1);
}

.folder-icon {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 14px;
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
  min-height: 54px;
  align-items: center;
  justify-content: center;
  gap: 9px;
  overflow: hidden;
  color: rgba(57, 106, 220, 0.9);
  border: 1px solid rgba(79, 136, 255, 0.12);
  background:
    linear-gradient(100deg, rgba(79, 136, 255, 0.1), rgba(111, 213, 255, 0.12) 62%, rgba(240, 166, 223, 0.14)),
    rgba(255, 255, 255, 0.76);
  box-shadow: 0 14px 28px rgba(79, 136, 255, 0.1);
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
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.8), rgba(247, 251, 255, 0.58)),
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
  border-radius: 18px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.76), rgba(255, 255, 255, 0.32));
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
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
  transform: translateX(3px);
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
  border-radius: 16px;
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
  border-radius: 13px;
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

.capture-hint {
  position: sticky;
  z-index: 8;
  top: 10px;
  width: fit-content;
  margin: 0 auto 12px;
  padding: 9px 16px;
  color: rgba(40, 74, 150, 0.82);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow:
    inset 0 0 0 1px rgba(79, 136, 255, 0.16),
    0 12px 26px rgba(79, 136, 255, 0.12);
  font-size: 13px;
  font-weight: 900;
}

.document-surface {
  position: relative;
  width: fit-content;
  min-width: min(100%, 860px);
  min-height: 780px;
  margin: 0 auto;
  background: #fff;
  border-radius: 18px;
  box-shadow:
    0 28px 56px rgba(56, 75, 130, 0.18),
    0 0 0 1px rgba(73, 116, 221, 0.1);
}

.document-surface.pdf-surface {
  width: auto;
  max-width: none;
  border-radius: 8px;
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
  z-index: 3;
}

.document-surface.capturing {
  cursor: crosshair;
  user-select: none;
}

.capture-layer {
  position: absolute;
  inset: 0;
  z-index: 6;
  pointer-events: none;
}

.capture-selection {
  position: absolute;
  border: 2px solid rgba(79, 136, 255, 0.78);
  border-radius: 16px;
  background:
    linear-gradient(135deg, rgba(79, 136, 255, 0.12), rgba(240, 166, 223, 0.13));
  box-shadow:
    0 0 0 9999px rgba(20, 34, 70, 0.16),
    0 14px 32px rgba(79, 136, 255, 0.18),
    inset 0 0 0 1px rgba(255, 255, 255, 0.72);
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
  position: relative;
  z-index: 1;
  width: min(760px, 72vw);
  min-height: 960px;
  padding: 56px 64px;
  color: #202a44;
  font-size: 16px;
  line-height: 1.75;
  background: #fff;
  border-radius: 18px;
}

.docx-page :deep(p) {
  margin: 0 0 1em;
  color: #202a44;
}

.docx-page :deep(*) {
  max-width: 100%;
  color: inherit;
  background-color: transparent !important;
  box-shadow: none !important;
  text-shadow: none !important;
}

.docx-page :deep(span),
.docx-page :deep(p),
.docx-page :deep(div),
.docx-page :deep(li) {
  color: #202a44 !important;
  opacity: 1 !important;
}

.docx-page :deep(table) {
  border-collapse: collapse;
}

.docx-page :deep(td),
.docx-page :deep(th) {
  border: 1px solid rgba(73, 116, 221, 0.18);
  padding: 6px 8px;
}

.image-page {
  max-width: 920px;
}

.media-page {
  position: relative;
  z-index: 4;
  display: block;
  width: min(100%, 980px);
  min-width: min(100%, 680px);
  pointer-events: auto;
}

.video-page {
  aspect-ratio: 16 / 9;
  max-height: min(68vh, 680px);
  background: #111827;
  border-radius: 14px;
}

.audio-page {
  width: min(720px, 72vw);
  min-height: 88px;
  padding: 24px;
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(79, 136, 255, 0.12), rgba(240, 166, 223, 0.16)),
    rgba(255, 255, 255, 0.88);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.12);
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

.annotation-note-layer {
  position: absolute;
  inset: 0;
  z-index: 3;
  pointer-events: none;
}

.annotation-note-card {
  position: absolute;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 22px;
  gap: 4px;
  padding: 7px;
  border-left: 4px solid;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 8px 18px rgba(55, 77, 134, 0.14);
  pointer-events: auto;
}

.annotation-note-card textarea {
  min-width: 0;
  min-height: 60px;
  resize: vertical;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--resource-ink);
  font-family: inherit;
  font-size: 12px;
  font-weight: 900;
  line-height: 1.45;
}

.annotation-note-card button {
  display: grid;
  width: 22px;
  height: 22px;
  padding: 0;
  place-items: center;
  color: var(--resource-muted);
  border: 1px solid rgba(73, 116, 221, 0.15);
  border-radius: 4px;
  background: rgba(248, 250, 255, 0.9);
  cursor: pointer;
  font-weight: 900;
}

.resource-inspector {
  display: grid;
  align-content: start;
  grid-auto-rows: max-content;
  gap: 14px;
  padding: 12px;
  overflow: auto;
  isolation: isolate;
}

.resource-inspector section {
  position: relative;
  z-index: 0;
  display: grid;
  min-width: 0;
  gap: 12px;
  padding: 4px 2px 14px;
  border-bottom: 1px solid rgba(73, 116, 221, 0.12);
}

.resource-inspector section:last-child {
  border-bottom: 0;
}

.resource-progress-panel {
  justify-items: center;
  min-width: 0;
  grid-auto-rows: max-content;
  align-content: start;
}

.resource-progress-panel > .resource-kicker {
  justify-self: start;
}

.progress-orbit {
  display: grid;
  width: min(138px, 100%);
  aspect-ratio: 1;
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
  width: min(104px, 76%);
  aspect-ratio: 1;
  place-items: center;
  align-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.09);
}

.progress-orbit strong {
  color: var(--resource-blue);
  font-size: 34px;
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
  position: relative;
  z-index: 2;
  display: flex;
  min-height: 44px;
  width: 100%;
  min-width: 0;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  background: linear-gradient(100deg, rgba(79, 136, 255, 0.9), rgba(111, 213, 255, 0.78), rgba(240, 166, 223, 0.82));
  box-shadow: 0 12px 24px rgba(79, 136, 255, 0.14);
}

.resource-secondary {
  position: relative;
  z-index: 2;
  display: flex;
  min-height: 42px;
  width: 100%;
  min-width: 0;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  color: var(--resource-blue);
  border: 0;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.68);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.14);
  font-weight: 900;
}

.resource-primary span,
.resource-secondary span {
  min-width: 0;
  overflow-wrap: anywhere;
}

.resource-primary:disabled,
.resource-secondary:disabled {
  cursor: not-allowed;
  filter: grayscale(0.35);
  opacity: 0.55;
}

.resource-secondary.card-action {
  color: #7b5fd6;
  background:
    linear-gradient(100deg, rgba(255, 255, 255, 0.76), rgba(246, 241, 255, 0.82)),
    rgba(255, 255, 255, 0.68);
  box-shadow:
    inset 0 0 0 1px rgba(123, 95, 214, 0.18),
    0 10px 22px rgba(123, 95, 214, 0.08);
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
  width: min(680px, 100%);
  max-height: min(92vh, 860px);
  overflow: auto;
  color: var(--resource-ink);
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 251, 255, 0.94)),
    repeating-linear-gradient(135deg, rgba(79, 136, 255, 0.028) 0 1px, transparent 1px 11px);
  box-shadow:
    0 26px 70px rgba(38, 58, 118, 0.25),
    inset 0 0 0 1px rgba(73, 116, 221, 0.16);
}

.capture-preview {
  display: grid;
  min-height: 220px;
  place-items: center;
  overflow: hidden;
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(247, 251, 255, 0.62));
  box-shadow:
    inset 0 0 0 1px rgba(73, 116, 221, 0.14),
    0 14px 34px rgba(74, 96, 155, 0.08);
}

.capture-preview img {
  display: block;
  max-width: 100%;
  max-height: min(48vh, 430px);
  object-fit: contain;
}

.capture-preview.card-preview {
  min-height: 260px;
  background:
    radial-gradient(circle at 12% 12%, rgba(123, 95, 214, 0.1), transparent 32%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(248, 246, 255, 0.7));
}

.card-capture-modal .resource-primary.inline {
  background: linear-gradient(100deg, #6d7dff, #7bd4ff 52%, #d8a5f4);
}

.capture-preview span {
  color: var(--resource-muted);
  font-weight: 900;
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
  border-radius: 14px;
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
  border-radius: 14px;
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
  border-radius: 16px;
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
  border-radius: 16px;
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
  border-radius: 14px;
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

.ai-extract-result {
  display: grid;
  gap: 5px;
  width: 100%;
  padding: 10px;
  border-radius: 16px;
  background: rgba(79, 136, 255, 0.06);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.12);
}

.ai-extract-result strong {
  color: var(--resource-blue);
  font-size: 13px;
}

.ai-extract-result span,
.ai-extract-result p {
  margin: 0;
  color: var(--resource-muted);
  font-size: 12px;
  font-weight: 800;
  line-height: 1.55;
}

.resource-toast {
  position: fixed;
  right: 22px;
  bottom: 22px;
  z-index: 1200;
  margin: 0;
  padding: 12px 16px;
  color: #fff;
  border-radius: 16px;
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
  display: grid;
  grid-template-columns: minmax(72px, 0.8fr) minmax(0, 1.2fr);
  align-items: start;
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
  min-width: 0;
  overflow-wrap: anywhere;
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
  border-radius: 18px;
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
    grid-template-columns: 1fr;
    overflow: visible;
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
