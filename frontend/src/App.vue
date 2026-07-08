<template>
  <main class="app-shell">
    <section v-if="!isLoggedIn" class="login-scene">
      <div class="login-book">
        <section class="login-art" aria-label="ToveloKno">
          <div class="brand-mark">
            <span class="brand-line-mark"></span>
            <strong>ToveloKno</strong>
            <small>STUDY COMMAND UI</small>
          </div>

          <div class="art-hud art-hud-primary" aria-hidden="true">
            <span>TOVELOKNO CORE</span>
            <strong>LEARNING DESK</strong>
            <em>SYNC-07</em>
          </div>

          <div class="art-hud art-hud-secondary" aria-hidden="true">
            <span>STUDY STATUS</span>
            <strong>ACTIVE</strong>
            <em>0.42ms</em>
          </div>

          <div class="signal-bars" aria-hidden="true">
            <i></i>
            <i></i>
            <i></i>
          </div>
        </section>

        <section class="login-panel">
          <div class="panel-meta">
            <span>TOVELOKNO / STUDY OPS</span>
            <b>{{ authMode === 'register' ? 'NEW USER' : 'ONLINE' }}</b>
          </div>
          <h1>{{ authMode === 'register' ? 'Register' : 'Login' }}</h1>
          <p class="login-subtitle">
            {{ authMode === 'register' ? '创建一个新的学习终端账号' : '接入你的个人学习控制台' }}
          </p>

          <div class="auth-switch" role="tablist" aria-label="认证模式">
            <button type="button" :class="{ active: authMode === 'login' }" @click="setAuthMode('login')">LOGIN</button>
            <button type="button" :class="{ active: authMode === 'register' }" @click="setAuthMode('register')">REGISTER</button>
          </div>

          <form class="auth-form" @submit.prevent="submitAuth">
            <label>
              <span>用户名</span>
              <div class="soft-input">
                <span>ID</span>
                <input v-model.trim="authForm.username" type="text" autocomplete="username" placeholder="请输入用户名" />
              </div>
            </label>
            <label>
              <span>密码</span>
              <div class="soft-input">
                <span>PW</span>
                <input
                  v-model="authForm.password"
                  type="password"
                  :autocomplete="authMode === 'register' ? 'new-password' : 'current-password'"
                  placeholder="请输入密码"
                />
              </div>
            </label>
            <label>
              <span>邮箱</span>
              <div class="soft-input">
                <span>ML</span>
                <input
                  v-model.trim="authForm.email"
                  type="email"
                  autocomplete="email"
                  :placeholder="authMode === 'register' ? '可选：用于资料展示' : '登录可不填邮箱'"
                />
              </div>
            </label>
            <p v-if="message.text" class="message-line" :class="message.type">{{ message.text }}</p>
            <button class="login-submit" type="submit" :disabled="loading">
              <span>{{ loading ? '处理中...' : authMode === 'register' ? '创建账号' : '登录系统' }}</span>
              <b>-></b>
            </button>
          </form>
        </section>
      </div>
    </section>

    <section v-else-if="activePanel === 'profile'" class="profile-screen">
      <header class="profile-topbar">
        <button type="button" @click="switchPanel('home')">返回首页</button>
        <strong>ToveloKno</strong>
        <button type="button" @click="handleLogout">退出登录</button>
      </header>

      <main class="profile-canvas">
        <section
          class="profile-cover"
          :style="profileCoverStyle"
        >
          <div v-if="profileDiy.backgroundUrl" class="profile-cover-image" aria-hidden="true"></div>
          <div class="profile-avatar-wrap">
            <button class="profile-hero-avatar" type="button" title="悬停片刻放大头像">
              <img v-if="profileAvatarSrc" :src="profileAvatarSrc" alt="头像" />
              <span v-else>{{ profileName.slice(0, 1).toUpperCase() }}</span>
            </button>
          </div>

          <div class="profile-main-copy">
            <span class="ark-kicker">PERSONAL PAGE</span>
            <h2>{{ profileName }}</h2>
            <p>{{ profileForm.bio || profile.bio }}</p>
            <em>{{ profileDiy.signature || '把知识整理成自己的节奏。' }}</em>
          </div>

          <div class="profile-cover-note">
            <span>LV.{{ level }}</span>
            <strong>{{ profileDiy.coverText || '今日状态：轻装上阵' }}</strong>
          </div>
        </section>

        <section class="profile-personal-grid">
          <article class="profile-story">
            <span>ABOUT</span>
            <h3>{{ profileDiy.coverText || '我的学习档案' }}</h3>
            <p>{{ profileForm.bio || profile.bio }}</p>
            <div class="profile-mini-stats">
              <span><b>{{ resources.length }}</b>资料</span>
              <span><b>{{ cards.length }}</b>卡片</span>
              <span><b>{{ planProgress }}%</b>进度</span>
            </div>
            <button class="profile-edit-banner-button" type="button" @click="openBannerEditor">
              修改推荐画幅
            </button>
          </article>

          <form class="profile-editor" @submit.prevent="saveProfile">
            <header>
              <span>DIY</span>
              <strong>个人主页编辑</strong>
            </header>
            <label>
              <span>名字</span>
              <input v-model.trim="profileForm.nickname" placeholder="你的名字" />
            </label>
            <label>
              <span>头像图片</span>
              <input type="file" accept="image/*" @change="uploadProfileImage($event, 'avatar')" />
            </label>
            <label>
              <span>头图背景</span>
              <input type="file" accept="image/*" @change="uploadProfileImage($event, 'background')" />
            </label>
            <label>
              <span>个人简介</span>
              <textarea v-model.trim="profileForm.bio" rows="4" placeholder="写一点你想展示的简介"></textarea>
            </label>
            <label>
              <span>签名</span>
              <input v-model.trim="profileDiy.signature" placeholder="一句短签名" />
            </label>
            <label>
              <span>头图文案</span>
              <input v-model.trim="profileDiy.coverText" placeholder="例如：今日状态：轻装上阵" />
            </label>
            <button class="primary-button soft" type="submit">保存主页资料</button>
          </form>

          <section v-if="showBannerEditor && bannerDrafts[editingBannerIndex]" class="banner-editor-panel">
            <header>
              <div>
                <span>HOME BANNER</span>
                <strong>推荐画幅编辑</strong>
              </div>
              <div class="banner-editor-header-actions">
                <button class="banner-save-button" type="button" @click="saveHomeBanners()">保存到首页</button>
                <button type="button" @click="showBannerEditor = false">收起</button>
              </div>
            </header>

            <div class="banner-editor-tabs">
              <button
                v-for="(slide, index) in bannerDrafts"
                :key="slide.id"
                type="button"
                :class="{ active: editingBannerIndex === index }"
                @click="selectBannerDraft(index)"
              >
                {{ index + 1 }}
              </button>
            </div>

            <form class="banner-editor-form" @submit.prevent="saveHomeBanners()">
              <label>
                <span>图片介绍文字</span>
                <input v-model.trim="bannerDrafts[editingBannerIndex].text" placeholder="默认留空，只在画幅底部显示一行" />
              </label>
              <label class="banner-editor-wide">
                <span>背景图片</span>
                <div class="banner-upload-row">
                  <label class="banner-upload-button">
                    <input type="file" accept="image/*" @change="uploadHomeBannerImage" />
                    <span>本地上传</span>
                  </label>
                  <button type="button" @click="clearHomeBannerImage">清除</button>
                </div>
              </label>
              <label v-if="bannerDrafts[editingBannerIndex].imageUrl" class="banner-editor-wide">
                <span>背景预览</span>
                <div
                  class="banner-image-preview"
                  :style="bannerPreviewStyle(bannerDrafts[editingBannerIndex])"
                  aria-hidden="true"
                ></div>
              </label>
              <section v-if="bannerCrop.active" class="banner-crop-panel">
                <div class="banner-crop-preview">
                  <img :src="bannerCrop.source" alt="裁剪预览" :style="bannerCropImageStyle" />
                </div>
                <div class="banner-crop-controls">
                  <label>
                    <span>缩放</span>
                    <input v-model.number="bannerCrop.zoom" type="range" min="1" max="2.4" step="0.01" />
                  </label>
                  <label>
                    <span>横向位置</span>
                    <input v-model.number="bannerCrop.x" type="range" min="0" max="100" step="1" />
                  </label>
                  <label>
                    <span>纵向位置</span>
                    <input v-model.number="bannerCrop.y" type="range" min="0" max="100" step="1" />
                  </label>
                  <div class="banner-crop-actions">
                    <button class="primary-button soft" type="button" @click="applyBannerCrop()">应用并保存</button>
                    <button class="outline-button" type="button" @click="cancelBannerCrop">取消</button>
                  </div>
                </div>
              </section>
              <div class="banner-editor-actions">
                <button class="banner-save-button wide" type="submit">保存到首页</button>
                <button class="outline-button" type="button" @click="resetHomeBanners">恢复默认</button>
              </div>
            </form>
          </section>
        </section>
      </main>
    </section>

    <section v-else-if="activePanel === 'practice'" class="question-bank-screen">
      <QuestionBankWorkspace @back-home="switchPanel('home')" />
    </section>

    <section v-else-if="activePanel === 'resources'" class="full-module-screen">
      <LearningResourceWorkspace @back-home="switchPanel('home')" />
    </section>

    <section v-else-if="activePanel === 'wrong'" class="full-module-screen">
      <WrongQuestionWorkspace @back-home="switchPanel('home')" @open-module="switchPanel" />
    </section>

    <section v-else-if="activePanel === 'plan'" class="full-module-screen">
      <StudyPlanWorkspace @back-home="switchPanel('home')" @open-module="switchPanel" />
    </section>

    <section v-else :class="['workspace', { 'home-workspace': activePanel === 'home' }]">
      <aside class="sidebar">
        <div class="side-brand">
          <div class="side-identity">
            <span class="side-avatar">
              <img v-if="profileAvatarSrc" :src="profileAvatarSrc" alt="头像" />
              <b v-else>{{ profileName.slice(0, 1).toUpperCase() }}</b>
            </span>
            <span class="side-user-copy">
              <small>当前用户</small>
              <strong>{{ profileName }}</strong>
              <em>LV.{{ level }} / {{ resources.length + cards.length }} 项资料</em>
            </span>
          </div>
          <div class="side-brand-name">
            <strong>ToveloKno</strong>
            <span>学习终端</span>
          </div>
          <div class="side-progress">
            <span>计划进度 <b>{{ planProgress }}%</b></span>
            <i><b :style="{ width: `${planProgress}%` }"></b></i>
          </div>
        </div>

        <nav class="module-nav">
          <section v-for="group in navGroups" :key="group.title" class="nav-group">
            <p>{{ group.title }}</p>
            <button
              v-for="item in group.items"
              :key="item.id"
              type="button"
              :class="{ active: activePanel === item.id }"
              @click="switchPanel(item.id)"
            >
              <em>{{ item.number }}</em>
              <span>{{ item.label }}</span>
              <i></i>
            </button>
          </section>
        </nav>

        <button class="logout-button" type="button" @click="handleLogout"><span>退出登录</span><b>09</b></button>
      </aside>

      <section class="content-frame" :class="{ 'dashboard-frame': activePanel === 'home' }">
        <Transition name="panel-shift" mode="out-in">
        <section v-if="activePanel === 'home'" :key="'home'" class="recommend-home">
          <section class="recommend-carousel" aria-label="推荐画幅">
            <article
              class="recommend-slide"
            >
              <div
                class="home-banner-art"
                :class="{ 'has-image': activeRecommendationSlide.imageUrl }"
                :style="bannerVisualStyle(activeRecommendationSlide)"
                aria-hidden="true"
              >
              </div>
              <div class="home-banner-caption">
                <p v-if="activeRecommendationSlide.text">{{ activeRecommendationSlide.text }}</p>
                <div class="home-banner-footer">
                  <div class="banner-dots" aria-label="画幅切换">
                    <button
                      v-for="(slide, index) in recommendationSlides"
                      :key="slide.id"
                      type="button"
                      :class="{ active: homeBannerIndex === index }"
                      @click.stop="setBannerIndex(index)"
                    ></button>
                  </div>
                  <div class="banner-controls">
                    <button type="button" aria-label="上一张" @click.stop="moveBanner(-1)">‹</button>
                    <button type="button" aria-label="下一张" @click.stop="moveBanner(1)">›</button>
                  </div>
                </div>
              </div>
            </article>
          </section>
          <section class="home-link-hub" aria-label="学习联动中枢">
            <header>
              <div>
                <span class="ark-kicker">CONNECTED STUDY</span>
                <h3>学习中枢</h3>
              </div>
              <button type="button" @click="loadStudyLinkOverview">刷新</button>
            </header>
            <div class="home-link-stats">
              <article v-for="item in studyLinkStats" :key="item.label" @click="switchPanel(item.target)">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
                <p>{{ item.desc }}</p>
              </article>
            </div>
            <div class="home-link-columns">
              <section>
                <header>
                  <strong>资料生成的题</strong>
                  <button type="button" @click="switchPanel('practice')">题库</button>
                </header>
                <p v-if="!studyLinkOverview.recentResourceQuestions?.length" class="home-link-empty">
                  暂无资料摘录题，去资料模块截取内容生成题目。
                </p>
                <article v-for="item in studyLinkOverview.recentResourceQuestions || []" :key="item.id">
                  <b>{{ item.content }}</b>
                  <span>{{ item.sourceResourceName || '学习资料' }}{{ item.sourcePage ? ` / P${item.sourcePage}` : '' }}</span>
                </article>
              </section>
              <section>
                <header>
                  <strong>关联计划</strong>
                  <button type="button" @click="switchPanel('plan')">计划</button>
                </header>
                <p v-if="!studyLinkOverview.recentPlans?.length" class="home-link-empty">
                  暂无计划，把错题或资料加入复盘队列。
                </p>
                <article v-for="item in studyLinkOverview.recentPlans || []" :key="item.id">
                  <b>{{ item.title }}</b>
                  <span>{{ targetTypeLabel(item.targetType) }}{{ item.targetTitle ? ` / ${item.targetTitle}` : '' }} · {{ statusLabel(item.status) }}</span>
                </article>
              </section>
            </div>
          </section>
        </section>

        <section v-else-if="activePanel === 'resources'" :key="'resources'" class="module-board ark-board">
          <div class="board-toolbar">
            <div>
              <span class="ark-kicker">RESOURCE / INDEX</span>
              <h3>学习资料</h3>
              <div class="tabs">
                <button
                  v-for="tab in resourceTabs"
                  :key="tab"
                  type="button"
                  :class="{ active: activeResourceTab === tab }"
                  @click="activeResourceTab = tab"
                >
                  {{ tab }}
                </button>
              </div>
            </div>
            <div class="tool-actions">
              <label class="search-box">
                <input v-model.trim="resourceKeyword" type="search" placeholder="搜索资源" />
                <span>Q</span>
              </label>
              <button type="button" @click="resourceView = resourceView === 'table' ? 'grid' : 'table'">
                {{ resourceView === 'table' ? '网格' : '列表' }}
              </button>
              <button type="button" @click="showResourceForm = !showResourceForm">新增</button>
            </div>
          </div>

          <form v-if="showResourceForm" class="ark-form inline-form" @submit.prevent="addResource">
            <input v-model.trim="resourceDraft.name" placeholder="资料名称" />
            <select v-model="resourceDraft.type">
              <option v-for="type in resourceTabs.slice(1)" :key="type">{{ type }}</option>
            </select>
            <input v-model.trim="resourceDraft.size" placeholder="大小，如 2.4 MB" />
            <button class="primary-button" type="submit">加入资料库</button>
          </form>

          <div v-if="resourceView === 'table'" class="resource-table">
            <div class="table-head">
              <span>名称</span>
              <span>类型</span>
              <span>来源</span>
              <span>修改时间</span>
              <span>大小</span>
              <span>操作</span>
            </div>
            <div
              v-for="resource in filteredResources"
              :key="resource.id"
              class="table-row"
              :class="{ selected: selectedResourceId === resource.id }"
              @click="selectResource(resource)"
            >
              <span><i :class="['file-icon', resource.tone]">{{ resource.icon }}</i>{{ resource.name }}</span>
              <span>{{ resource.type }}</span>
              <span>{{ resource.source }}</span>
              <span>{{ resource.time }}</span>
              <span>{{ resource.size }}</span>
              <span class="row-actions">
                <button type="button" @click.stop="toggleFavorite(resource)">{{ resource.favorite ? '已标' : '星标' }}</button>
                <button type="button" @click.stop="removeResource(resource.id)">删除</button>
              </span>
            </div>
          </div>

          <div v-else class="resource-grid">
            <article
              v-for="resource in filteredResources"
              :key="resource.id"
              class="ops-card"
              :class="{ selected: selectedResourceId === resource.id }"
              @click="selectResource(resource)"
            >
              <span>{{ resource.type }}</span>
              <h4>{{ resource.name }}</h4>
              <p>{{ resource.description }}</p>
              <div class="card-actions">
                <button type="button" @click.stop="toggleFavorite(resource)">{{ resource.favorite ? '取消星标' : '星标' }}</button>
                <button type="button" @click.stop="removeResource(resource.id)">删除</button>
              </div>
            </article>
          </div>
          <p class="table-count">共 {{ filteredResources.length }} 项</p>
        </section>

        <section v-else-if="activePanel === 'cards'" :key="'cards'" class="module-board ark-board">
          <div class="simple-head">
            <div>
              <span class="ark-kicker">MEMORY / CARD</span>
              <h3>知识卡片</h3>
            </div>
            <button type="button" class="outline-button" @click="showCardForm = !showCardForm">新建卡片</button>
          </div>

          <form v-if="showCardForm" class="ark-form card-editor" @submit.prevent="addCard">
            <input v-model.trim="cardDraft.title" placeholder="卡片标题" />
            <input v-model.trim="cardDraft.tag" placeholder="标签" />
            <textarea v-model.trim="cardDraft.content" placeholder="写下定义、公式或易错点"></textarea>
            <button class="primary-button" type="submit">保存卡片</button>
          </form>

          <div class="card-grid">
            <article v-for="card in cards" :key="card.id" class="study-card">
              <span>{{ card.tag }}</span>
              <h4>{{ card.title }}</h4>
              <p>{{ card.content }}</p>
              <div class="card-actions">
                <button type="button" @click="reviewCard(card)">复习 +1</button>
                <button type="button" @click="removeCard(card.id)">删除</button>
              </div>
              <small>已复习 {{ card.reviewCount }} 次</small>
            </article>
          </div>
        </section>

        <section v-else-if="activePanel === 'stats'" :key="'stats'" class="module-board ark-board">
          <div class="simple-head">
            <div>
              <span class="ark-kicker">DATA / OVERVIEW</span>
              <h3>学习统计</h3>
            </div>
            <p class="score-chip">本地实时统计</p>
          </div>

          <div class="stats-grid">
            <article v-for="stat in statsCards" :key="stat.label">
              <span>{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
              <p>{{ stat.desc }}</p>
            </article>
          </div>
          <div class="chart-panel">
            <article v-for="bar in chartBars" :key="bar.label">
              <span>{{ bar.label }}</span>
              <div><b :style="{ width: bar.value + '%' }"></b></div>
              <em>{{ bar.value }}%</em>
            </article>
          </div>
        </section>

        <section v-else-if="activePanel === 'settings'" :key="'settings'" class="module-board ark-board">
          <div class="simple-head">
            <div>
              <span class="ark-kicker">SYSTEM / SETTINGS</span>
              <h3>设置</h3>
            </div>
            <p v-if="profileMessage" class="message-line success">{{ profileMessage }}</p>
          </div>

          <div class="settings-grid">
            <form class="ark-form settings-form" @submit.prevent="saveProfile">
              <h4>个人资料</h4>
              <input v-model.trim="profileForm.nickname" placeholder="昵称" />
              <input v-model.trim="profileForm.email" type="email" placeholder="邮箱" />
              <textarea v-model.trim="profileForm.bio" placeholder="个人简介"></textarea>
              <button class="primary-button" type="submit">保存资料</button>
            </form>

            <form class="ark-form settings-form" @submit.prevent="savePassword">
              <h4>修改密码</h4>
              <input v-model="passwordForm.oldPassword" type="password" placeholder="旧密码" />
              <input v-model="passwordForm.newPassword" type="password" placeholder="新密码" />
              <input v-model="passwordForm.confirmPassword" type="password" placeholder="确认新密码" />
              <button class="primary-button" type="submit">更新密码</button>
            </form>
          </div>
        </section>
        </Transition>
      </section>

      <aside v-if="activePanel !== 'home'" class="detail-panel">
        <div class="detail-tabs">
          <button type="button" class="active">详情</button>
          <button type="button">标签</button>
        </div>

        <Transition name="side-shift" mode="out-in">
          <div :key="activePanel" class="detail-panel-body">
            <section class="selected-file">
              <i :class="['file-icon', sidePanel.tone]">{{ sidePanel.icon }}</i>
              <div>
                <h4>{{ sidePanel.title }}</h4>
                <p>{{ sidePanel.subtitle }}</p>
              </div>
              <button v-if="activePanel === 'resources'" type="button" @click="toggleFavorite(selectedResource)">
                {{ selectedResource.favorite ? '★' : '☆' }}
              </button>
            </section>

            <dl class="file-meta">
              <template v-for="item in sidePanel.meta" :key="item.label">
                <dt>{{ item.label }}</dt>
                <dd>{{ item.value }}</dd>
              </template>
            </dl>

            <section class="side-tags">
              <span v-for="tag in sidePanel.tags" :key="tag">{{ tag }}</span>
            </section>

            <section class="mini-stat">
              <header>
                <strong>{{ sidePanel.statTitle }}</strong>
                <button type="button" @click="switchPanel(sidePanel.statTarget)">更多</button>
              </header>
              <div>
                <span v-for="stat in sidePanel.stats" :key="stat.label"><b>{{ stat.value }}</b>{{ stat.label }}</span>
              </div>
            </section>

            <section class="recent-list">
              <header>
                <strong>{{ sidePanel.listTitle }}</strong>
                <button type="button" @click="switchPanel(sidePanel.listTarget)">更多</button>
              </header>
              <p v-for="item in sidePanel.items" :key="item.name">
                <span>{{ item.name }}</span>
                <em>{{ item.time }}</em>
              </p>
            </section>
          </div>
        </Transition>
      </aside>

      <nav v-if="activePanel !== 'home'" class="module-dock" aria-label="模块快捷连接">
        <button
          v-for="item in moduleDock"
          :key="item.id"
          type="button"
          :class="{ active: activePanel === item.id }"
          @click="switchPanel(item.id)"
        >
          <span>{{ item.code }}</span>
          <strong>{{ item.label }}</strong>
        </button>
      </nav>
    </section>
  </main>
</template>

<script>
import {
  changeCurrentUserPassword,
  clearAuthToken,
  createStudyPlan,
  deleteStudyPlan,
  fetchStudyLinkOverview,
  getAuthToken,
  getCurrentUserProfile,
  listStudyPlans,
  loginUser,
  listHomeBanners,
  registerUser,
  resetHomeBanners as resetHomeBannersApi,
  resolveAssetUrl,
  saveHomeBanners as saveHomeBannersApi,
  updateCurrentUserProfile,
  updateStudyPlan,
  updateStudyPlanStatus,
  uploadCurrentUserProfileImage,
  uploadHomeBannerImage as uploadHomeBannerImageFile,
} from './api'
import QuestionBankWorkspace from './modules/question-bank/views/QuestionBankWorkspace.vue'
import './modules/question-bank/styles/question-bank-workspace.css'
import LearningResourceWorkspace from './modules/learning-resource/LearningResourceWorkspace.vue'
import StudyPlanWorkspace from './modules/study-plan/StudyPlanWorkspace.vue'
import WrongQuestionWorkspace from './modules/wrong-question/WrongQuestionWorkspace.vue'

const nowTime = () =>
  new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date())

