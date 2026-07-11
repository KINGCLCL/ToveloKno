<template>
  <div class="forum-workspace">
    <aside class="forum-rail">
      <button class="forum-back" type="button" @click="emit('back-home')">
        <ChevronLeft :size="18" />
        <strong>学习控制台</strong>
      </button>

      <section class="forum-brand">
        <small>BOARD / DISCUSS</small>
        <h1>学习论坛</h1>
        <p>按版块发帖、回帖和收藏主题，适合约资料、问问题、整理经验贴。</p>
      </section>

      <section class="forum-stats">
        <article>
          <strong>{{ threads.length }}</strong>
          <span>主题</span>
        </article>
        <article>
          <strong>{{ totalReplies }}</strong>
          <span>回复</span>
        </article>
        <article>
          <strong>{{ favoriteCount }}</strong>
          <span>收藏</span>
        </article>
      </section>

      <section class="forum-boards" aria-label="论坛版块">
        <button
          v-for="board in boards"
          :key="board.id"
          type="button"
          :class="{ active: activeBoard === board.id }"
          @click="setBoard(board.id)"
        >
          <span>{{ board.code }}</span>
          <strong>{{ board.name }}</strong>
          <b>{{ countByBoard(board.id) }}</b>
        </button>
      </section>
    </aside>

    <main class="forum-main">
      <header class="forum-topbar">
        <div>
          <span class="forum-kicker">NGA STYLE BOARD</span>
          <h2>{{ activeBoardMeta.name }}</h2>
        </div>
        <label class="forum-search">
          <Search :size="17" />
          <input v-model.trim="keyword" type="search" placeholder="搜索主题、正文、作者" />
        </label>
        <div class="forum-sort">
          <button type="button" :class="{ active: sortMode === 'latest' }" @click="sortMode = 'latest'">最后回复</button>
          <button type="button" :class="{ active: sortMode === 'hot' }" @click="sortMode = 'hot'">热度</button>
          <button type="button" :class="{ active: sortMode === 'favorite' }" @click="sortMode = 'favorite'">收藏</button>
        </div>
        <button class="forum-new-button" type="button" @click="openComposer">
          <Plus :size="19" />
          <span>发表主题</span>
        </button>
      </header>

      <section class="forum-layout">
        <section class="forum-thread-panel">
          <div class="forum-thread-head">
            <span>主题</span>
            <span>作者</span>
            <span>回复/赞</span>
            <span>最后回复</span>
          </div>
          <button
            v-for="thread in filteredThreads"
            :key="thread.id"
            type="button"
            class="forum-thread-row"
            :class="{ active: selectedThread?.id === thread.id, pinned: thread.pinned }"
            @click="selectThread(thread.id)"
          >
            <span class="forum-thread-title">
              <em v-if="thread.pinned">置顶</em>
              <b>{{ thread.title }}</b>
              <small>{{ boardName(thread.boardId) }} · {{ thread.tags.join(' / ') }}</small>
            </span>
            <span class="forum-thread-author">
              <strong>{{ thread.author }}</strong>
              <small>{{ thread.createdAt }}</small>
            </span>
            <span class="forum-thread-counts">
              <b>{{ thread.replies.length }}</b>
              <small>{{ thread.likes }}</small>
            </span>
            <span class="forum-thread-last">
              <strong>{{ lastReplyAuthor(thread) }}</strong>
              <small>{{ lastActivity(thread) }}</small>
            </span>
          </button>
          <p v-if="!filteredThreads.length" class="forum-empty">没有匹配的主题，换个版块或关键词试试。</p>
        </section>

        <aside class="forum-detail">
          <template v-if="selectedThread">
            <header class="forum-detail-head">
              <div>
                <span class="forum-kicker">{{ boardName(selectedThread.boardId) }}</span>
                <h3>{{ selectedThread.title }}</h3>
                <p>{{ selectedThread.author }} · {{ selectedThread.createdAt }} · {{ selectedThread.views }} 阅读</p>
              </div>
              <div class="forum-detail-actions">
                <button type="button" :class="{ active: selectedThread.favorite }" @click="toggleFavorite(selectedThread.id)">
                  <Star :size="17" :fill="selectedThread.favorite ? 'currentColor' : 'none'" />
                </button>
                <button type="button" @click="likeThread(selectedThread.id)">
                  <ThumbsUp :size="17" />
                  <span>{{ selectedThread.likes }}</span>
                </button>
              </div>
            </header>

            <article class="forum-post">
              <header>
                <span class="forum-avatar">{{ selectedThread.author.slice(0, 1) }}</span>
                <div>
                  <strong>{{ selectedThread.author }}</strong>
                  <small>楼主 / 1 楼</small>
                </div>
              </header>
              <p>{{ selectedThread.content }}</p>
              <footer>
                <span v-for="tag in selectedThread.tags" :key="tag">{{ tag }}</span>
              </footer>
            </article>

            <section class="forum-replies">
              <article v-for="(reply, index) in selectedThread.replies" :key="reply.id" class="forum-reply">
                <header>
                  <span class="forum-avatar soft">{{ reply.author.slice(0, 1) }}</span>
                  <div>
                    <strong>{{ reply.author }}</strong>
                    <small>{{ index + 2 }} 楼 · {{ reply.createdAt }}</small>
                  </div>
                </header>
                <p>{{ reply.content }}</p>
              </article>
            </section>

            <form class="forum-reply-box" @submit.prevent="submitReply">
              <textarea v-model.trim="replyDraft" rows="4" maxlength="500" placeholder="回复这个主题，像论坛楼层一样追加到下方"></textarea>
              <footer>
                <span>{{ replyDraft.length }}/500</span>
                <button type="submit" :disabled="!replyDraft">发表回复</button>
              </footer>
            </form>
          </template>

          <p v-else class="forum-empty detail-empty">选择一个主题查看楼层和回复。</p>
        </aside>
      </section>
    </main>

    <div v-if="composerOpen" class="forum-modal-backdrop" @click.self="closeComposer">
      <section class="forum-modal" role="dialog" aria-modal="true" aria-label="发表主题">
        <header>
          <div>
            <span class="forum-kicker">NEW THREAD</span>
            <h3>发表主题</h3>
          </div>
          <button type="button" aria-label="关闭" @click="closeComposer">×</button>
        </header>
        <form class="forum-form" @submit.prevent="submitThread">
          <label>
            <span>标题 *</span>
            <input v-model.trim="threadDraft.title" required maxlength="80" placeholder="例如：高数二轮复习怎么安排？" />
          </label>
          <div class="forum-form-grid">
            <label>
              <span>版块</span>
              <select v-model="threadDraft.boardId">
                <option v-for="board in boards.filter((item) => item.id !== 'all')" :key="board.id" :value="board.id">
                  {{ board.name }}
                </option>
              </select>
            </label>
          </div>
          <label>
            <span>标签</span>
            <input v-model.trim="threadDraft.tags" maxlength="80" placeholder="用逗号分隔，例如：求助, 资料, 经验" />
          </label>
          <label>
            <span>正文 *</span>
            <textarea v-model.trim="threadDraft.content" required rows="7" maxlength="1200" placeholder="写下问题、经验或资源说明"></textarea>
          </label>
          <p v-if="formError" class="forum-form-error">{{ formError }}</p>
          <footer>
            <button type="button" @click="closeComposer">取消</button>
            <button type="submit">发布</button>
          </footer>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  ChevronLeft,
  Plus,
  Search,
  Star,
  ThumbsUp,
} from '@lucide/vue'
import {
  createForumReply,
  createForumThread,
  listForumThreads,
  markForumThreadViewed,
  toggleForumThreadFavorite,
  toggleForumThreadLike,
} from '../../api'

