<template>
  <div class="share-workspace">
    <aside class="share-rail">
      <button class="share-back" type="button" @click="emit('back-home')">
        <ChevronLeft :size="18" />
        <strong>学习控制台</strong>
      </button>

      <section class="share-brand">
        <small>COMMUNITY / RESOURCE</small>
        <h1>资源分享</h1>
        <p>把课堂资料、视频链接、笔记模板和工具包放到这里，像频道一样浏览，也能收藏到自己的学习清单。</p>
      </section>

      <section class="share-stats">
        <article>
          <strong>{{ resources.length }}</strong>
          <span>资源</span>
        </article>
        <article>
          <strong>{{ favoriteResources.length }}</strong>
          <span>收藏</span>
        </article>
        <article>
          <strong>{{ uploadCount }}</strong>
          <span>上传</span>
        </article>
      </section>

      <section class="share-categories">
        <button
          v-for="category in categories"
          :key="category.id"
          type="button"
          :class="{ active: activeCategory === category.id }"
          @click="activeCategory = category.id"
        >
          <span :style="{ background: category.color }"></span>
          <strong>{{ category.label }}</strong>
          <b>{{ countByCategory(category.id) }}</b>
        </button>
      </section>
    </aside>

    <main class="share-main">
      <header class="share-topbar">
        <div>
          <span class="share-kicker">RESOURCE SHARE</span>
          <h2>学习资源广场</h2>
        </div>
        <label class="share-search">
          <Search :size="17" />
          <input v-model.trim="keyword" type="search" placeholder="搜索标题、作者、标签" />
        </label>
        <div class="share-tabs" role="tablist" aria-label="资源视图">
          <button type="button" :class="{ active: viewMode === 'discover' }" @click="viewMode = 'discover'">
            <Clapperboard :size="17" />
            <span>发现</span>
          </button>
          <button type="button" :class="{ active: viewMode === 'favorites' }" @click="viewMode = 'favorites'">
            <Heart :size="17" />
            <span>收藏</span>
          </button>
        </div>
        <button class="share-upload-button" type="button" @click="openUpload">
          <UploadCloud :size="20" />
          <span>上传资源</span>
        </button>
      </header>

      <section class="share-category-strip" aria-label="资源分类">
        <button
          v-for="category in categories"
          :key="category.id"
          type="button"
          :class="{ active: activeCategory === category.id }"
          @click="activeCategory = category.id"
        >
          {{ category.label }}
        </button>
      </section>

      <section class="share-video-board">
        <article
          v-if="featuredResource"
          class="share-bili-hero"
          :class="{ playing: heroPlayableUrl }"
          :style="coverStyle(featuredResource)"
          @click="selectResource(featuredResource)"
        >
          <video
            v-if="heroPlayableUrl"
            class="share-hero-player"
            :src="heroPlayableUrl"
            controls
            preload="metadata"
            :poster="featuredResource.cover || undefined"
            @click.stop
          ></video>
          <span class="share-category-pill">{{ featuredResource.category }}</span>
          <button class="share-hero-heart" type="button" title="收藏" @click.stop="toggleFavorite(featuredResource.id)">
            <Heart :size="20" :fill="featuredResource.favorite ? 'currentColor' : 'none'" />
          </button>
          <div v-if="filteredResources.length > 1" class="share-hero-arrows">
            <button type="button" aria-label="上一张" @click.stop="moveFeatured(-1)">
              <ChevronLeft :size="22" />
            </button>
            <button type="button" aria-label="下一张" @click.stop="moveFeatured(1)">
              <ChevronRight :size="22" />
            </button>
          </div>
          <PlayCircle v-if="!heroPlayableUrl" :size="62" />
          <div class="share-bili-caption">
            <h3>{{ featuredResource.title }}</h3>
            <p>{{ featuredResource.author }} · {{ featuredResource.views }} 次浏览 · {{ featuredResource.duration }}</p>
            <div v-if="filteredResources.length > 1" class="share-dots" aria-label="推荐画幅切换">
              <button
                v-for="(resource, index) in filteredResources"
                :key="resource.id"
                type="button"
                :class="{ active: featuredIndex === index }"
                :aria-label="`切换到第 ${index + 1} 张`"
                @click.stop="setFeaturedIndex(index)"
              ></button>
            </div>
          </div>
        </article>

        <div v-if="boardResources.length" class="share-board-grid">
          <article v-for="resource in boardResources" :key="resource.id" class="share-video-card">
            <button class="share-cover" type="button" :style="coverStyle(resource)" @click="selectResource(resource)">
              <span class="share-duration">{{ resource.duration }}</span>
              <span class="share-cover-stats">
                <span><PlayCircle :size="14" />{{ resource.views }}</span>
                <span><Heart :size="14" />{{ resource.favorite ? '已藏' : '收藏' }}</span>
              </span>
              <PlayCircle v-if="resource.kind === '视频'" :size="38" />
              <FileText v-else-if="resource.kind === '文档'" :size="38" />
              <ImageIcon v-else-if="resource.kind === '图片'" :size="38" />
              <LinkIcon v-else :size="38" />
            </button>
            <div class="share-video-copy">
              <button class="share-heart" type="button" title="收藏" @click="toggleFavorite(resource.id)">
                <Heart :size="18" :fill="resource.favorite ? 'currentColor' : 'none'" />
              </button>
              <h3 @click="selectResource(resource)">{{ resource.title }}</h3>
              <p>{{ resource.author }} · {{ resource.createdAt }}</p>
            </div>
          </article>
        </div>

        <p v-if="!featuredResource" class="share-empty">
          {{ viewMode === 'favorites' ? '还没有收藏资源，去发现页点亮爱心吧。' : '没有匹配的资源，换个分类或关键词试试。' }}
        </p>
      </section>

      <section class="share-lower-layout">
        <section class="share-panel share-collection-panel">
          <div>
            <span class="share-kicker">COLLECTION</span>
            <h3>收藏页面</h3>
            <p>点过爱心的资源会进入这里，之后可以直接切到收藏视图继续学习。</p>
          </div>
          <button type="button" @click="viewMode = 'favorites'">
            <Heart :size="17" />
            <span>查看收藏</span>
          </button>
        </section>

        <section class="share-panel share-ranking">
          <span class="share-kicker">TRENDING</span>
          <article v-for="(resource, index) in trendingResources" :key="resource.id" @click="selectResource(resource)">
            <b>{{ index + 1 }}</b>
            <span>
              <strong>{{ resource.title }}</strong>
              <small>{{ resource.category }} · {{ resource.views }} 次浏览</small>
            </span>
          </article>
        </section>
      </section>
    </main>

    <section v-if="selectedResource" class="share-detail-drawer">
      <button type="button" aria-label="关闭详情" @click="selectedResource = null">×</button>
      <video
        v-if="selectedResource.kind === '视频' && playableUrl(selectedResource)"
        class="share-detail-player"
        :src="playableUrl(selectedResource)"
        controls
        preload="metadata"
        :poster="selectedResource.cover || undefined"
      ></video>
      <div v-else class="share-detail-cover" :style="coverStyle(selectedResource)"></div>
      <div>
        <span class="share-kicker">{{ selectedResource.category }}</span>
        <h3>{{ selectedResource.title }}</h3>
        <p>{{ selectedResource.description }}</p>
        <div class="share-tags">
          <span v-for="tag in selectedResource.tags" :key="tag">{{ tag }}</span>
        </div>
        <div class="share-detail-actions">
          <button type="button" @click="toggleFavorite(selectedResource.id)">
            <Heart :size="18" :fill="selectedResource.favorite ? 'currentColor' : 'none'" />
            <span>{{ selectedResource.favorite ? '已收藏' : '收藏' }}</span>
          </button>
          <a v-if="selectedResource.link" :href="selectedResource.link" target="_blank" rel="noreferrer">打开资源</a>
        </div>
      </div>
    </section>

    <div v-if="uploadOpen" class="share-modal-backdrop" @click.self="closeUpload">
      <section class="share-modal" role="dialog" aria-modal="true" aria-label="上传资源">
        <header>
          <div>
            <span class="share-kicker">UPLOAD</span>
            <h3>上传分享资源</h3>
          </div>
          <button type="button" aria-label="关闭" @click="closeUpload">×</button>
        </header>
        <form class="share-form" @submit.prevent="submitUpload">
          <label>
            <span>资源标题 *</span>
            <input v-model.trim="uploadForm.title" required maxlength="80" placeholder="例如：线性代数期末复习包" />
          </label>
          <div class="share-form-grid">
            <label>
              <span>分类</span>
              <select v-model="uploadForm.category">
                <option v-for="category in uploadCategories" :key="category" :value="category">{{ category }}</option>
              </select>
            </label>
            <label>
              <span>资源类型</span>
              <select v-model="uploadForm.kind">
                <option>视频</option>
                <option>文档</option>
                <option>图片</option>
                <option>链接</option>
              </select>
            </label>
          </div>
          <label>
            <span>资源链接</span>
            <input v-model.trim="uploadForm.link" placeholder="可填网盘、视频页或文档地址" />
          </label>
          <label>
            <span>本地文件</span>
            <input type="file" @change="handleFileUpload" />
          </label>
          <label>
            <span>简介</span>
            <textarea v-model.trim="uploadForm.description" rows="4" maxlength="260" placeholder="说明适用课程、亮点、使用方式"></textarea>
          </label>
          <label>
            <span>标签</span>
            <input v-model.trim="uploadForm.tags" placeholder="用逗号分隔，例如：考研, 速查, 模板" />
          </label>
          <p v-if="formError" class="share-form-error">{{ formError }}</p>
          <footer>
            <button type="button" @click="closeUpload">取消</button>
            <button class="share-submit" type="submit">
              <UploadCloud :size="18" />
              <span>发布资源</span>
            </button>
          </footer>
        </form>
      </section>
    </div>

    <p v-if="toast" class="share-toast">{{ toast }}</p>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  ChevronLeft,
  ChevronRight,
  Clapperboard,
  FileText,
  Heart,
  Image as ImageIcon,
  Link as LinkIcon,
  PlayCircle,
  Search,
  UploadCloud,
} from '@lucide/vue'
import homeAccentBook from '../../assets/home-accent-book.png'
import homeAccentFlower from '../../assets/home-accent-flower.png'
import homeAccentPencilNote from '../../assets/home-accent-pencil-note.png'
import homeBgDocs from '../../assets/home-bg-docs.png'
import homeBgPlan from '../../assets/home-bg-plan.png'
import homeBgPractice from '../../assets/home-bg-practice.png'
import {
  createSharedResource,
  listSharedResources,
  markSharedResourceViewed,
  resolveAssetUrl,
  toggleSharedResourceFavorite,
} from '../../api'