const createId = (prefix) => `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`

const HOME_BANNER_STORAGE_KEY = 'tovelokno-home-banners'
const BANNER_IMAGE_MAX_WIDTH = 1800
const BANNER_IMAGE_QUALITY = 0.86
const BANNER_CROP_WIDTH = 1600
const BANNER_CROP_HEIGHT = 680

const DEFAULT_HOME_BANNERS = [
  {
    id: 'continue-resource',
    title: '从星标资料继续',
    text: '',
    imageUrl: '',
  },
  {
    id: 'today-plan',
    title: '把任务推进到下一格',
    text: '',
    imageUrl: '',
  },
  {
    id: 'wrong-review',
    title: '错题优先处理',
    text: '',
    imageUrl: '',
  },
  {
    id: 'question-bank',
    title: '进入练习模块',
    text: '',
    imageUrl: '',
  },
]

const cloneHomeBanners = (banners = DEFAULT_HOME_BANNERS) =>
  banners.map((item, index) => ({
    id: item.id || `banner-${index}`,
    title: item.title || DEFAULT_HOME_BANNERS[index]?.title || '推荐画幅',
    text: item.text || DEFAULT_HOME_BANNERS[index]?.text || '',
    imageUrl: item.imageUrl || '',
  }))

const readHomeBanners = () => {
  if (typeof window === 'undefined') return cloneHomeBanners()
  try {
    const saved = JSON.parse(window.localStorage.getItem(HOME_BANNER_STORAGE_KEY) || 'null')
    return Array.isArray(saved) && saved.length ? cloneHomeBanners(saved) : cloneHomeBanners()
  } catch {
    return cloneHomeBanners()
  }
}