const emit = defineEmits(['back-home'])

const boards = [
  { id: 'all', code: 'ALL', name: '全部版块' },
  { id: 'qa', code: 'Q&A', name: '学习求助' },
  { id: 'resource', code: 'RES', name: '资料互助' },
  { id: 'experience', code: 'EXP', name: '经验攻略' },
  { id: 'chat', code: 'CHAT', name: '自习茶馆' },
]

const threads = ref([])
const activeBoard = ref('all')
const keyword = ref('')
const sortMode = ref('latest')
const selectedThreadId = ref(null)
const composerOpen = ref(false)
const replyDraft = ref('')
const formError = ref('')
const threadDraft = reactive({
  title: '',
  boardId: 'qa',
  tags: '',
  content: '',
})

const activeBoardMeta = computed(() => boards.find((board) => board.id === activeBoard.value) || boards[0])
const totalReplies = computed(() => threads.value.reduce((sum, thread) => sum + thread.replies.length, 0))
const favoriteCount = computed(() => threads.value.filter((thread) => thread.favorite).length)
const selectedThread = computed(() => threads.value.find((thread) => thread.id === selectedThreadId.value) || null)

const filteredThreads = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  const next = threads.value.filter((thread) => {
    const boardMatched = activeBoard.value === 'all' || thread.boardId === activeBoard.value
    const textMatched = !text || [
      thread.title,
      thread.author,
      thread.content,
      thread.tags.join(' '),
    ].join(' ').toLowerCase().includes(text)
    const favoriteMatched = sortMode.value !== 'favorite' || thread.favorite
    return boardMatched && textMatched && favoriteMatched
  })
  return next.sort((first, second) => {
    if (first.pinned !== second.pinned) return first.pinned ? -1 : 1
    if (sortMode.value === 'hot') return threadHeat(second) - threadHeat(first)
    return activityStamp(second) - activityStamp(first)
  })
})