const emit = defineEmits(['back-home'])

const categories = [
  { id: 'all', label: '全部资源', color: '#5b8cff' },
  { id: '课程视频', label: '课程视频', color: '#ff6f91' },
  { id: '复习资料', label: '复习资料', color: '#35c5a8' },
  { id: '笔记模板', label: '笔记模板', color: '#f4bd4f' },
  { id: '工具软件', label: '工具软件', color: '#8a7cff' },
  { id: '学习经验', label: '学习经验', color: '#41b8e8' },
]

const uploadCategories = categories.filter((item) => item.id !== 'all').map((item) => item.id)
const fallbackCovers = [homeBgPractice, homeBgDocs, homeAccentPencilNote, homeAccentBook, homeBgPlan, homeAccentFlower]

const resources = ref([])
const activeCategory = ref('all')
const viewMode = ref('discover')
const keyword = ref('')
const featuredIndex = ref(0)
const uploadOpen = ref(false)
const selectedResource = ref(null)
const formError = ref('')
const toast = ref('')
const uploadForm = reactive({
  title: '',
  category: '复习资料',
  kind: '文档',
  link: '',
  file: null,
  coverDataUrl: '',
  description: '',
  tags: '',
})
const generatedVideoCovers = reactive({})

const filteredResources = computed(() => {
  const text = keyword.value.toLowerCase()
  const base = viewMode.value === 'favorites' ? favoriteResources.value : resources.value
  return base.filter((resource) => {
    const categoryMatched = activeCategory.value === 'all' || resource.category === activeCategory.value
    const keywordMatched = !text || [resource.title, resource.author, resource.category, resource.description, resource.tags.join(' ')].join(' ').toLowerCase().includes(text)
    return categoryMatched && keywordMatched
  })
})