const readImageAsDataUrl = (file, maxWidth = BANNER_IMAGE_MAX_WIDTH, quality = BANNER_IMAGE_QUALITY) =>
  new Promise((resolve, reject) => {
    if (!file?.type?.startsWith('image/')) {
      reject(new Error('请选择图片文件'))
      return
    }

    const reader = new FileReader()
    reader.onerror = () => reject(new Error('图片读取失败'))
    reader.onload = () => {
      const rawUrl = reader.result
      const image = new Image()
      image.onerror = () => resolve(rawUrl)
      image.onload = () => {
        const ratio = Math.min(1, maxWidth / image.width)
        const width = Math.round(image.width * ratio)
        const height = Math.round(image.height * ratio)
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const context = canvas.getContext('2d')
        if (!context) {
          resolve(rawUrl)
          return
        }
        context.drawImage(image, 0, 0, width, height)
        resolve(canvas.toDataURL('image/jpeg', quality))
      }
      image.src = rawUrl
    }
    reader.readAsDataURL(file)
  })

const dataUrlToFile = (dataUrl, filename = 'home-banner.jpg') => {
  const [meta, content] = dataUrl.split(',')
  const mimeMatch = meta.match(/data:(.*?);base64/)
  const mime = mimeMatch?.[1] || 'image/jpeg'
  const binary = atob(content)
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }
  return new File([bytes], filename, { type: mime })
}