watch(filteredThreads, (next) => {
  if (!next.length) {
    selectedThreadId.value = null
    return
  }
  if (!next.some((thread) => thread.id === selectedThreadId.value)) {
    selectedThreadId.value = next[0].id
  }
})

onMounted(loadThreads)

async function loadThreads() {
  try {
    const response = await listForumThreads()
    threads.value = (response.data?.data || []).map(normalizeThread)
    selectedThreadId.value = filteredThreads.value[0]?.id || threads.value[0]?.id || null
  } catch {
    formError.value = '论坛加载失败，请确认已登录'
  }
}

function normalizeThread(thread) {
  return {
    id: thread?.id,
    boardId: boards.some((board) => board.id === thread?.boardId) && thread.boardId !== 'all' ? thread.boardId : 'qa',
    title: String(thread?.title || '未命名主题'),
    author: String(thread?.author || '学习用户'),
    content: String(thread?.content || '这个主题还没有正文。'),
    tags: Array.isArray(thread?.tags) && thread.tags.length ? thread.tags.map(String) : ['讨论'],
    createdAt: formatForumTime(thread?.createdAt),
    updatedAt: formatForumTime(thread?.updatedAt || thread?.createdAt),
    views: safeNumber(thread?.views),
    likes: safeNumber(thread?.likes),
    favorite: Boolean(thread?.favorite),
    liked: Boolean(thread?.liked),
    pinned: Boolean(thread?.pinned),
    replies: Array.isArray(thread?.replies)
      ? thread.replies.map((reply) => ({
        id: reply?.id,
        author: String(reply?.author || '学习用户'),
        content: String(reply?.content || ''),
        createdAt: formatForumTime(reply?.createdAt),
      }))
      : [],
  }
}

function safeNumber(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number : 0
}

function setBoard(boardId) {
  activeBoard.value = boardId
}

function countByBoard(boardId) {
  if (boardId === 'all') return threads.value.length
  return threads.value.filter((thread) => thread.boardId === boardId).length
}

function boardName(boardId) {
  return boards.find((board) => board.id === boardId)?.name || '学习论坛'
}

async function selectThread(threadId) {
  selectedThreadId.value = threadId
  try {
    const response = await markForumThreadViewed(threadId)
    replaceThread(normalizeThread(response.data?.data))
  } catch {
    // 浏览计数失败不影响阅读。
  }
}

function openComposer() {
  formError.value = ''
  threadDraft.boardId = activeBoard.value === 'all' ? 'qa' : activeBoard.value
  composerOpen.value = true
}

function closeComposer() {
  composerOpen.value = false
  formError.value = ''
}

async function submitThread() {
  if (!threadDraft.title || !threadDraft.content) {
    formError.value = '标题和正文都要填写'
    return
  }
  try {
    const response = await createForumThread({
      title: threadDraft.title,
      boardId: threadDraft.boardId,
      content: threadDraft.content,
      tags: threadDraft.tags || null,
    })
    const thread = normalizeThread(response.data?.data)
    threads.value = [thread, ...threads.value.filter((item) => item.id !== thread.id)]
    selectedThreadId.value = thread.id
    activeBoard.value = thread.boardId
    Object.assign(threadDraft, { title: '', boardId: 'qa', tags: '', content: '' })
    closeComposer()
  } catch (exception) {
    formError.value = exception.response?.data?.message || '主题发布失败'
  }
}