const favoriteResources = computed(() => resources.value.filter((resource) => resource.favorite))
const featuredResource = computed(() => filteredResources.value[featuredIndex.value] || filteredResources.value[0] || null)
const heroPlayableUrl = computed(() => playableUrl(featuredResource.value))
const boardResources = computed(() => filteredResources.value.filter((item) => item.id !== featuredResource.value?.id).slice(0, 6))
const uploadCount = computed(() => resources.value.filter((resource) => resource.uploaded).length)
const trendingResources = computed(() => [...resources.value].slice(0, 5))

function countByCategory(categoryId) {
  const base = viewMode.value === 'favorites' ? favoriteResources.value : resources.value
  if (categoryId === 'all') return base.length
  return base.filter((resource) => resource.category === categoryId).length
}

function clampFeaturedIndex() {
  const lastIndex = Math.max(0, filteredResources.value.length - 1)
  featuredIndex.value = Math.min(Math.max(featuredIndex.value, 0), lastIndex)
}

function setFeaturedIndex(index) {
  const total = filteredResources.value.length
  if (!total) {
    featuredIndex.value = 0
    return
  }
  featuredIndex.value = ((index % total) + total) % total
}

function moveFeatured(step) {
  setFeaturedIndex(featuredIndex.value + step)
}

function coverStyle(resource) {
  if (resource.cover) {
    return { backgroundImage: `linear-gradient(180deg, rgba(14, 25, 58, 0.08), rgba(14, 25, 58, 0.44)), url("${resource.cover}")` }
  }
  const palettes = {
    blue: ['#5b8cff', '#8edcff', '#ffd8ef'],
    green: ['#36c6a9', '#b5efd4', '#fff1b8'],
    gold: ['#f4bd4f', '#ffdfe8', '#76d1ff'],
    violet: ['#817cff', '#d6c5ff', '#ffc5dc'],
    cyan: ['#41b8e8', '#a4f0e2', '#ffd572'],
  }
  const palette = palettes[resource.tone] || palettes.blue
  return {
    backgroundImage: `linear-gradient(135deg, ${palette[0]}, ${palette[1]} 58%, ${palette[2]})`,
  }
}