export default {
  name: 'App',
  components: {
    LearningResourceWorkspace,
    QuestionBankWorkspace,
    StudyPlanWorkspace,
    WrongQuestionWorkspace,
  },
  data() {
    return {
      loading: false,
      isLoggedIn: Boolean(getAuthToken()),
      message: { type: '', text: '' },
      authMode: 'login',
      authForm: {
        username: 'ADMIN',
        password: '',
        email: '',
      },
      activePanel: 'home',
      navItems: [
        { id: 'home', label: '推荐首页', icon: 'HM' },
        { id: 'profile', label: '个人主页', icon: 'PR' },
        { id: 'resources', label: '学习资料', icon: 'RS' },
        { id: 'cards', label: '知识卡片', icon: 'CD' },
        { id: 'practice', label: '题目练习', icon: 'TR' },
        { id: 'wrong', label: '错题本', icon: 'ER' },
        { id: 'plan', label: '学习计划', icon: 'PL' },
        { id: 'stats', label: '统计', icon: 'DT' },
      ],
      homeBannerIndex: 0,
      showBannerEditor: false,
      editingBannerIndex: 0,
      homeBanners: readHomeBanners(),
      bannerDrafts: cloneHomeBanners(),
      bannerTimer: null,
      studyLinkOverview: {},
      bannerCrop: {
        active: false,
        source: '',
        zoom: 1,
        x: 50,
        y: 50,
      },
      resourceTabs: ['全部', '文档', '图片', '视频', '音频', '其他'],
      activeResourceTab: '全部',
      resourceView: 'table',
      resourceKeyword: '',
      showResourceForm: false,
      selectedResourceId: 1,
      resourceDraft: { name: '', type: '文档', size: '' },
      resources: [
        {
          id: 1,
          name: '高等数学（上）笔记',
          type: '文档',
          source: '本地',
          time: '2024-05-20 21:30',
          size: '12 项',
          path: '/我的资源/高等数学（上）笔记',
          description: '课程上课学习资料整理，包含极限、连续与导数。',
          icon: 'W',
          tone: 'blue',
          favorite: true,
        },
        {
          id: 2,
          name: '第三章 极限与连续.docx',
          type: '文档',
          source: '本地',
          time: '2024-05-20 20:15',
          size: '2.4 MB',
          path: '/我的资源/第三章 极限与连续.docx',
          description: '极限计算、夹逼准则与连续性判断。',
          icon: 'W',
          tone: 'blue',
          favorite: false,
        },
        {
          id: 3,
          name: '典型公式汇总.pdf',
          type: '文档',
          source: '本地',
          time: '2024-05-19 18:40',
          size: '1.8 MB',
          path: '/我的资源/典型公式汇总.pdf',
          description: '高频公式与错题复盘入口。',
          icon: 'P',
          tone: 'red',
          favorite: false,
        },
        {
          id: 4,
          name: '极限的定义与性质.mp4',
          type: '视频',
          source: '网盘',
          time: '2024-05-18 16:20',
          size: '128 MB',
          path: '/我的资源/极限的定义与性质.mp4',
          description: '课堂录屏，适合二轮复习。',
          icon: 'V',
          tone: 'violet',
          favorite: true,
        },
        {
          id: 5,
          name: '重要公式思维导图.png',
          type: '图片',
          source: '本地',
          time: '2024-05-17 14:10',
          size: '2.1 MB',
          path: '/我的资源/重要公式思维导图.png',
          description: '把分散知识点按章节聚合。',
          icon: 'I',
          tone: 'blue',
          favorite: false,
        },
      ],
      showCardForm: false,
      cardDraft: { title: '', tag: '', content: '' },
      cards: [
        { id: 1, title: '夹逼准则', tag: '极限', content: '当两侧函数趋向同一极限时，中间函数也趋向该极限。', reviewCount: 2 },
        { id: 2, title: '连续判定', tag: '函数', content: '函数值存在、极限存在且二者相等。', reviewCount: 1 },
        { id: 3, title: '导数定义', tag: '微分', content: '本质是函数在一点附近的平均变化率极限。', reviewCount: 0 },
      ],
      currentQuestionIndex: 0,
      selectedAnswer: '',
      answers: {},
      questions: [
        {
          id: 1,
          title: '若 lim f(x)=A 且 lim g(x)=A，h(x) 被二者夹住，则 h(x) 的极限是？',
          options: ['A', '0', '不存在', '无法判断'],
          answer: 'A',
          analysis: '夹逼准则要求上下界趋于同一值，中间函数也趋于同一值。',
        },
        {
          id: 2,
          title: '函数在 x0 连续，以下哪项必须成立？',
          options: ['函数值等于极限', '导数存在', '单调递增', '二阶导存在'],
          answer: '函数值等于极限',
          analysis: '连续的核心条件是函数值存在、极限存在且相等。',
        },
      ],
      wrongFilter: '全部',
      wrongQuestions: [
        { id: 1, subject: '高数', title: '无穷小比较误判', note: '需要先同阶替换，再判断主项。', mastered: false },
        { id: 2, subject: '英语', title: '定语从句关系词', note: '先判断先行词，再看从句缺少成分。', mastered: true },
      ],
      planDraft: { title: '', time: '' },
      planFilter: { status: '', planDate: '' },
      planLoading: false,
      planPage: 1,
      planTotal: 0,
      planTotalPages: 1,
      planEditorOpen: false,
      planSubmitting: false,
      planFormError: '',
      editingPlan: null,
      planForm: {
        title: '',
        content: '',
        planDate: '',
      },
      plans: [
        { id: 1, title: '整理第三章公式', time: '09:30', done: true },
        { id: 2, title: '完成 20 道极限题', time: '15:00', done: false },
        { id: 3, title: '复盘错题本', time: '21:00', done: false },
      ],
      profile: {
        nickname: 'ToveloKno',
        email: '',
        bio: '把资料、卡片、练习与复盘串成自己的学习控制台。',
      },
      profileForm: {
        nickname: 'ToveloKno',
        email: '',
        bio: '把资料、卡片、练习与复盘串成自己的学习控制台。',
      },
      profileDiy: {
        avatarUrl: '',
        backgroundUrl: '',
        signature: '把知识整理成自己的节奏。',
        coverText: '今日状态：轻装上阵',
      },
      passwordForm: { oldPassword: '', newPassword: '', confirmPassword: '' },
      profileMessage: '',
    }
  },
  computed: {
    filteredResources() {
      const keyword = this.resourceKeyword.toLowerCase()
      return this.resources.filter((item) => {
        const matchTab = this.activeResourceTab === '全部' || item.type === this.activeResourceTab
        const matchKeyword = !keyword || `${item.name}${item.type}${item.description}`.toLowerCase().includes(keyword)
        return matchTab && matchKeyword
      })
    },
    selectedResource() {
      return this.resources.find((item) => item.id === this.selectedResourceId) || this.resources[0]
    },
    sidePanel() {
      const baseStats = [
        { label: '文件数', value: this.resources.length },
        { label: '卡片', value: this.cards.length },
        { label: '错题', value: this.wrongQuestions.length },
        { label: '进度', value: `${this.planProgress}%` },
      ]
      const fallback = {
        icon: 'T',
        tone: 'blue',
        title: '推荐首页',
        subtitle: '推荐画幅、常用入口与今日队列',
        meta: [
          { label: '当前用户', value: this.profileName },
          { label: '今日队列', value: `${this.todayFocus.length} 项` },
          { label: '完成进度', value: `${this.planProgress}%` },
          { label: '说明', value: '作为登录后的默认中转页，各模块返回都会回到这里。' },
        ],
        tags: ['推荐首页', '今日任务', '模块入口'],
        statTitle: '总览统计',
        statTarget: 'stats',
        stats: baseStats,
        listTitle: '今日队列',
        listTarget: 'plan',
        items: this.todayFocus.map((item) => ({ name: item.title, time: item.done ? '已完成' : '待执行' })),
      }

      const panels = {
        home: fallback,
        profile: {
          icon: this.profileName.slice(0, 1).toUpperCase(),
          tone: 'blue',
          title: this.profileName,
          subtitle: this.profile.email || '个人主页与资料',
          meta: [
            { label: '等级', value: `LV.${this.level}` },
            { label: '签名', value: this.profileDiy.signature || '未设置' },
            { label: '简介', value: this.profileForm.bio || this.profile.bio },
          ],
          tags: ['个人主页', '头像', '签名'],
          statTitle: '主页数据',
          statTarget: 'stats',
          stats: baseStats,
          listTitle: '主页入口',
          listTarget: 'profile',
          items: this.profileModules.map((item) => ({ name: item.title, time: item.text })),
        },
        dashboard: fallback,
        resources: {
          icon: this.selectedResource.icon,
          tone: this.selectedResource.tone,
          title: this.selectedResource.name,
          subtitle: `${this.selectedResource.type} - ${this.selectedResource.size}`,
          meta: [
            { label: '位置', value: this.selectedResource.path },
            { label: '来源', value: this.selectedResource.source },
            { label: '修改时间', value: this.selectedResource.time },
            { label: '描述', value: this.selectedResource.description },
          ],
          tags: [this.selectedResource.type, this.selectedResource.favorite ? '星标' : '未星标', this.selectedResource.source],
          statTitle: '资料统计',
          statTarget: 'stats',
          stats: [
            { label: '文件数', value: this.resources.length },
            { label: '文档', value: this.resources.filter((item) => item.type === '文档').length },
            { label: '星标', value: this.resources.filter((item) => item.favorite).length },
            { label: '筛选', value: this.filteredResources.length },
          ],
          listTitle: '最近资料',
          listTarget: 'resources',
          items: this.recentStudy,
        },
        cards: {
          icon: 'C',
          tone: 'blue',
          title: '知识卡片',
          subtitle: `${this.cards.length} 张可复习卡片`,
          meta: [
            { label: '卡片总数', value: `${this.cards.length} 张` },
            { label: '复习次数', value: `${this.cards.reduce((sum, item) => sum + item.reviewCount, 0)} 次` },
            { label: '最近卡片', value: this.cards[0]?.title || '暂无' },
            { label: '说明', value: '把概念、公式和易错点压缩成可复习卡片。' },
          ],
          tags: [...new Set(this.cards.map((item) => item.tag))],
          statTitle: '卡片统计',
          statTarget: 'stats',
          stats: [
            { label: '卡片', value: this.cards.length },
            { label: '标签', value: new Set(this.cards.map((item) => item.tag)).size },
            { label: '复习', value: this.cards.reduce((sum, item) => sum + item.reviewCount, 0) },
            { label: '待复习', value: this.cards.filter((item) => item.reviewCount === 0).length },
          ],
          listTitle: '卡片列表',
          listTarget: 'cards',
          items: this.cards.slice(0, 3).map((item) => ({ name: item.title, time: item.tag })),
        },
        practice: {
          icon: 'Q',
          tone: 'violet',
          title: '题库工作台',
          subtitle: '题目管理、分类、练习与统计',
          meta: [
            { label: '模块来源', value: '组员题库模块' },
            { label: '接入方式', value: '已嵌入主工作台' },
            { label: '后端接口', value: '/api/questions 与 /api/question-bank' },
            { label: '说明', value: '支持题目增删改查、分类关系、练习答题、错题与统计。' },
          ],
          tags: ['题库', '分类', '练习', '错题', '统计'],
          statTitle: '题库能力',
          statTarget: 'stats',
          stats: [
            { label: '题目', value: '✓' },
            { label: '分类', value: '✓' },
            { label: '练习', value: '✓' },
            { label: '统计', value: '✓' },
          ],
          listTitle: '功能入口',
          listTarget: 'practice',
          items: [
            { name: '题目列表', time: '管理' },
            { name: '分类关系', time: '整理' },
            { name: '练习模式', time: '答题' },
            { name: '数据统计', time: '分析' },
          ],
        },
        wrong: {
          icon: 'E',
          tone: 'red',
          title: '错题本',
          subtitle: `${this.filteredWrongQuestions.length} 条当前记录`,
          meta: [
            { label: '当前筛选', value: this.wrongFilter },
            { label: '未掌握', value: `${this.wrongQuestions.filter((item) => !item.mastered).length} 条` },
            { label: '已掌握', value: `${this.wrongQuestions.filter((item) => item.mastered).length} 条` },
            { label: '说明', value: '集中处理练习中收集的错题和复盘笔记。' },
          ],
          tags: [this.wrongFilter, ...new Set(this.wrongQuestions.map((item) => item.subject))],
          statTitle: '错题统计',
          statTarget: 'stats',
          stats: [
            { label: '错题', value: this.wrongQuestions.length },
            { label: '未掌握', value: this.wrongQuestions.filter((item) => !item.mastered).length },
            { label: '已掌握', value: this.wrongQuestions.filter((item) => item.mastered).length },
            { label: '筛选', value: this.filteredWrongQuestions.length },
          ],
          listTitle: '错题列表',
          listTarget: 'wrong',
          items: this.filteredWrongQuestions.map((item) => ({ name: item.title, time: item.mastered ? '已掌握' : '未掌握' })),
        },
        plan: {
          icon: 'P',
          tone: 'blue',
          title: '学习计划',
          subtitle: `${this.planProgress}% 完成`,
          meta: [
            { label: '计划总数', value: `${this.plans.length} 项` },
            { label: '完成数量', value: `${this.plans.filter((item) => item.done).length} 项` },
            { label: '筛选状态', value: this.planFilter.status || '全部' },
            { label: '计划日期', value: this.planFilter.planDate || '未限定' },
          ],
          tags: ['学习计划', '今日路线', `${this.planProgress}%`],
          statTitle: '计划统计',
          statTarget: 'stats',
          stats: [
            { label: '计划', value: this.plans.length },
            { label: '完成', value: this.plans.filter((item) => item.done).length },
            { label: '待办', value: this.plans.filter((item) => !item.done).length },
            { label: '进度', value: `${this.planProgress}%` },
          ],
          listTitle: '计划队列',
          listTarget: 'plan',
          items: this.plans.slice(0, 4).map((item) => ({ name: item.title, time: item.done ? '已完成' : item.time })),
        },
        stats: {
          icon: 'D',
          tone: 'blue',
          title: '学习统计',
          subtitle: '本地实时统计',
          meta: [
            { label: '资料整理', value: `${this.chartBars[0].value}%` },
            { label: '卡片复习', value: `${this.chartBars[1].value}%` },
            { label: '练习正确', value: `${this.chartBars[2].value}%` },
            { label: '计划推进', value: `${this.chartBars[3].value}%` },
          ],
          tags: ['统计', '进度', '总览'],
          statTitle: '核心数据',
          statTarget: 'stats',
          stats: baseStats,
          listTitle: '指标列表',
          listTarget: 'stats',
          items: this.chartBars.map((item) => ({ name: item.label, time: `${item.value}%` })),
        },
        settings: {
          icon: 'S',
          tone: 'blue',
          title: '设置',
          subtitle: '账户资料与密码',
          meta: [
            { label: '昵称', value: this.profileForm.nickname || '未设置' },
            { label: '邮箱', value: this.profileForm.email || '未设置' },
            { label: '头像', value: this.profileDiy.avatarUrl ? '已上传' : '未上传' },
            { label: '简介', value: this.profileForm.bio || '未填写' },
          ],
          tags: ['账户', '资料', '安全'],
          statTitle: '账户状态',
          statTarget: 'settings',
          stats: [
            { label: '头像', value: this.profileDiy.avatarUrl ? '有' : '无' },
            { label: '邮箱', value: this.profileForm.email ? '有' : '无' },
            { label: '等级', value: this.level },
            { label: '进度', value: `${this.planProgress}%` },
          ],
          listTitle: '设置项',
          listTarget: 'settings',
          items: [
            { name: '个人资料', time: '可编辑' },
            { name: '邮箱信息', time: this.profileForm.email ? '已填写' : '未填写' },
            { name: '登录密码', time: '可更新' },
          ],
        },
      }
      return panels[this.activePanel] || fallback
    },
    currentQuestion() {
      return this.questions[this.currentQuestionIndex]
    },
    practiceScore() {
      const checked = Object.entries(this.answers)
      const correct = checked.filter(([id, answer]) => {
        const question = this.questions.find((item) => String(item.id) === id)
        return question?.answer === answer
      }).length
      return { correct, total: checked.length }
    },
    filteredWrongQuestions() {
      if (this.wrongFilter === '已掌握') return this.wrongQuestions.filter((item) => item.mastered)
      if (this.wrongFilter === '未掌握') return this.wrongQuestions.filter((item) => !item.mastered)
      return this.wrongQuestions
    },
    planProgress() {
      if (!this.plans.length) return 0
      return Math.round((this.plans.filter((item) => item.done).length / this.plans.length) * 100)
    },
    statsCards() {
      return [
        { label: '文件数', value: this.resources.length, desc: '资料库当前条目' },
        { label: '卡片', value: this.cards.length, desc: '可复习知识卡' },
        { label: '错题', value: this.wrongQuestions.length, desc: '需要持续压低' },
        { label: '计划完成', value: `${this.planProgress}%`, desc: '今日执行进度' },
      ]
    },
    chartBars() {
      return [
        { label: '资料整理', value: Math.min(100, this.resources.length * 14) },
        { label: '卡片复习', value: Math.min(100, this.cards.reduce((sum, item) => sum + item.reviewCount, 0) * 12) },
        { label: '练习正确', value: this.practiceScore.total ? Math.round((this.practiceScore.correct / this.practiceScore.total) * 100) : 0 },
        { label: '计划推进', value: this.planProgress },
      ]
    },
    studyLinkStats() {
      const overview = this.studyLinkOverview || {}
      return [
        { label: '资料', value: overview.resourceCount ?? this.resources.length, desc: '已归档学习资料', target: 'resources' },
        { label: '题目', value: overview.questionCount ?? '-', desc: '题库总量', target: 'practice' },
        { label: '资料题', value: overview.resourceQuestionCount ?? '-', desc: '从资料摘录生成', target: 'practice' },
        { label: '未掌握', value: overview.activeWrongCount ?? this.wrongQuestions.length, desc: '错题复盘队列', target: 'wrong' },
        { label: '待计划', value: overview.pendingPlanCount ?? '-', desc: '待执行学习任务', target: 'plan' },
      ]
    },
    recommendationSlides() {
      return this.homeBanners.length ? this.homeBanners : cloneHomeBanners()
    },
    activeRecommendationSlide() {
      return this.recommendationSlides[this.homeBannerIndex] || this.recommendationSlides[0] || cloneHomeBanners()[0]
    },
    navGroups() {
      const order = new Map(this.navItems.map((item, index) => [item.id, {
        ...item,
        number: String(index + 1).padStart(2, '0'),
      }]))
      return [
        { title: '入口', items: ['home', 'profile'].map((id) => order.get(id)).filter(Boolean) },
        { title: '学习', items: ['resources', 'cards', 'practice', 'wrong', 'plan'].map((id) => order.get(id)).filter(Boolean) },
        { title: '系统', items: ['stats'].map((id) => order.get(id)).filter(Boolean) },
      ]
    },
    moduleDock() {
      return [
        { id: 'home', code: 'HM', label: '首页' },
        { id: 'resources', code: 'RS', label: '资料' },
        { id: 'cards', code: 'CD', label: '卡片' },
        { id: 'practice', code: 'QB', label: '题库' },
        { id: 'wrong', code: 'ER', label: '错题' },
        { id: 'plan', code: 'PL', label: '计划' },
      ]
    },
    todayFocus() {
      return this.plans.slice(0, 3).map((item) => ({
        time: item.time,
        title: item.title,
        desc: item.done ? '已完成' : '等待执行',
        done: item.done,
      }))
    },
    recentStudy() {
      return this.resources.slice(0, 3).map((item, index) => ({
        name: item.name,
        time: index === 0 ? '刚刚' : `${index * 20} 分钟前`,
      }))
    },
    profileModules() {
      return [
        { code: 'RS', title: '资料库', text: `${this.resources.length} 个资源正在归档`, target: 'resources' },
        { code: 'CD', title: '知识卡片', text: `${this.cards.length} 张卡片可复习`, target: 'cards' },
        { code: 'ER', title: '错题压制', text: `${this.filteredWrongQuestions.length} 条记录可处理`, target: 'wrong' },
      ]
    },
    level() {
      return Math.max(1, Math.ceil((this.cards.length + this.resources.length + this.plans.filter((item) => item.done).length) / 3))
    },
    profileName() {
      return this.profileForm.nickname || this.profile.nickname || 'ToveloKno'
    },
    profileCoverStyle() {
      return this.profileDiy.backgroundUrl
        ? { '--profile-cover-image': `url("${this.profileBackgroundSrc}")` }
        : {}
    },
    profileAvatarSrc() {
      return resolveAssetUrl(this.profileDiy.avatarUrl)
    },
    profileBackgroundSrc() {
      return resolveAssetUrl(this.profileDiy.backgroundUrl)
    },
    bannerCropImageStyle() {
      return {
        transform: `scale(${this.bannerCrop.zoom})`,
        transformOrigin: `${this.bannerCrop.x}% ${this.bannerCrop.y}%`,
        objectPosition: `${this.bannerCrop.x}% ${this.bannerCrop.y}%`,
      }
    },
  },
  mounted() {
    if (this.isLoggedIn) {
      this.loadProfile()
      this.loadPlans()
      this.loadStudyLinkOverview()
    }
    this.loadHomeBanners().finally(() => this.startBannerAutoplay())
  },
  beforeUnmount() {
    this.stopBannerAutoplay()
  },
  watch: {
    activePanel(panel) {
      if (panel === 'home' && this.isLoggedIn) {
        this.loadStudyLinkOverview()
      }
      if (panel === 'plan' && this.isLoggedIn) {
        this.loadPlans()
      }
    },
  },
  methods: {
    switchPanel(panel) {
      const removedPanels = new Set(['dashboard', 'settings'])
      const targetPanel = removedPanels.has(panel) ? 'home' : panel
      if (!targetPanel || this.activePanel === targetPanel) return
      this.activePanel = targetPanel
      if (typeof window !== 'undefined') {
        window.requestAnimationFrame(() => window.scrollTo({ top: 0, behavior: 'smooth' }))
      }
    },
    setBannerIndex(index) {
      const total = this.recommendationSlides.length
      if (!total) return
      this.homeBannerIndex = (index + total) % total
      this.startBannerAutoplay()
    },
    moveBanner(offset) {
      this.setBannerIndex(this.homeBannerIndex + offset)
    },
    targetTypeLabel(type) {
      return {
        RESOURCE: '资料',
        QUESTION: '题目',
        WRONG_QUESTION: '错题',
      }[type] || '未关联'
    },
    async loadStudyLinkOverview() {
      if (!this.isLoggedIn) return
      try {
        const response = await fetchStudyLinkOverview()
        this.studyLinkOverview = response.data?.data || {}
      } catch {
        this.studyLinkOverview = {}
      }
    },
    startBannerAutoplay() {
      this.stopBannerAutoplay()
      if (typeof window === 'undefined' || this.recommendationSlides.length <= 1) return
      this.bannerTimer = window.setInterval(() => {
        const total = this.recommendationSlides.length
        if (total > 1) {
          this.homeBannerIndex = (this.homeBannerIndex + 1) % total
        }
      }, 5200)
    },
    stopBannerAutoplay() {
      if (typeof window !== 'undefined' && this.bannerTimer) {
        window.clearInterval(this.bannerTimer)
      }
      this.bannerTimer = null
    },
    bannerVisualStyle(slide) {
      return slide?.imageUrl
        ? { '--banner-image': `url("${resolveAssetUrl(slide.imageUrl)}")` }
        : {}
    },
    bannerPreviewStyle(slide) {
      return slide?.imageUrl
        ? { '--preview-image': `url("${resolveAssetUrl(slide.imageUrl)}")` }
        : {}
    },
    openBannerEditor() {
      this.bannerDrafts = cloneHomeBanners(this.homeBanners)
      this.editingBannerIndex = Math.min(this.homeBannerIndex, this.bannerDrafts.length - 1)
      this.showBannerEditor = true
    },
    selectBannerDraft(index) {
      this.editingBannerIndex = index
    },
    async uploadHomeBannerImage(event) {
      const file = event.target.files?.[0]
      event.target.value = ''
      if (!file || !this.bannerDrafts[this.editingBannerIndex]) return
      try {
        this.bannerCrop = {
          active: true,
          source: await readImageAsDataUrl(file),
          zoom: 1,
          x: 50,
          y: 50,
        }
        this.profileMessage = '图片已载入，保存时会自动应用裁剪'
      } catch (error) {
        this.profileMessage = error.message || '图片载入失败'
      }
      setTimeout(() => {
        this.profileMessage = ''
      }, 1800)
    },
    async clearHomeBannerImage() {
      if (!this.bannerDrafts[this.editingBannerIndex]) return
      this.bannerDrafts[this.editingBannerIndex].imageUrl = ''
      this.cancelBannerCrop()
      await this.saveHomeBanners('图片已清除并同步到首页')
    },
    async applyBannerCrop(successMessage) {
      const message = typeof successMessage === 'string' ? successMessage : '图片已保存并同步到首页'
      if (!this.bannerCrop.source || !this.bannerDrafts[this.editingBannerIndex]) return
      try {
        const croppedDataUrl = await this.createCroppedBannerImage()
        const imageFile = dataUrlToFile(croppedDataUrl, `home-banner-${this.editingBannerIndex + 1}.jpg`)
        const response = await uploadHomeBannerImageFile(imageFile)
        const imageUrl = response.data?.data?.imageUrl || ''
        if (!imageUrl) {
          throw new Error('图片上传失败')
        }
        this.bannerDrafts[this.editingBannerIndex].imageUrl = imageUrl
        this.cancelBannerCrop()
        await this.saveHomeBanners(message)
      } catch (error) {
        this.profileMessage = error.response?.data?.message || error.message || '裁剪失败'
        setTimeout(() => {
          this.profileMessage = ''
        }, 1800)
      }
    },
    cancelBannerCrop() {
      this.bannerCrop = {
        active: false,
        source: '',
        zoom: 1,
        x: 50,
        y: 50,
      }
    },
    createCroppedBannerImage() {
      return new Promise((resolve, reject) => {
        const image = new Image()
        image.onerror = () => reject(new Error('图片裁剪失败'))
        image.onload = () => {
          const canvas = document.createElement('canvas')
          canvas.width = BANNER_CROP_WIDTH
          canvas.height = BANNER_CROP_HEIGHT
          const context = canvas.getContext('2d')
          if (!context) {
            reject(new Error('浏览器不支持裁剪'))
            return
          }

          const zoom = Math.max(1, Number(this.bannerCrop.zoom) || 1)
          const baseScale = Math.max(BANNER_CROP_WIDTH / image.width, BANNER_CROP_HEIGHT / image.height)
          const scale = baseScale * zoom
          const drawWidth = image.width * scale
          const drawHeight = image.height * scale
          const maxOffsetX = Math.max(0, drawWidth - BANNER_CROP_WIDTH)
          const maxOffsetY = Math.max(0, drawHeight - BANNER_CROP_HEIGHT)
          const offsetX = (maxOffsetX * (Number(this.bannerCrop.x) || 50)) / 100
          const offsetY = (maxOffsetY * (Number(this.bannerCrop.y) || 50)) / 100

          context.drawImage(image, -offsetX, -offsetY, drawWidth, drawHeight)
          resolve(canvas.toDataURL('image/jpeg', BANNER_IMAGE_QUALITY))
        }
        image.src = this.bannerCrop.source
      })
    },
    async saveHomeBanners(successMessage) {
      const message = typeof successMessage === 'string' ? successMessage : '推荐画幅已保存到网站首页'
      if (this.bannerCrop.active && this.bannerCrop.source) {
        return this.applyBannerCrop(message)
      }
      const targetIndex = this.editingBannerIndex
      try {
        const response = await saveHomeBannersApi({ banners: cloneHomeBanners(this.bannerDrafts) })
        this.applyHomeBannerResponse(response.data?.data, targetIndex)
        this.profileMessage = message
        return true
      } catch (error) {
        this.profileMessage = error.response?.data?.message || '推荐画幅保存失败'
        return false
      } finally {
        setTimeout(() => {
          this.profileMessage = ''
        }, 1800)
      }
    },
    persistHomeBannerDrafts() {
      this.homeBanners = cloneHomeBanners(this.bannerDrafts)
      this.homeBannerIndex = Math.min(this.homeBannerIndex, this.homeBanners.length - 1)
    },
    async resetHomeBanners() {
      try {
        const response = await resetHomeBannersApi()
        this.applyHomeBannerResponse(response.data?.data, 0)
        this.profileMessage = '推荐画幅已恢复默认'
      } catch (error) {
        this.profileMessage = error.response?.data?.message || '恢复默认失败'
      }
      setTimeout(() => {
        this.profileMessage = ''
      }, 1800)
    },
    async loadHomeBanners() {
      try {
        const response = await listHomeBanners()
        this.applyHomeBannerResponse(response.data?.data)
      } catch {
        this.homeBanners = cloneHomeBanners()
        this.bannerDrafts = cloneHomeBanners(this.homeBanners)
      }
    },
    applyHomeBannerResponse(banners, preferredIndex = this.homeBannerIndex) {
      this.homeBanners = cloneHomeBanners(Array.isArray(banners) && banners.length ? banners : DEFAULT_HOME_BANNERS)
      this.bannerDrafts = cloneHomeBanners(this.homeBanners)
      this.homeBannerIndex = Math.min(Math.max(preferredIndex, 0), this.homeBanners.length - 1)
      this.editingBannerIndex = Math.min(Math.max(this.editingBannerIndex, 0), this.bannerDrafts.length - 1)
      this.startBannerAutoplay()
    },
    setAuthMode(mode) {
      this.authMode = mode
      this.message = { type: '', text: '' }
    },
    async submitAuth() {
      if (!this.authForm.username || !this.authForm.password) {
        this.message = { type: 'error', text: '请输入用户名和密码' }
        return
      }
      if (this.authMode === 'register' && this.authForm.password.length < 6) {
        this.message = { type: 'error', text: '密码至少需要 6 位' }
        return
      }
      this.loading = true
      this.message = { type: '', text: '' }
      try {
        if (this.authMode === 'register') {
          await registerUser({
            username: this.authForm.username,
            password: this.authForm.password,
            email: this.authForm.email || undefined,
          })
        }
        await loginUser({
          username: this.authForm.username,
          password: this.authForm.password,
        })
        this.isLoggedIn = true
        this.activePanel = 'home'
        this.message = { type: 'success', text: '' }
        await this.loadProfile()
      } catch (error) {
        this.message = {
          type: 'error',
          text: error.response?.data?.message || (this.authMode === 'register' ? '注册失败' : '登录失败'),
        }
      } finally {
        this.loading = false
      }
    },
    handleLogout() {
      clearAuthToken()
      this.isLoggedIn = false
      this.activePanel = 'home'
    },
    async loadProfile() {
      try {
        const response = await getCurrentUserProfile()
        const data = response.data?.data || response.data || {}
        this.profile = {
          nickname: data.nickname || data.username || this.profile.nickname,
          email: data.email || this.profile.email,
          bio: data.bio || data.description || this.profile.bio,
          avatar: data.avatar || this.profile.avatar,
          profileBackground: data.profileBackground || this.profile.profileBackground,
        }
        this.profileForm = { ...this.profile }
        this.profileDiy.avatarUrl = data.avatar || this.profileDiy.avatarUrl
        this.profileDiy.backgroundUrl = data.profileBackground || this.profileDiy.backgroundUrl
      } catch {
        this.profileForm = { ...this.profile }
      }
    },
    selectResource(resource) {
      this.selectedResourceId = resource.id
    },
    addResource() {
      if (!this.resourceDraft.name) return
      const resource = {
        id: createId('resource'),
        name: this.resourceDraft.name,
        type: this.resourceDraft.type,
        source: '本地',
        time: nowTime(),
        size: this.resourceDraft.size || '未标注',
        path: `/我的资源/${this.resourceDraft.name}`,
        description: '新加入的学习资料，可继续补充描述与标签。',
        icon: this.resourceDraft.type.slice(0, 1).toUpperCase(),
        tone: this.resourceDraft.type === '文档' ? 'blue' : this.resourceDraft.type === '视频' ? 'violet' : 'red',
        favorite: false,
      }
      this.resources.unshift(resource)
      this.selectedResourceId = resource.id
      this.resourceDraft = { name: '', type: '文档', size: '' }
      this.showResourceForm = false
    },
    removeResource(id) {
      this.resources = this.resources.filter((item) => item.id !== id)
      if (this.selectedResourceId === id && this.resources.length) {
        this.selectedResourceId = this.resources[0].id
      }
    },
    toggleFavorite(resource) {
      const target = this.resources.find((item) => item.id === resource.id)
      if (target) target.favorite = !target.favorite
    },
    addCard() {
      if (!this.cardDraft.title || !this.cardDraft.content) return
      this.cards.unshift({
        id: createId('card'),
        title: this.cardDraft.title,
        tag: this.cardDraft.tag || '未分类',
        content: this.cardDraft.content,
        reviewCount: 0,
      })
      this.cardDraft = { title: '', tag: '', content: '' }
      this.showCardForm = false
    },
    reviewCard(card) {
      card.reviewCount += 1
    },
    removeCard(id) {
      this.cards = this.cards.filter((item) => item.id !== id)
    },
    chooseAnswer(option) {
      this.selectedAnswer = option
      this.answers = { ...this.answers, [this.currentQuestion.id]: option }
    },
    answerClass(option) {
      if (!this.selectedAnswer) return ''
      if (option === this.currentQuestion.answer) return 'correct'
      if (option === this.selectedAnswer) return 'wrong'
      return ''
    },
    prevQuestion() {
      this.currentQuestionIndex = (this.currentQuestionIndex + this.questions.length - 1) % this.questions.length
      this.selectedAnswer = this.answers[this.currentQuestion.id] || ''
    },
    nextQuestion() {
      this.currentQuestionIndex = (this.currentQuestionIndex + 1) % this.questions.length
      this.selectedAnswer = this.answers[this.currentQuestion.id] || ''
    },
    collectWrongQuestion() {
      if (this.wrongQuestions.some((item) => item.title === this.currentQuestion.title)) return
      this.wrongQuestions.unshift({
        id: createId('wrong'),
        subject: '练习',
        title: this.currentQuestion.title,
        note: this.currentQuestion.analysis,
        mastered: false,
      })
      this.switchPanel('wrong')
    },
    removeWrongQuestion(id) {
      this.wrongQuestions = this.wrongQuestions.filter((item) => item.id !== id)
    },
    addPlan() {
      if (!this.planDraft.title) return
      this.createPlanFromDraft()
    },
    async createPlanFromDraft() {
      const planDate = new Date().toISOString().slice(0, 10)
      try {
        await createStudyPlan({
          title: this.planDraft.title,
          content: this.planDraft.time ? `计划时间：${this.planDraft.time}` : null,
          planDate,
        })
        this.planDraft = { title: '', time: '' }
        await this.loadPlans()
      } catch (error) {
        this.profileMessage = error.response?.data?.message || '计划保存失败'
      }
    },
    async loadPlans() {
      this.planLoading = true
      try {
        const params = {
          page: this.planPage,
          size: 10,
        }
        if (this.planFilter.status) params.status = this.planFilter.status
        if (this.planFilter.planDate) params.planDate = this.planFilter.planDate

        const response = await listStudyPlans(params)
        const pageData = response.data?.data || {}
        this.plans = (pageData.records || []).map(this.mapStudyPlan)
        this.planTotal = pageData.total || this.plans.length
        this.planTotalPages = pageData.totalPages || 1
      } catch {
        // 后端未启动时保留本地示例计划，方便继续预览界面。
      } finally {
        this.planLoading = false
      }
    },
    mapStudyPlan(plan) {
      return {
        id: plan.id,
        title: plan.title,
        content: plan.content || '',
        planDate: plan.planDate || '',
        time: plan.planDate || '今日',
        status: plan.status || (plan.done ? 'completed' : 'pending'),
        done: plan.status === 'completed' || Boolean(plan.done),
      }
    },
    openPlanEditor(plan = null) {
      this.planFormError = ''
      if (plan) {
        this.editingPlan = plan
        this.planForm = {
          title: plan.title,
          content: plan.content || '',
          planDate: plan.planDate || new Date().toISOString().slice(0, 10),
        }
      } else {
        this.editingPlan = null
        this.planForm = {
          title: '',
          content: '',
          planDate: new Date().toISOString().slice(0, 10),
        }
      }
      this.planEditorOpen = true
    },
    async submitPlan() {
      this.planFormError = ''
      if (!this.planForm.title.trim()) {
        this.planFormError = '请填写计划标题'
        return
      }
      if (!this.planForm.planDate) {
        this.planFormError = '请选择计划日期'
        return
      }

      this.planSubmitting = true
      try {
        const data = {
          title: this.planForm.title.trim(),
          content: this.planForm.content.trim() || null,
          planDate: this.planForm.planDate,
        }
        if (this.editingPlan) {
          await updateStudyPlan(this.editingPlan.id, data)
          this.profileMessage = '计划已更新'
        } else {
          await createStudyPlan(data)
          this.profileMessage = '计划已创建'
        }
        this.planEditorOpen = false
        await this.loadPlans()
      } catch (error) {
        this.planFormError = error.response?.data?.message || '保存失败'
      } finally {
        this.planSubmitting = false
      }
    },
    async removePlan(id) {
      if (!window.confirm('确定要删除这条学习计划吗？')) return
      try {
        await deleteStudyPlan(id)
        if (this.plans.length === 1 && this.planPage > 1) {
          this.planPage -= 1
        }
        await this.loadPlans()
      } catch (error) {
        this.profileMessage = error.response?.data?.message || '删除失败'
      }
    },
    async markPlanStatus(id, status) {
      try {
        await updateStudyPlanStatus(id, status)
        await this.loadPlans()
      } catch (error) {
        this.profileMessage = error.response?.data?.message || '状态更新失败'
      }
    },
    statusLabel(status) {
      return { pending: '待完成', completed: '已完成', cancelled: '已取消' }[status] || status
    },
    refreshPlanFilter() {
      this.planPage = 1
      this.loadPlans()
    },
    async saveProfile() {
      try {
        const response = await updateCurrentUserProfile({
          ...this.profileForm,
          avatar: this.profileDiy.avatarUrl || null,
          profileBackground: this.profileDiy.backgroundUrl || null,
        })
        const data = response.data?.data || {}
        this.profileDiy.avatarUrl = data.avatar || this.profileDiy.avatarUrl
        this.profileDiy.backgroundUrl = data.profileBackground || this.profileDiy.backgroundUrl
      } catch {
        // 后端未启动或接口未完全实现时，仍保留前端本地预览。
      }
      this.profile = { ...this.profileForm }
      this.profileMessage = '资料已保存'
      setTimeout(() => {
        this.profileMessage = ''
      }, 1800)
    },
    async uploadProfileImage(event, type) {
      const file = event.target.files?.[0]
      event.target.value = ''
      if (!file) return
      try {
        const response = await uploadCurrentUserProfileImage(file, type)
        const data = response.data?.data || {}
        const url = data.url
        if (type === 'background') {
          this.profileDiy.backgroundUrl = url
          this.profile.profileBackground = url
          this.profileForm.profileBackground = url
        } else {
          this.profileDiy.avatarUrl = url
          this.profile.avatar = url
          this.profileForm.avatar = url
        }
        if (data.user) {
          this.profile = {
            ...this.profile,
            nickname: data.user.nickname || data.user.username || this.profile.nickname,
            email: data.user.email || this.profile.email,
            bio: data.user.bio || this.profile.bio,
            avatar: data.user.avatar || this.profile.avatar,
            profileBackground: data.user.profileBackground || this.profile.profileBackground,
          }
          this.profileForm = { ...this.profile }
        }
        this.profileMessage = type === 'background' ? '主页背景已上传' : '头像已上传'
      } catch (error) {
        this.profileMessage = error.response?.data?.message || '图片上传失败'
      }
      setTimeout(() => {
        this.profileMessage = ''
      }, 1800)
    },
    async savePassword() {
      if (!this.passwordForm.newPassword || this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
        this.profileMessage = '两次新密码不一致'
        return
      }
      try {
        await changeCurrentUserPassword(this.passwordForm)
        this.profileMessage = '密码已更新'
      } catch {
        this.profileMessage = '密码已在本地校验，后端连接后可同步'
      }
      this.passwordForm = { oldPassword: '', newPassword: '', confirmPassword: '' }
    },
  },
}
</script>