async function submitReply() {
  if (!selectedThread.value || !replyDraft.value) return
  try {
    const response = await createForumReply(selectedThread.value.id, { content: replyDraft.value })
    replaceThread(normalizeThread(response.data?.data))
    replyDraft.value = ''
  } catch (exception) {
    formError.value = exception.response?.data?.message || '回复失败'
  }
}

async function toggleFavorite(threadId) {
  try {
    const response = await toggleForumThreadFavorite(threadId)
    replaceThread(normalizeThread(response.data?.data))
  } catch (exception) {
    formError.value = exception.response?.data?.message || '收藏更新失败'
  }
}

async function likeThread(threadId) {
  try {
    const response = await toggleForumThreadLike(threadId)
    replaceThread(normalizeThread(response.data?.data))
  } catch (exception) {
    formError.value = exception.response?.data?.message || '点赞失败'
  }
}

function splitTags(text) {
  const tags = text.split(/[，,]/).map((item) => item.trim()).filter(Boolean)
  return tags.length ? tags.slice(0, 4) : ['讨论']
}

function lastReplyAuthor(thread) {
  const last = thread.replies[thread.replies.length - 1]
  return last?.author || thread.author
}

function lastActivity(thread) {
  const last = thread.replies[thread.replies.length - 1]
  return last?.createdAt || thread.updatedAt
}

function activityStamp(thread) {
  return Number(thread.id) || 0
}

function threadHeat(thread) {
  return thread.views + thread.likes * 8 + thread.replies.length * 16
}

function replaceThread(updated) {
  threads.value = threads.value.map((thread) => thread.id === updated.id ? { ...thread, ...updated } : thread)
}