async function toggleFavorite(resourceId) {
  const resource = resources.value.find((item) => item.id === resourceId)
  if (!resource) return
  try {
    const response = await toggleSharedResourceFavorite(resourceId)
    const updated = normalizeShareResource(response.data?.data)
    replaceResource(updated)
    showToast(updated.favorite ? '已加入收藏页面' : '已取消收藏')
  } catch (exception) {
    showToast(exception.response?.data?.message || '收藏更新失败')
  }
}

async function selectResource(resource) {
  selectedResource.value = resource
  if (!resource?.id) return
  try {
    const response = await markSharedResourceViewed(resource.id)
    replaceResource(normalizeShareResource(response.data?.data))
  } catch {
    // 浏览计数失败不影响打开资源详情。
  }
}

function playableUrl(resource) {
  const link = `${resource?.link || ''}`.trim()
  if (/^(https?:\/\/|\/).+\.(mp4|webm|ogg|mov|m4v)(\?.*)?$/i.test(link)) return resolveAssetUrl(link)
  return ''
}

function openUpload() {
  uploadOpen.value = true
  formError.value = ''
}

function closeUpload() {
  uploadOpen.value = false
}

function resetUploadForm() {
  Object.assign(uploadForm, {
    title: '',
    category: '复习资料',
    kind: '文档',
    link: '',
    file: null,
    coverDataUrl: '',
    description: '',
    tags: '',
  })
}

async function handleFileUpload(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  uploadForm.file = file
  uploadForm.coverDataUrl = ''
  if (file.type.startsWith('video/')) {
    uploadForm.kind = '视频'
    try {
      uploadForm.coverDataUrl = await captureVideoCoverFromFile(file)
    } catch {
      showToast('视频封面截取失败，将使用播放器首帧')
    }
    return
  }
  if (file.type.startsWith('image/')) {
    uploadForm.kind = '图片'
  }
}

async function submitUpload() {
  formError.value = ''
  if (!uploadForm.title) {
    formError.value = '请填写资源标题'
    return
  }
  if (!uploadForm.link && !uploadForm.file) {
    formError.value = '请填写资源链接或选择一个本地文件'
    return
  }
  try {
    const response = await createSharedResource({
      title: uploadForm.title,
      category: uploadForm.category,
      kind: uploadForm.kind,
      linkUrl: uploadForm.link || null,
      description: uploadForm.description || null,
      tags: uploadForm.tags || null,
      coverDataUrl: uploadForm.coverDataUrl || null,
    }, uploadForm.file)
    const nextResource = normalizeShareResource(response.data?.data)
    resources.value = [nextResource, ...resources.value.filter((item) => item.id !== nextResource.id)]
    selectedResource.value = nextResource
    activeCategory.value = 'all'
    viewMode.value = 'discover'
    featuredIndex.value = 0
    closeUpload()
    resetUploadForm()
    showToast('资源已发布到分享广场')
  } catch (exception) {
    formError.value = exception.response?.data?.message || '资源发布失败'
  }
}

function showToast(message) {
  toast.value = message
  window.setTimeout(() => {
    toast.value = ''
  }, 1800)
}

async function loadResources() {
  try {
    const response = await listSharedResources()
    resources.value = (response.data?.data || []).map(normalizeShareResource)
  } catch (exception) {
    showToast(exception.response?.data?.message || '资源分享加载失败，请确认已登录')
  }
}

