<template>
  <div class="app-page">
    <aside class="left-sidebar ornamental-card">
      <div class="logo-block">
        <div class="hex-frame">
          <div class="hex-core"></div>
        </div>
        <h1>Learner</h1>
        <p>✦ 学习中 ✦</p>
      </div>

      <nav class="menu-list">
        <button
          v-for="item in navItems"
          :key="item.id"
          type="button"
          class="menu-item"
          :class="{ active: activeNav === item.id }"
          @click="activeNav = item.id"
        >
          <span class="menu-icon">{{ item.icon }}</span>
          <span class="menu-label">{{ item.label }}</span>
          <span class="menu-index">{{ item.index }}</span>
        </button>
      </nav>
    </aside>

    <main class="content-shell">
      <section class="top-illustration">
        <div class="illustration-copy"></div>
      </section>

      <section class="resource-panel ornamental-card">
        <header class="resource-header">
          <h2>资源列表</h2>

          <div class="resource-toolbar">
            <div class="category-tabs">
              <button
                v-for="tab in resourceTabs"
                :key="tab"
                type="button"
                class="category-tab"
                :class="{ active: activeTab === tab }"
                @click="activeTab = tab"
              >
                {{ tab }}
              </button>
            </div>

            <div class="toolbar-actions">
              <label class="search-input">
                <input v-model.trim="keyword" type="text" placeholder="搜索资源..." />
                <span>⌕</span>
              </label>

              <button type="button" class="square-toggle" :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'">
                ☰
              </button>
              <button type="button" class="square-toggle" :class="{ active: viewMode === 'grid' }" @click="viewMode = 'grid'">
                ⊞
              </button>
              <button type="button" class="upload-action">上传</button>
            </div>
          </div>
        </header>

        <section v-if="viewMode === 'list'" class="table-wrap">
          <div class="table-head">
            <span>名称</span>
            <span>类型</span>
            <span>来源</span>
            <span>修改时间 ↓</span>
            <span>大小</span>
            <span>操作</span>
          </div>

          <button
            v-for="item in filteredResources"
            :key="item.id"
            type="button"
            class="table-row"
            :class="{ active: selectedResource.id === item.id }"
            @click="selectedResource = item"
          >
            <div class="name-cell">
              <span class="asset-icon" :class="item.kind">{{ item.badge }}</span>
              <span class="asset-name">{{ item.name }}</span>
            </div>
            <span>{{ item.type }}</span>
            <span>{{ item.source }}</span>
            <span>{{ item.updatedAt }}</span>
            <span>{{ item.size }}</span>
            <span class="more-action">···</span>
          </button>
        </section>

        <section v-else class="grid-wrap">
          <button
            v-for="item in filteredResources"
            :key="item.id"
            type="button"
            class="grid-item"
            :class="{ active: selectedResource.id === item.id }"
            @click="selectedResource = item"
          >
            <span class="asset-icon large" :class="item.kind">{{ item.badge }}</span>
            <strong>{{ item.name }}</strong>
            <p>{{ item.type }} · {{ item.size }}</p>
            <span>{{ item.updatedAt }}</span>
          </button>
        </section>

        <footer class="table-footer">共 {{ filteredResources.length }} 项</footer>
      </section>

      <aside class="detail-panel ornamental-card">
        <header class="detail-tabs">
          <button
            v-for="tab in detailTabs"
            :key="tab"
            type="button"
            class="detail-tab"
            :class="{ active: activeDetailTab === tab }"
            @click="activeDetailTab = tab"
          >
            {{ tab }}
          </button>
        </header>

        <section class="detail-section">
          <div class="detail-top">
            <span class="asset-icon large folder">📁</span>
            <div>
              <h3>{{ selectedResource.name }}</h3>
              <p>{{ selectedResource.group }}</p>
            </div>
            <span class="star-mark">☆</span>
          </div>

          <dl class="detail-meta">
            <div>
              <dt>位置</dt>
              <dd>{{ selectedResource.path }}</dd>
            </div>
            <div>
              <dt>创建时间</dt>
              <dd>{{ selectedResource.createdAt }}</dd>
            </div>
            <div>
              <dt>修改时间</dt>
              <dd>{{ selectedResource.updatedAt }}</dd>
            </div>
            <div>
              <dt>描述</dt>
              <dd>{{ selectedResource.description }}</dd>
            </div>
          </dl>
        </section>

        <section class="info-section">
          <div class="section-head">
            <h4>统计</h4>
            <button type="button">更多 ›</button>
          </div>

          <div class="stats-grid">
            <article v-for="stat in stats" :key="stat.label" class="stat-card">
              <span>{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
            </article>
          </div>
        </section>

        <section class="info-section">
          <div class="section-head">
            <h4>最近学习</h4>
            <button type="button">更多 ›</button>
          </div>

          <div class="recent-list">
            <div v-for="item in recentItems" :key="item.name" class="recent-row">
              <span class="asset-icon small" :class="item.kind">{{ item.badge }}</span>
              <div class="recent-copy">
                <strong>{{ item.name }}</strong>
                <span>{{ item.time }}</span>
              </div>
            </div>
          </div>
        </section>
      </aside>
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const navItems = [
  { id: 'resource', label: '学习资料', index: '01', icon: '⌂' },
  { id: 'card', label: '知识卡片', index: '02', icon: '▣' },
  { id: 'exercise', label: '题前练习', index: '03', icon: '✎' },
  { id: 'wrong', label: '错题本', index: '04', icon: '◈' },
  { id: 'plan', label: '学习计划', index: '05', icon: '⌲' },
  { id: 'stats', label: '统计', index: '06', icon: '◍' },
  { id: 'setting', label: '设置', index: '07', icon: '⚙' },
]

const resourceTabs = ['全部', '文档', '图片', '视频', '音频', '其他']
const detailTabs = ['详情', '标签']
const activeNav = ref('resource')
const activeTab = ref('全部')
const activeDetailTab = ref('详情')
const keyword = ref('')
const viewMode = ref('list')

const resources = [
  {
    id: 1,
    name: '高等数学（上）笔记',
    type: '文件夹',
    kind: 'folder',
    badge: '📁',
    source: '本地',
    updatedAt: '2024-05-20 21:30',
    createdAt: '2024-04-28 09:20',
    size: '—',
    group: '文件夹 · 12 项',
    path: '/我的资源/高等数学（上）笔记',
    description: '课程上课学习资料整理',
  },
  {
    id: 2,
    name: '第三章 极限与连续.docx',
    type: '文档',
    kind: 'doc',
    badge: 'W',
    source: '本地',
    updatedAt: '2024-05-20 20:15',
    createdAt: '2024-05-11 13:20',
    size: '2.4 MB',
    group: '文档 · 重点',
    path: '/我的资源/高数/第三章 极限与连续.docx',
    description: '章节整理与例题归纳',
  },
  {
    id: 3,
    name: '典型公式汇总.pdf',
    type: '文档',
    kind: 'pdf',
    badge: 'PDF',
    source: '本地',
    updatedAt: '2024-05-19 18:40',
    createdAt: '2024-05-09 11:10',
    size: '1.8 MB',
    group: '文档 · 总结',
    path: '/我的资源/高数/典型公式汇总.pdf',
    description: '考试前常用公式速查',
  },
  {
    id: 4,
    name: '极限的定义与性质.mp4',
    type: '视频',
    kind: 'video',
    badge: '▶',
    source: '网盘',
    updatedAt: '2024-05-18 16:20',
    createdAt: '2024-05-06 17:30',
    size: '128 MB',
    group: '视频 · 回看',
    path: '/我的资源/高数/视频/极限的定义与性质.mp4',
    description: '课堂录屏与重点讲解',
  },
  {
    id: 5,
    name: '重要公式思维导图.png',
    type: '图片',
    kind: 'image',
    badge: '▣',
    source: '本地',
    updatedAt: '2024-05-17 14:10',
    createdAt: '2024-05-08 09:42',
    size: '2.1 MB',
    group: '图片 · 导图',
    path: '/我的资源/高数/图像/重要公式思维导图.png',
    description: '思维导图版知识结构',
  },
  {
    id: 6,
    name: '知识点速记.mp3',
    type: '音频',
    kind: 'audio',
    badge: '♫',
    source: '本地',
    updatedAt: '2024-05-16 11:05',
    createdAt: '2024-05-07 22:00',
    size: '5.7 MB',
    group: '音频 · 速记',
    path: '/我的资源/高数/音频/知识点速记.mp3',
    description: '用于碎片化复习的朗读音频',
  },
  {
    id: 7,
    name: '微积分前置笔记.docx',
    type: '文档',
    kind: 'doc',
    badge: 'W',
    source: '本地',
    updatedAt: '2024-05-15 10:30',
    createdAt: '2024-05-04 14:50',
    size: '3.2 MB',
    group: '文档 · 预习',
    path: '/我的资源/高数/微积分前置笔记.docx',
    description: '课程预习与基础整理',
  },
]

const stats = [
  { label: '文件数', value: '12' },
  { label: '总大小', value: '256MB' },
  { label: '学习时长', value: '18.6h' },
  { label: '完成进度', value: '68%' },
]

const recentItems = [
  { name: '第三章 极限与连续.docx', time: '刚刚', kind: 'doc', badge: 'W' },
  { name: '极限的定义与性质.mp4', time: '20 分钟前', kind: 'video', badge: '▶' },
  { name: '典型公式汇总.pdf', time: '1 小时前', kind: 'pdf', badge: 'PDF' },
]

const selectedResource = ref(resources[0])

const filteredResources = computed(() =>
  resources.filter((item) => {
    const matchesTab = activeTab.value === '全部' || item.type === activeTab.value
    const matchesKeyword =
      !keyword.value ||
      item.name.includes(keyword.value) ||
      item.description.includes(keyword.value)
    return matchesTab && matchesKeyword
  }),
)
</script>