function formatForumTime(value) {
  if (!value) return '刚刚'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  const now = new Date()
  const pad = (item) => String(item).padStart(2, '0')
  if (date.toDateString() === now.toDateString()) {
    return `今天 ${pad(date.getHours())}:${pad(date.getMinutes())}`
  }
  return `${date.getMonth() + 1}/${date.getDate()} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
</script>

<style scoped>
.forum-workspace {
  --forum-blue: #4f88ff;
  --forum-ink: #163b83;
  --forum-muted: #7380a5;
  --forum-line: rgba(96, 132, 214, 0.16);
  display: grid;
  grid-template-columns: 310px minmax(0, 1fr);
  min-height: 100vh;
  color: var(--forum-ink);
  background:
    radial-gradient(circle at 10% 12%, rgba(157, 183, 255, 0.34), transparent 30%),
    radial-gradient(circle at 88% 8%, rgba(255, 210, 238, 0.34), transparent 28%),
    linear-gradient(135deg, #f7fbff 0%, #fff8fd 48%, #eef5ff 100%);
}

.forum-rail,
.forum-main,
.forum-detail,
.forum-modal {
  position: relative;
  z-index: 1;
}

.forum-rail {
  display: grid;
  align-content: start;
  gap: 22px;
  padding: 28px 18px 30px;
  border-right: 1px solid rgba(110, 144, 218, 0.2);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.76), rgba(246, 250, 255, 0.42));
  backdrop-filter: blur(18px);
}

.forum-back,
.forum-new-button,
.forum-sort button,
.forum-detail-actions button,
.forum-reply-box button,
.forum-modal button {
  border: 0;
  cursor: pointer;
  font: inherit;
}

.forum-back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: max-content;
  min-height: 38px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--forum-ink);
  font-weight: 900;
  box-shadow: 0 10px 24px rgba(77, 103, 180, 0.08);
}

.forum-brand {
  display: grid;
  gap: 10px;
  padding: 18px;
}

.forum-brand small,
.forum-kicker {
  color: rgba(79, 136, 255, 0.82);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.forum-brand h1 {
  margin: 0;
  font-size: 34px;
}

.forum-brand p {
  margin: 0;
  color: var(--forum-muted);
  line-height: 1.8;
}

.forum-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.forum-stats article {
  display: grid;
  gap: 4px;
  min-height: 76px;
  place-items: center;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.6);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.forum-stats strong {
  font-size: 24px;
}

.forum-stats span {
  color: var(--forum-muted);
  font-size: 12px;
  font-weight: 900;
}

.forum-boards {
  display: grid;
  gap: 12px;
}

.forum-boards button {
  position: relative;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 34px;
  align-items: center;
  gap: 12px;
  min-height: 62px;
  padding: 0 14px;
  border: 1px solid var(--forum-line);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.56);
  color: #6d7ba0;
  text-align: left;
  transition: transform 0.18s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.forum-boards button.active {
  color: var(--forum-blue);
  transform: translateX(6px);
  background: linear-gradient(100deg, rgba(79, 136, 255, 0.14), rgba(111, 213, 255, 0.12), rgba(240, 166, 223, 0.16));
  box-shadow: 0 18px 36px rgba(79, 136, 255, 0.14);
}

.forum-boards span {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 16px;
  background: rgba(235, 241, 255, 0.88);
  font-size: 12px;
  font-weight: 900;
}

.forum-boards strong,
.forum-boards b {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.forum-main {
  display: grid;
  align-content: start;
  gap: 18px;
  padding: 28px;
}

.forum-topbar {
  display: grid;
  grid-template-columns: minmax(210px, 0.8fr) minmax(260px, 1fr) auto auto;
  gap: 14px;
  align-items: center;
}

.forum-topbar h2 {
  margin: 4px 0 0;
  font-size: clamp(28px, 3vw, 42px);
}

.forum-search {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 46px;
  padding: 0 14px;
  border: 1px solid var(--forum-line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
}

.forum-search input,
.forum-form input,
.forum-form select,
.forum-form textarea,
.forum-reply-box textarea {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--forum-ink);
  font: inherit;
}

.forum-sort {
  display: inline-flex;
  gap: 6px;
  padding: 5px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.62);
}

.forum-sort button {
  min-height: 36px;
  padding: 0 12px;
  border-radius: 999px;
  background: transparent;
  color: var(--forum-muted);
  font-weight: 900;
}

.forum-sort button.active,
.forum-new-button,
.forum-reply-box button,
.forum-form footer button:last-child {
  background: linear-gradient(100deg, var(--forum-blue), #6fd5ff, #f0a6df);
  color: #fff;
  box-shadow: 0 14px 30px rgba(79, 136, 255, 0.18);
}

.forum-new-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 46px;
  padding: 0 18px;
  border-radius: 999px;
  font-weight: 900;
}

.forum-layout {
  display: grid;
  grid-template-columns: minmax(520px, 1fr) minmax(360px, 0.62fr);
  gap: 18px;
  min-height: calc(100vh - 112px);
}

.forum-thread-panel,
.forum-detail {
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.74);
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.62);
  box-shadow: 0 24px 58px rgba(77, 103, 180, 0.1);
  backdrop-filter: blur(18px);
}

.forum-thread-head,
.forum-thread-row {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) 150px 84px 150px;
  gap: 12px;
  align-items: center;
}

.forum-thread-head {
  min-height: 48px;
  padding: 0 18px;
  color: #7a87aa;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
  border-bottom: 1px solid var(--forum-line);
}

.forum-thread-row {
  width: 100%;
  min-height: 88px;
  padding: 12px 18px;
  border: 0;
  border-bottom: 1px solid rgba(96, 132, 214, 0.1);
  background: transparent;
  color: var(--forum-ink);
  text-align: left;
}

.forum-thread-row:hover,
.forum-thread-row.active {
  background: linear-gradient(90deg, rgba(232, 240, 255, 0.96), rgba(255, 255, 255, 0.42));
}

.forum-thread-title,
.forum-thread-author,
.forum-thread-counts,
.forum-thread-last {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.forum-thread-title b,
.forum-thread-author strong,
.forum-thread-last strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.forum-thread-title em {
  width: max-content;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(79, 136, 255, 0.12);
  color: var(--forum-blue);
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.forum-thread-title small,
.forum-thread-author small,
.forum-thread-counts small,
.forum-thread-last small,
.forum-detail-head p,
.forum-post small,
.forum-reply small {
  color: var(--forum-muted);
  font-size: 12px;
}

.forum-thread-counts {
  text-align: center;
}

.forum-detail {
  display: grid;
  align-content: start;
  max-height: calc(100vh - 112px);
  overflow-y: auto;
}

.forum-detail-head {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 22px;
  border-bottom: 1px solid var(--forum-line);
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(16px);
}

.forum-detail-head h3 {
  margin: 6px 0 8px;
  font-size: 22px;
  line-height: 1.35;
}

.forum-detail-actions {
  display: flex;
  gap: 8px;
}

.forum-detail-actions button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-width: 42px;
  height: 38px;
  justify-content: center;
  border-radius: 14px;
  background: rgba(235, 241, 255, 0.86);
  color: var(--forum-blue);
  font-weight: 900;
}

.forum-detail-actions button.active {
  background: rgba(255, 232, 244, 0.9);
  color: #df6aa6;
}

.forum-post,
.forum-reply {
  display: grid;
  gap: 14px;
  padding: 22px;
  border-bottom: 1px solid rgba(96, 132, 214, 0.1);
}

.forum-post header,
.forum-reply header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.forum-avatar {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--forum-blue), #9fc7ff);
  color: #fff;
  font-weight: 900;
}

.forum-avatar.soft {
  background: rgba(235, 241, 255, 0.96);
  color: var(--forum-blue);
}

.forum-post p,
.forum-reply p {
  margin: 0;
  color: #304775;
  line-height: 1.85;
  white-space: pre-wrap;
}

.forum-post footer {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.forum-post footer span {
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(79, 136, 255, 0.09);
  color: var(--forum-blue);
  font-size: 12px;
  font-weight: 900;
}

.forum-reply-box {
  display: grid;
  gap: 10px;
  margin: 18px;
  padding: 14px;
  border: 1px solid var(--forum-line);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.72);
}

.forum-reply-box footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--forum-muted);
  font-size: 12px;
  font-weight: 900;
}

.forum-reply-box button {
  min-height: 38px;
  padding: 0 16px;
  border-radius: 999px;
  font-weight: 900;
}

.forum-reply-box button:disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

.forum-empty {
  margin: 24px;
  color: var(--forum-muted);
  font-weight: 800;
}

.detail-empty {
  align-self: center;
  justify-self: center;
}

.forum-modal-backdrop {
  position: fixed;
  z-index: 80;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(20, 38, 82, 0.24);
  backdrop-filter: blur(12px);
}

.forum-modal {
  display: grid;
  gap: 18px;
  width: min(720px, 100%);
  max-height: min(760px, calc(100vh - 48px));
  overflow: auto;
  padding: 24px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 34px 80px rgba(43, 70, 140, 0.2);
}

.forum-modal > header,
.forum-form footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.forum-modal h3 {
  margin: 4px 0 0;
  font-size: 26px;
}

.forum-modal > header button,
.forum-form footer button:first-child {
  min-width: 40px;
  min-height: 40px;
  border-radius: 14px;
  background: rgba(235, 241, 255, 0.9);
  color: var(--forum-ink);
  font-weight: 900;
}

.forum-form {
  display: grid;
  gap: 14px;
}

.forum-form label {
  display: grid;
  gap: 8px;
}

.forum-form span {
  color: var(--forum-muted);
  font-size: 13px;
  font-weight: 900;
}

.forum-form input,
.forum-form select,
.forum-form textarea {
  min-height: 44px;
  padding: 0 12px;
  border: 1px solid var(--forum-line);
  border-radius: 16px;
  background: rgba(247, 250, 255, 0.82);
}

.forum-form textarea {
  min-height: 150px;
  padding: 12px;
  resize: vertical;
}

.forum-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.forum-form-error {
  margin: 0;
  color: #d84c72;
  font-weight: 900;
}

.forum-form footer button {
  min-height: 42px;
  padding: 0 18px;
  border-radius: 999px;
  font-weight: 900;
}

@media (max-width: 1180px) {
  .forum-workspace,
  .forum-layout,
  .forum-topbar {
    grid-template-columns: 1fr;
  }

  .forum-rail {
    border-right: 0;
    border-bottom: 1px solid rgba(110, 144, 218, 0.2);
  }

  .forum-boards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .forum-detail {
    max-height: none;
  }
}

@media (max-width: 760px) {
  .forum-main {
    padding: 16px;
  }

  .forum-thread-head {
    display: none;
  }

  .forum-thread-row {
    grid-template-columns: 1fr;
  }

  .forum-boards,
  .forum-form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