function normalizeShareResource(resource) {
  const index = resources.value.length
  const resolvedCover = resolveAssetUrl(resource.cover)
  const isVideo = (resource.kind || '文档') === '视频'
  const normalized = {
    id: resource.id,
    title: resource.title || '未命名资源',
    category: resource.category || '复习资料',
    kind: resource.kind || '文档',
    author: resource.author || '学习用户',
    description: resource.description || '这个资源还没有简介。',
    tags: Array.isArray(resource.tags) ? resource.tags : [],
    views: resource.views || 0,
    duration: resource.duration || resource.originalFilename || resource.kind || '资源',
    createdAt: formatDate(resource.createdAt),
    favorite: Boolean(resource.favorite),
    cover: resolvedCover || generatedVideoCovers[resource.id] || (isVideo ? '' : fallbackCovers[index % fallbackCovers.length]),
    hasRealCover: Boolean(resolvedCover),
    link: resolveAssetUrl(resource.link || resource.fileUrl),
    tone: ['blue', 'green', 'gold', 'violet', 'cyan'][index % 5],
    uploaded: Boolean(resource.uploaded),
  }
  ensureVideoCover(normalized)
  return normalized
}

async function captureVideoCoverFromFile(file) {
  const url = URL.createObjectURL(file)
  try {
    return await captureVideoCover(url)
  } finally {
    URL.revokeObjectURL(url)
  }
}

function captureVideoCover(src) {
  return new Promise((resolve, reject) => {
    const video = document.createElement('video')
    video.muted = true
    video.playsInline = true
    video.preload = 'metadata'
    video.crossOrigin = 'anonymous'
    const cleanup = () => {
      video.removeAttribute('src')
      video.load()
    }
    const fail = () => {
      cleanup()
      reject(new Error('video cover failed'))
    }
    video.onerror = fail
    video.onloadedmetadata = () => {
      const targetTime = Math.min(Math.max(0.1, (video.duration || 1) * 0.02), 1)
      video.currentTime = targetTime
    }
    video.onseeked = () => {
      try {
        const width = video.videoWidth || 1280
        const height = video.videoHeight || 720
        const maxWidth = 960
        const scale = Math.min(1, maxWidth / Math.max(1, width))
        const canvas = document.createElement('canvas')
        canvas.width = Math.max(1, Math.round(width * scale))
        canvas.height = Math.max(1, Math.round(height * scale))
        const context = canvas.getContext('2d')
        context.drawImage(video, 0, 0, canvas.width, canvas.height)
        const dataUrl = canvas.toDataURL('image/jpeg', 0.78)
        cleanup()
        resolve(dataUrl)
      } catch (error) {
        cleanup()
        reject(error)
      }
    }
    video.src = src
  })
}

function ensureVideoCover(resource) {
  if (!resource || resource.kind !== '视频' || resource.hasRealCover || !playableUrl(resource) || generatedVideoCovers[resource.id]) return
  captureVideoCover(playableUrl(resource))
    .then((cover) => {
      generatedVideoCovers[resource.id] = cover
      resource.cover = cover
    })
    .catch(() => {})
}

function replaceResource(updated) {
  resources.value = resources.value.map((item) => item.id === updated.id ? { ...item, ...updated } : item)
  if (selectedResource.value?.id === updated.id) {
    selectedResource.value = { ...selectedResource.value, ...updated }
  }
}

function formatDate(value) {
  if (!value) return '刚刚'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  const now = new Date()
  if (date.toDateString() === now.toDateString()) {
    return '今天'
  }
  return `${date.getMonth() + 1}/${date.getDate()}`
}

watch([filteredResources, activeCategory, viewMode, keyword], () => {
  clampFeaturedIndex()
})

onMounted(loadResources)
</script>

<style scoped>
.share-workspace {
  --share-blue: #326cff;
  --share-ink: #163472;
  --share-muted: #6d82b7;
  --share-line: rgba(73, 116, 221, 0.16);
  display: grid;
  grid-template-columns: 310px minmax(0, 1fr);
  min-height: 100vh;
  color: var(--share-ink);
  background:
    radial-gradient(circle at 18% 8%, rgba(91, 140, 255, 0.2), transparent 32%),
    radial-gradient(circle at 86% 12%, rgba(255, 201, 226, 0.32), transparent 28%),
    linear-gradient(135deg, #f3f8ff, #fff9fd 54%, #edf5ff);
}

.share-rail {
  position: sticky;
  top: 0;
  display: grid;
  height: 100vh;
  align-content: start;
  gap: 16px;
  padding: 22px;
  overflow: auto;
  border-right: 1px solid var(--share-line);
  background: rgba(255, 255, 255, 0.54);
  backdrop-filter: blur(18px);
}

.share-back,
.share-upload-button,
.share-tabs button,
.share-panel button,
.share-detail-actions button,
.share-submit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 42px;
  cursor: pointer;
  color: var(--share-blue);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.14);
  font-weight: 900;
}

.share-back {
  width: fit-content;
  padding: 0 13px;
}

.share-brand {
  display: grid;
  gap: 10px;
  padding: 24px 2px 12px;
}

.share-kicker,
.share-brand small {
  color: rgba(50, 108, 255, 0.72);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0;
}

.share-brand h1,
.share-topbar h2,
.share-bili-caption h3,
.share-video-copy h3,
.share-panel h3,
.share-detail-drawer h3,
.share-modal h3 {
  margin: 0;
  color: var(--share-ink);
}

.share-brand h1 {
  font-size: 34px;
}

.share-brand p,
.share-panel p,
.share-detail-drawer p {
  margin: 0;
  color: var(--share-muted);
  font-weight: 800;
  line-height: 1.7;
}

.share-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.share-stats article {
  display: grid;
  gap: 3px;
  padding: 14px 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.66);
  box-shadow: inset 0 0 0 1px var(--share-line);
}

.share-stats strong {
  color: var(--share-blue);
  font-size: 23px;
}

.share-stats span {
  color: var(--share-muted);
  font-size: 12px;
  font-weight: 900;
}

.share-categories {
  display: grid;
  gap: 8px;
}

.share-categories button {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 48px;
  padding: 0 12px;
  cursor: pointer;
  color: var(--share-muted);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.52);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.1);
  text-align: left;
}

.share-categories button.active,
.share-categories button:hover {
  color: var(--share-blue);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 12px 24px rgba(79, 136, 255, 0.12), inset 0 0 0 1px rgba(73, 116, 221, 0.16);
}

.share-categories span {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.share-main {
  display: grid;
  align-content: start;
  gap: 16px;
  min-width: 0;
  padding: 24px;
}

.share-topbar {
  display: grid;
  grid-template-columns: minmax(170px, auto) minmax(240px, 1fr) auto auto;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.74);
  box-shadow: 0 14px 34px rgba(82, 108, 179, 0.12), inset 0 0 0 1px var(--share-line);
}

.share-topbar h2 {
  margin-top: 4px;
  font-size: 25px;
  line-height: 1;
}

.share-upload-button,
.share-submit {
  min-width: 124px;
  color: #fff;
  background: linear-gradient(100deg, #326cff, #56c7e8, #ff7eac);
  box-shadow: 0 14px 28px rgba(79, 136, 255, 0.18);
}

.share-search {
  display: flex;
  width: 100%;
  min-height: 46px;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  color: var(--share-blue);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.74);
  box-shadow: inset 0 0 0 1px var(--share-line);
}

.share-search input {
  width: 100%;
  min-width: 0;
  border: 0;
  outline: 0;
  color: var(--share-ink);
  background: transparent;
  font-weight: 900;
}

.share-tabs {
  display: flex;
  gap: 8px;
}

.share-tabs button {
  padding: 0 14px;
}

.share-tabs button.active {
  color: #fff;
  background: linear-gradient(100deg, #326cff, #56c7e8);
}

.share-category-strip {
  display: flex;
  gap: 9px;
  overflow-x: auto;
  padding: 2px;
}

.share-category-strip button {
  flex: 0 0 auto;
  min-height: 36px;
  padding: 0 13px;
  cursor: pointer;
  color: var(--share-muted);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.58);
  box-shadow: inset 0 0 0 1px rgba(73, 116, 221, 0.1);
  font-weight: 900;
}

.share-category-strip button.active,
.share-category-strip button:hover {
  color: #fff;
  background: linear-gradient(100deg, #326cff, #56c7e8);
}

.share-video-board {
  display: grid;
  grid-template-columns: minmax(360px, 0.9fr) minmax(0, 1.35fr);
  gap: 16px;
  align-items: stretch;
  min-height: 420px;
}

.share-bili-hero,
.share-cover,
.share-detail-cover {
  position: relative;
  display: grid;
  place-items: center;
  overflow: hidden;
  color: rgba(255, 255, 255, 0.92);
  border-radius: 8px;
  background-position: center;
  background-size: cover;
}

.share-bili-hero {
  min-height: 420px;
  cursor: pointer;
  box-shadow: 0 18px 42px rgba(68, 90, 152, 0.18);
}

.share-bili-hero.playing {
  background: #101827 !important;
  cursor: default;
}

.share-hero-player {
  position: absolute;
  inset: 0;
  z-index: 1;
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #101827;
}

.share-bili-hero::after {
  position: absolute;
  inset: auto 0 0;
  height: 46%;
  pointer-events: none;
  content: "";
  background: linear-gradient(180deg, transparent, rgba(15, 28, 62, 0.78));
  z-index: 1;
}

.share-category-pill,
.share-duration {
  position: absolute;
  display: inline-grid;
  min-height: 26px;
  place-items: center;
  padding: 0 9px;
  color: #fff;
  border-radius: 6px;
  background: rgba(22, 52, 114, 0.72);
  font-size: 12px;
  font-weight: 900;
}

.share-category-pill {
  z-index: 3;
  top: 10px;
  left: 10px;
}

.share-duration {
  right: 10px;
  bottom: 10px;
}

.share-hero-heart {
  position: absolute;
  z-index: 3;
  top: 10px;
  right: 10px;
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  cursor: pointer;
  color: #ff6f91;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 10px 22px rgba(16, 28, 62, 0.16);
}

.share-hero-arrows {
  position: absolute;
  right: 18px;
  bottom: 22px;
  z-index: 4;
  display: flex;
  gap: 10px;
}

.share-hero-arrows button {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  cursor: pointer;
  color: #fff;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.18);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(10px);
}

.share-hero-arrows button:hover {
  background: rgba(255, 255, 255, 0.28);
}

.share-bili-caption {
  position: absolute;
  z-index: 3;
  inset: auto 0 0;
  display: grid;
  gap: 8px;
  padding: 0 20px 18px;
}

.share-bili-caption h3 {
  max-width: 92%;
  color: #fff;
  font-size: 24px;
  line-height: 1.35;
}

.share-bili-caption p {
  margin: 0;
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
  font-weight: 900;
}

.share-dots {
  display: flex;
  gap: 7px;
}

.share-dots button {
  width: 9px;
  height: 9px;
  padding: 0;
  cursor: pointer;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.38);
}

.share-dots button.active {
  width: 18px;
  border-radius: 999px;
  background: #fff;
}

.share-board-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(160px, 1fr));
  gap: 16px;
}

.share-video-card {
  display: grid;
  grid-template-rows: auto minmax(72px, auto);
  gap: 9px;
  min-width: 0;
}

.share-cover {
  width: 100%;
  aspect-ratio: 16 / 9;
  min-height: auto;
  cursor: pointer;
  border: 0;
}

.share-cover::after {
  position: absolute;
  inset: auto 0 0;
  height: 48%;
  content: "";
  background: linear-gradient(180deg, transparent, rgba(12, 25, 58, 0.66));
}

.share-cover > svg {
  filter: drop-shadow(0 8px 18px rgba(12, 25, 58, 0.26));
}

.share-cover-stats {
  position: absolute;
  z-index: 2;
  left: 9px;
  bottom: 9px;
  display: flex;
  gap: 8px;
  color: #fff;
  font-size: 12px;
  font-weight: 900;
}

.share-cover-stats span {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.share-video-copy {
  position: relative;
  min-width: 0;
  padding-right: 38px;
}

.share-video-copy h3,
.share-video-copy p {
  margin: 0;
}

.share-video-copy h3 {
  display: -webkit-box;
  overflow: hidden;
  cursor: pointer;
  color: var(--share-ink);
  font-size: 15px;
  line-height: 1.42;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.share-video-copy p,
.share-ranking small {
  color: var(--share-muted);
  font-size: 12px;
  font-weight: 900;
}

.share-video-copy p {
  margin-top: 5px;
}

.share-detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.share-detail-actions button {
  padding: 0 14px;
}

.share-detail-actions a {
  display: inline-grid;
  min-height: 42px;
  place-items: center;
  padding: 0 14px;
  color: #fff;
  border-radius: 8px;
  background: #163472;
  text-decoration: none;
  font-weight: 900;
}

.share-heart {
  position: absolute;
  top: 0;
  right: 0;
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  cursor: pointer;
  color: #ff5f91;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: inset 0 0 0 1px var(--share-line);
}

.share-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.share-tags span {
  display: inline-grid;
  min-height: 24px;
  place-items: center;
  padding: 0 8px;
  color: var(--share-blue);
  border-radius: 6px;
  background: rgba(50, 108, 255, 0.08);
  font-size: 12px;
  font-weight: 900;
}

.share-lower-layout,
.share-panel {
  display: grid;
  gap: 12px;
}

.share-lower-layout {
  grid-template-columns: minmax(0, 0.9fr) minmax(280px, 1.1fr);
  align-items: start;
}

.share-panel {
  padding: 16px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.7);
  box-shadow: inset 0 0 0 1px var(--share-line);
}

.share-panel button {
  width: 100%;
}

.share-collection-panel {
  grid-template-columns: minmax(0, 1fr) 170px;
  align-items: center;
}

.share-ranking article {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 9px;
  align-items: start;
  padding: 9px 0;
  cursor: pointer;
  border-bottom: 1px dashed var(--share-line);
}

.share-ranking article:last-child {
  border-bottom: 0;
}

.share-ranking b {
  color: #ff6f91;
  font-size: 20px;
}

.share-ranking strong {
  display: block;
  overflow: hidden;
  color: var(--share-ink);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.share-empty {
  margin: 0;
  padding: 20px;
  color: var(--share-muted);
  border: 1px dashed var(--share-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.56);
  font-weight: 900;
}

.share-detail-drawer {
  position: fixed;
  right: 22px;
  bottom: 22px;
  z-index: 900;
  display: grid;
  grid-template-columns: 180px minmax(0, 300px);
  gap: 14px;
  width: min(560px, calc(100vw - 44px));
  padding: 14px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 26px 70px rgba(38, 58, 118, 0.25), inset 0 0 0 1px var(--share-line);
  backdrop-filter: blur(16px);
}

.share-detail-player {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 9;
  object-fit: contain;
  border-radius: 8px;
  background: #101827;
}

.share-detail-drawer > button {
  position: absolute;
  top: 8px;
  right: 8px;
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  cursor: pointer;
  color: var(--share-blue);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
  font-weight: 900;
}

.share-detail-cover {
  min-height: 150px;
}

.share-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  padding: 18px;
  background: rgba(20, 34, 70, 0.28);
  backdrop-filter: blur(10px);
}

.share-modal {
  width: min(680px, 100%);
  max-height: min(92vh, 820px);
  overflow: auto;
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(247, 251, 255, 0.96));
  box-shadow: 0 26px 70px rgba(38, 58, 118, 0.25), inset 0 0 0 1px var(--share-line);
}

.share-modal header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px 14px;
  border-bottom: 1px solid var(--share-line);
}

.share-modal header > button {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  cursor: pointer;
  color: var(--share-blue);
  border-radius: 8px;
  background: rgba(50, 108, 255, 0.08);
  font-size: 18px;
  font-weight: 900;
}

.share-form {
  display: grid;
  gap: 13px;
  padding: 18px 22px 22px;
}

.share-form label {
  display: grid;
  gap: 7px;
}

.share-form label > span {
  color: var(--share-muted);
  font-size: 12px;
  font-weight: 900;
}

.share-form input,
.share-form select,
.share-form textarea {
  width: 100%;
  min-width: 0;
  border: 0;
  border-radius: 8px;
  outline: 0;
  color: var(--share-ink);
  background: rgba(255, 255, 255, 0.86);
  box-shadow: inset 0 0 0 1px var(--share-line);
  font: inherit;
  font-weight: 800;
}

.share-form input,
.share-form select {
  min-height: 42px;
  padding: 0 11px;
}

.share-form textarea {
  resize: vertical;
  padding: 11px;
  line-height: 1.6;
}

.share-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.share-form footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.share-form footer > button {
  min-height: 40px;
  padding: 0 14px;
  cursor: pointer;
  color: var(--share-blue);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: inset 0 0 0 1px var(--share-line);
  font-weight: 900;
}

.share-form footer > .share-submit {
  color: #fff;
}

.share-form-error {
  margin: 0;
  color: #d9415f;
  font-size: 13px;
  font-weight: 900;
}

.share-toast {
  position: fixed;
  right: 22px;
  bottom: 22px;
  z-index: 1200;
  margin: 0;
  padding: 12px 16px;
  color: #fff;
  border-radius: 8px;
  background: linear-gradient(100deg, #326cff, #56c7e8, #ff7eac);
  box-shadow: 0 18px 38px rgba(79, 136, 255, 0.24);
  font-weight: 900;
}

@media (max-width: 1220px) {
  .share-video-board {
    grid-template-columns: 1fr;
    min-height: 0;
  }

  .share-bili-hero {
    min-height: 360px;
  }

  .share-board-grid {
    grid-template-columns: repeat(3, minmax(190px, 1fr));
  }
}

@media (max-width: 860px) {
  .share-workspace,
  .share-topbar,
  .share-lower-layout,
  .share-collection-panel {
    grid-template-columns: 1fr;
  }

  .share-rail {
    position: static;
    height: auto;
  }

  .share-topbar {
    align-items: stretch;
  }

  .share-board-grid,
  .share-form-grid,
  .share-detail-drawer {
    grid-template-columns: 1fr;
  }

  .share-bili-hero {
    min-height: 280px;
  }

  .share-hero-arrows {
    right: 12px;
    bottom: 14px;
  }

  .share-hero-arrows button {
    width: 36px;
    height: 36px;
  }
}
</style>
