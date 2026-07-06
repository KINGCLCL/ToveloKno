<template>
  <div class="bank-app" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <aside class="bank-sidebar">
      <button class="bank-brand" type="button" @click="selectNav('questions')">
        <span class="bank-brand-mark"><LineIcon name="book-open" /></span>
        <span><strong>题库模块</strong><small>QUESTION BANK</small></span>
      </button>

      <nav class="bank-nav" aria-label="题库功能导航">
        <button
          v-for="item in navigationItems"
          :key="item.id"
          type="button"
          :class="{ active: activeNav === item.id }"
          :title="sidebarCollapsed ? item.label : undefined"
          @click="selectNav(item.id)"
        >
          <LineIcon :name="item.icon" />
          <span>{{ item.label }}</span>
          <i />
        </button>
      </nav>

      <div class="bank-sidebar-tip">
        <LineIcon name="target" />
        <span><strong>今日目标</strong><small>已完成 {{ practiceDashboard.today.practiceCount }} / {{ practiceDashboard.today.goal }} 题</small><i><b :style="{ width: `${practiceDashboard.today.progress}%` }" /></i></span>
      </div>

      <button class="bank-collapse" type="button" @click="sidebarCollapsed = !sidebarCollapsed">
        <span>{{ sidebarCollapsed ? '›' : '‹' }}</span>
        <span>收起侧栏</span>
      </button>
    </aside>

    <div class="bank-page">
      <header class="bank-topbar">
        <button class="bank-mobile-menu" type="button" aria-label="切换侧栏" @click="sidebarCollapsed = !sidebarCollapsed">
          <LineIcon name="menu" />
        </button>
        <button class="bank-back-home" type="button" @click="emit('back-home')">
          <LineIcon name="chevron-left" />
          <span>学习控制台</span>
        </button>
        <div class="bank-breadcrumb"><span>题库工作台</span><b>/</b><strong>{{ activeNavLabel }}</strong></div>
        <label class="bank-global-search">
          <LineIcon name="search" />
          <input v-model.trim="globalKeyword" type="search" placeholder="搜索题目、知识点、题库..." />
          <kbd>Ctrl K</kbd>
        </label>
        <button class="bank-icon-button" type="button" title="消息" @click="notify('暂无新的系统消息')">
          <LineIcon name="bell" /><i />
        </button>
        <button class="bank-user" type="button" @click="handleUserButton">
          <span class="bank-avatar">
            <img v-if="avatarSrc" :src="avatarSrc" alt="头像" />
            <b v-else>{{ userInitial }}</b>
          </span>
          <span><strong>{{ displayName }}</strong><small>{{ authenticated ? currentUsername : '未登录' }}</small></span>
          <LineIcon name="chevron-down" />
        </button>
        <div v-if="profileOpen" class="bank-profile-menu">
          <button type="button" @click="notify('个人中心正在建设中')">个人中心</button>
          <button type="button" @click="handleLogout">退出登录</button>
        </div>
      </header>

      <main class="bank-content">
        <header class="bank-page-heading">
          <div>
            <p>{{ pageMeta.eyebrow }}</p>
            <h1>{{ pageMeta.title }}</h1>
            <span>{{ pageMeta.description }}</span>
          </div>
          <div class="bank-heading-actions">
            <button v-if="activeNav === 'questions'" type="button" class="bank-btn" :disabled="!authenticated" @click="triggerImport"><LineIcon name="upload" />批量导入</button>
            <button v-if="activeNav === 'questions'" type="button" class="bank-btn primary" @click="openCreateEditor"><LineIcon name="plus-circle" />新增题目</button>
            <input ref="importInput" class="bank-hidden-input" type="file" accept=".json,application/json" @change="handleImportFile" />
            <button v-if="activeNav === 'categories'" type="button" class="bank-btn primary" @click="openCategoryEditor()"><LineIcon name="plus-circle" />新增分类</button>
            <button v-if="activeNav === 'analytics'" type="button" class="bank-btn" @click="exportAnalytics"><LineIcon name="download" />导出报表</button>
            <button v-if="activeNav === 'settings'" type="button" class="bank-btn primary" :disabled="settingsSaving" @click="saveQuestionBankSettings"><LineIcon name="check-square" />{{ settingsSaving ? '保存中...' : '保存设置' }}</button>
          </div>
        </header>

        <template v-if="activeNav === 'questions'">
          <section class="bank-stat-grid four">
            <article v-for="stat in questionStatCards" :key="stat.label" class="bank-stat-card" :class="stat.tone">
              <div><span>{{ stat.label }}</span><strong>{{ stat.value }} <small>{{ stat.unit }}</small></strong><p>{{ stat.note }}</p></div>
              <i><LineIcon :name="stat.icon" /></i>
            </article>
          </section>

          <section class="bank-card bank-table-card">
            <div class="bank-card-head">
              <div><h2>题目列表</h2><span>集中管理题目内容、状态与分类</span></div>
              <div class="bank-segmented">
                <button v-for="tab in ['全部题目', '已发布', '草稿', '回收站']" :key="tab" type="button" :class="{ active: questionTab === tab }" @click="questionTab = tab">{{ tab }}</button>
              </div>
            </div>
            <div class="bank-filterbar">
              <select v-model="filters.categoryId" aria-label="分类"><option :value="null">全部分类</option><option v-for="category in categoryRows" :key="category.id" :value="category.id">{{ categoryOptionLabel(category) }}</option></select>
              <select v-model="filters.type" aria-label="题型"><option value="">全部题型</option><option value="SINGLE_CHOICE">单选题</option><option value="MULTIPLE_CHOICE">多选题</option><option value="TRUE_FALSE">判断题</option><option value="FILL_BLANK">填空题</option><option value="SHORT_ANSWER">简答题</option></select>
              <select v-model="filters.difficulty" aria-label="难度"><option value="">全部难度</option><option value="2">二星</option><option value="3">三星</option><option value="4">四星</option></select>
              <label><LineIcon name="search" /><input v-model.trim="questionKeyword" type="search" placeholder="搜索题目内容" /></label>
              <button type="button" class="bank-btn compact" @click="clearQuestionFilters"><LineIcon name="refresh" />重置</button>
            </div>
            <div v-if="selectedQuestionIds.length" class="bank-batchbar">
              <span>已选择 <strong>{{ selectedQuestionIds.length }}</strong> 道题</span>
              <template v-if="questionTab !== '回收站'">
                <button type="button" @click="handleBatchStatus('PUBLISHED')">批量发布</button>
                <button type="button" @click="handleBatchStatus('DRAFT')">转为草稿</button>
                <button type="button" class="danger" @click="handleBatchDelete">移入回收站</button>
              </template>
              <template v-else>
                <button type="button" @click="handleBatchRestore">批量恢复</button>
              </template>
              <button type="button" @click="selectedQuestionIds = []">取消选择</button>
            </div>
            <div class="bank-table-wrap">
              <table class="bank-table question-table">
                <thead><tr><th><input type="checkbox" :checked="allQuestionsSelected" @change="toggleAllQuestions" /></th><th>题目内容</th><th>科目 / 知识点</th><th>题型</th><th>难度</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-if="questionsLoading"><td colspan="8" class="bank-empty"><span class="bank-spinner" />正在加载题目...</td></tr>
                  <template v-else>
                    <tr v-for="question in questionRows" :key="question.id">
                      <td><input v-model="selectedQuestionIds" type="checkbox" :value="question.id" /></td>
                      <td><button class="bank-title-link" type="button" @click="openViewEditor(question)">{{ question.content }}</button></td>
                      <td><strong>{{ question.subject || '未分类' }}</strong><small>{{ question.knowledgePoint || '暂无知识点' }}</small></td>
                      <td><span class="bank-pill neutral">{{ questionTypeLabel(question.questionType) }}</span></td>
                      <td><span class="bank-stars">{{ difficultyStars(question.difficulty) }}</span></td>
                      <td><span class="bank-pill" :class="question.status === 'DRAFT' ? 'draft' : 'success'">{{ questionStatusLabel(question.status) }}</span></td>
                      <td>{{ formatDate(question.updatedAt) }}</td>
                      <td>
                        <div v-if="questionTab !== '回收站'" class="bank-row-actions">
                          <button type="button" title="编辑" @click="openEditEditor(question)"><LineIcon name="edit" /></button>
                          <button type="button" title="复制" @click="openDuplicateEditor(question)"><LineIcon name="copy" /></button>
                          <button type="button" title="删除" @click="handleDeleteQuestion(question)"><LineIcon name="trash" /></button>
                        </div>
                        <div v-else class="bank-row-actions">
                          <button type="button" title="恢复" @click="handleRestoreQuestion(question)"><LineIcon name="refresh" /></button>
                          <button type="button" title="永久删除" @click="handlePermanentDelete(question)"><LineIcon name="trash" /></button>
                        </div>
                      </td>
                    </tr>
                  </template>
                  <tr v-if="!questionsLoading && !questionRows.length"><td colspan="8" class="bank-empty">{{ authenticated ? '没有找到符合条件的题目' : '登录后即可管理题目' }}</td></tr>
                </tbody>
              </table>
            </div>
            <footer class="bank-table-footer">
              <span>已选择 {{ selectedQuestionIds.length }} 项 · 共 {{ pagination.total }} 条</span>
              <div>
                <button type="button" :disabled="pagination.page <= 1" @click="goToPage(pagination.page - 1)">‹</button>
                <button v-for="page in visiblePages" :key="page" type="button" :class="{ active: pagination.page === page }" @click="goToPage(page)">{{ page }}</button>
                <button type="button" :disabled="pagination.page >= pagination.totalPages" @click="goToPage(pagination.page + 1)">›</button>
              </div>
              <select v-model.number="pagination.size"><option :value="10">10 条 / 页</option><option :value="20">20 条 / 页</option><option :value="50">50 条 / 页</option></select>
            </footer>
          </section>
        </template>

        <template v-else-if="activeNav === 'categories'">
          <section class="bank-relation-summary">
            <article><span>科目分类</span><strong>{{ classificationSummary.categoryCount }}</strong><small>个层级节点</small></article>
            <article><span>知识点</span><strong>{{ classificationSummary.knowledgePointCount }}</strong><small>项题目聚合</small></article>
            <article><span>自定义标签</span><strong>{{ classificationSummary.tagCount }}</strong><small>个可复用标签</small></article>
            <article><span>已分类题目</span><strong>{{ classificationSummary.classifiedQuestionCount }}</strong><small>未分类 {{ classificationSummary.unclassifiedQuestionCount }} 题</small></article>
          </section>
          <section class="bank-category-layout">
            <aside class="bank-card bank-tree-card">
              <div class="bank-card-head compact"><div><h2>科目分类树</h2><span>选择节点可快速筛选</span></div><button type="button" class="bank-mini-add" title="新增一级分类" @click="openCategoryEditor()">＋</button></div>
              <label class="bank-inner-search"><LineIcon name="search" /><input v-model.trim="categoryKeyword" type="search" placeholder="搜索分类" /></label>
              <div class="bank-tree">
                <button type="button" :class="{ active: selectedCategory === null }" @click="selectedCategory = null"><span><LineIcon name="layers" />全部分类</span><small>{{ classificationSummary.categoryCount }}</small></button>
                <button
                  v-for="node in flattenedCategoryTree"
                  :key="node.id"
                  type="button"
                  :class="{ active: selectedCategory === node.id, muted: !node.active }"
                  :style="{ paddingLeft: `${12 + node.depth * 20}px` }"
                  :title="node.children?.length ? '单击展开或收起，双击查看分类题目' : '双击查看分类题目'"
                  @click="toggleCategoryNode(node)"
                  @dblclick="openCategoryQuestionsById(node.id)"
                >
                  <span>
                    <b v-if="node.children?.length" class="bank-tree-chevron" :class="{ expanded: isCategoryExpanded(node.id) }">›</b>
                    <i v-else />
                    <LineIcon v-if="!node.depth" name="folder" />{{ node.name }}
                  </span>
                  <small>{{ node.totalQuestionCount }}</small>
                </button>
                <p v-if="!classificationLoading && !flattenedCategoryTree.length" class="bank-tree-empty">还没有分类，点击右上角 ＋ 创建</p>
              </div>
            </aside>

            <section class="bank-card bank-table-card">
              <div class="bank-card-head">
                <div><h2>分类关系</h2><span>维护科目、知识点、标签与难度</span></div>
                <div class="bank-segmented">
                  <button v-for="tab in classificationTabs" :key="tab" type="button" :class="{ active: categoryTab === tab }" @click="categoryTab = tab">{{ tab }}</button>
                </div>
              </div>
              <div class="bank-filterbar category">
                <label><LineIcon name="search" /><input v-model.trim="categoryTableKeyword" type="search" :placeholder="classificationSearchPlaceholder" /></label>
                <button v-if="categoryTab === '科目管理'" type="button" class="bank-btn compact primary" @click="openCategoryEditor()"><LineIcon name="plus-circle" />新增分类</button>
                <button v-else-if="categoryTab === '标签'" type="button" class="bank-btn compact primary" @click="openTagEditor()"><LineIcon name="plus-circle" />新增标签</button>
                <button v-else type="button" class="bank-btn compact" :disabled="classificationLoading" @click="loadClassificationOverview"><LineIcon name="refresh" />刷新</button>
              </div>
              <div class="bank-table-wrap">
                <table v-if="categoryTab === '科目管理'" class="bank-table relation-table">
                  <thead><tr><th>分类名称</th><th>上级分类</th><th>关联题目</th><th>状态</th><th>排序</th><th>更新时间</th><th>操作</th></tr></thead>
                  <tbody>
                    <tr v-if="classificationLoading"><td colspan="7" class="bank-empty"><span class="bank-spinner" />正在加载分类关系...</td></tr>
                    <tr v-for="row in filteredCategoryRows" v-else :key="row.id">
                      <td><strong>{{ row.name }}</strong><small>{{ row.description || '暂无描述' }}</small></td>
                      <td>{{ row.parentName || '一级分类' }}</td><td><button type="button" class="bank-count-link" @click="openCategoryQuestions(row)">{{ categoryTotalQuestionCount(row.id) }} 题</button></td>
                      <td><span class="bank-pill" :class="row.active ? 'success' : 'draft'">{{ row.active ? '启用' : '停用' }}</span></td>
                      <td>{{ row.sortOrder }}</td><td>{{ formatDateTime(row.updatedAt) }}</td>
                      <td><div class="bank-row-actions"><button type="button" title="查看分类题目" @click="openCategoryQuestions(row)"><LineIcon name="eye" /></button><button type="button" title="新增子分类" @click="openCategoryEditor(null, row.id)"><LineIcon name="plus-circle" /></button><button type="button" title="编辑" @click="openCategoryEditor(row)"><LineIcon name="edit" /></button><button type="button" title="删除" @click="handleDeleteCategory(row)"><LineIcon name="trash" /></button></div></td>
                    </tr>
                    <tr v-if="!classificationLoading && !filteredCategoryRows.length"><td colspan="7" class="bank-empty">暂无符合条件的分类</td></tr>
                  </tbody>
                </table>
                <table v-else-if="categoryTab === '知识点'" class="bank-table relation-table">
                  <thead><tr><th>知识点</th><th>所属科目</th><th>关联题目</th><th>已发布</th><th>发布率</th><th>最近更新</th><th>操作</th></tr></thead>
                  <tbody>
                    <tr v-if="classificationLoading"><td colspan="7" class="bank-empty"><span class="bank-spinner" />正在汇总知识点...</td></tr>
                    <tr v-for="row in filteredKnowledgePointRows" v-else :key="`${row.subject}-${row.name}`">
                      <td><strong>{{ row.name }}</strong></td><td>{{ row.subject || '未分类' }}</td><td>{{ row.questionCount }} 题</td><td>{{ row.publishedCount }} 题</td>
                      <td><span class="bank-rate"><i><b :style="{ width: `${knowledgePublishRate(row)}%` }" /></i>{{ knowledgePublishRate(row) }}%</span></td>
                      <td>{{ formatDateTime(row.updatedAt) }}</td>
                      <td><div class="bank-row-actions"><button type="button" title="重命名" @click="openKnowledgeEditor(row)"><LineIcon name="edit" /></button><button type="button" title="从题目中移除" @click="handleClearKnowledgePoint(row)"><LineIcon name="trash" /></button></div></td>
                    </tr>
                    <tr v-if="!classificationLoading && !filteredKnowledgePointRows.length"><td colspan="7" class="bank-empty">题目中还没有知识点数据</td></tr>
                  </tbody>
                </table>
                <table v-else-if="categoryTab === '标签'" class="bank-table relation-table">
                  <thead><tr><th>标签名称</th><th>用途说明</th><th>创建者</th><th>更新时间</th><th>操作</th></tr></thead>
                  <tbody>
                    <tr v-if="classificationLoading"><td colspan="5" class="bank-empty"><span class="bank-spinner" />正在加载标签...</td></tr>
                    <tr v-for="row in filteredTagRows" v-else :key="row.id">
                      <td><span class="bank-tag-chip"># {{ row.name }}</span></td><td>{{ row.description || '暂无说明' }}</td><td>{{ currentUsername }}</td><td>{{ formatDateTime(row.updatedAt) }}</td>
                      <td><div class="bank-row-actions"><button type="button" title="编辑" @click="openTagEditor(row)"><LineIcon name="edit" /></button><button type="button" title="删除" @click="handleDeleteTag(row)"><LineIcon name="trash" /></button></div></td>
                    </tr>
                    <tr v-if="!classificationLoading && !filteredTagRows.length"><td colspan="5" class="bank-empty">还没有自定义标签</td></tr>
                  </tbody>
                </table>
                <table v-else class="bank-table relation-table">
                  <thead><tr><th>难度等级</th><th>说明</th><th>题目数量</th><th>题库占比</th><th>分布</th></tr></thead>
                  <tbody>
                    <tr v-if="classificationLoading"><td colspan="5" class="bank-empty"><span class="bank-spinner" />正在统计难度分布...</td></tr>
                    <tr v-for="row in filteredDifficultyRows" v-else :key="row.level">
                      <td><span class="bank-stars">{{ difficultyStars(row.level) }}</span></td><td><strong>{{ row.label }}</strong></td><td>{{ row.questionCount }} 题</td><td>{{ row.percentage.toFixed(1) }}%</td>
                      <td><span class="bank-distribution"><i><b :style="{ width: `${row.percentage}%` }" /></i></span></td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <footer class="bank-table-footer relation-footer"><span>共 {{ currentClassificationRowCount }} 条记录</span><div><button class="active" type="button">1</button></div><small>数据随题目实时更新</small></footer>
            </section>
          </section>
        </template>

        <template v-else-if="activeNav === 'practice'">
          <section class="bank-card bank-practice-hero">
            <div class="bank-card-head"><div><h2>选择练习模式</h2><span>系统将从已发布题目中生成练习</span></div><span class="bank-streak">🔥 连续学习 {{ practiceDashboard.streakDays }} 天</span></div>
            <div class="bank-practice-controls">
              <label>练习分类<select v-model="practiceConfig.categoryId"><option :value="null">全部分类</option><option v-for="category in categoryRows" :key="category.id" :value="category.id">{{ categoryOptionLabel(category) }}</option></select></label>
              <label>题目数量<select v-model.number="practiceConfig.count"><option :value="5">5 题</option><option :value="10">10 题</option><option :value="20">20 题</option><option :value="30">30 题</option></select></label>
            </div>
            <div class="bank-mode-grid">
              <button v-for="mode in practiceModes" :key="mode.id" type="button" :disabled="practiceLoading" :class="['bank-mode-card', mode.accent, { active: activePracticeMode === mode.id }]" @click="startPractice(mode.id)">
                <i><LineIcon :name="mode.icon" /></i><strong>{{ mode.label }}</strong><span>{{ mode.description }}</span><b>{{ practiceLoading && activePracticeMode === mode.id ? '生成中...' : '开始练习 →' }}</b>
              </button>
            </div>
          </section>
          <section class="bank-practice-grid">
            <article class="bank-card bank-records">
              <div class="bank-card-head compact"><div><h2>最近答题记录</h2><span>每次提交都会实时记录</span></div><button type="button" class="bank-text-btn" @click="loadPracticeDashboard">刷新</button></div>
              <button v-for="record in practiceDashboard.recentAttempts" :key="`${record.questionId}-${record.answeredAt}`" type="button" class="bank-record-row" @click="openQuestionFromAttempt(record.questionId)">
                <i><LineIcon :name="record.correct ? 'check-square' : 'x-circle'" /></i><span><strong>{{ record.title }}</strong><small>{{ record.subject || '未分类' }} · {{ record.correct ? '回答正确' : '回答错误' }}</small></span><time>{{ formatShortDateTime(record.answeredAt) }}</time><LineIcon name="chevron-right" />
              </button>
              <p v-if="!practiceDashboard.recentAttempts.length" class="bank-panel-empty">完成一次练习后，答题记录会显示在这里</p>
            </article>
            <article class="bank-card bank-today">
              <div class="bank-card-head compact"><div><h2>今日学习</h2><span>目标 {{ practiceDashboard.today.goal }} 题</span></div></div>
              <div class="bank-today-grid"><div><span>今日练习</span><strong>{{ practiceDashboard.today.practiceCount }}<small>题</small></strong></div><div><span>错题重练</span><strong>{{ practiceDashboard.today.wrongReviewCount }}<small>题</small></strong></div><div><span>待复习错题</span><strong>{{ practiceDashboard.today.activeWrongCount }}<small>题</small></strong></div><div><span>预计时长</span><strong>{{ practiceDashboard.today.estimatedMinutes }}<small>分钟</small></strong></div></div>
              <div class="bank-goal"><span>每日目标 <b>{{ practiceDashboard.today.progress }}%</b></span><i><b :style="{ width: `${practiceDashboard.today.progress}%` }" /></i></div>
            </article>
            <article class="bank-card bank-recommend">
              <div class="bank-card-head compact"><div><h2>薄弱知识点推荐</h2><span>基于真实答题正确率</span></div></div>
              <div v-for="point in practiceDashboard.weakPoints" :key="point.name" class="bank-progress-row"><span>{{ point.name }}</span><i><b :style="{ width: `${point.accuracy}%` }" /></i><strong>{{ point.accuracy }}%</strong></div>
              <p v-if="!practiceDashboard.weakPoints.length" class="bank-panel-empty compact">积累更多答题记录后生成推荐</p>
              <button type="button" class="bank-btn primary wide" :disabled="!practiceDashboard.weakPoints.length" @click="startPractice('weak')">生成专项练习</button>
            </article>
          </section>
        </template>

        <template v-else-if="activeNav === 'analytics'">
          <section class="bank-stat-grid five">
            <article v-for="stat in analyticsStatCards" :key="stat.label" class="bank-stat-card">
              <div><span>{{ stat.label }}</span><strong>{{ stat.value }} <small>{{ stat.unit }}</small></strong><p>{{ stat.note }}</p></div><i><LineIcon :name="stat.icon" /></i>
            </article>
          </section>
          <section class="bank-analytics-grid">
            <article class="bank-card bank-trend-card">
              <div class="bank-card-head compact"><div><h2>学习趋势</h2><span>近 7 天答题正确率</span></div><button type="button" class="bank-text-btn" @click="loadAnalytics">刷新</button></div>
              <div class="bank-line-chart">
                <div class="bank-chart-y"><span>100%</span><span>75%</span><span>50%</span><span>25%</span><span>0</span></div>
                <svg viewBox="0 0 720 220" preserveAspectRatio="none" aria-label="学习趋势折线图">
                  <defs><linearGradient id="trendFill" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#6386ed" stop-opacity=".28"/><stop offset="1" stop-color="#6386ed" stop-opacity="0"/></linearGradient></defs>
                  <polygon class="area" :points="trendAreaPoints" />
                  <polyline class="line" :points="trendLinePoints" />
                </svg>
                <div class="bank-chart-x"><span v-for="point in analyticsData.trend" :key="point.date">{{ formatMonthDay(point.date) }}</span></div>
              </div>
            </article>
            <article class="bank-card bank-donut-card">
              <div class="bank-card-head compact"><div><h2>题型正确率分布</h2><span>各类题型平均表现</span></div></div>
              <div class="bank-donut-content"><div class="bank-donut"><span><strong>{{ analyticsData.summary.accuracy }}%</strong><small>平均正确率</small></span></div><div class="bank-legend"><span v-for="segment in typeAccuracySegments" :key="segment.label"><i :style="{ background: segment.color }" /><b>{{ segment.label }}</b><strong>{{ segment.value }}%</strong></span><small v-if="!typeAccuracySegments.length">暂无答题数据</small></div></div>
            </article>
            <article class="bank-card bank-knowledge-card">
              <div class="bank-card-head compact"><div><h2>知识点掌握情况</h2><span>按正确率从低到高排序</span></div></div>
              <div v-for="point in analyticsData.weakPoints" :key="point.name" class="bank-progress-row"><span>{{ point.name }}</span><i><b :style="{ width: `${point.accuracy}%` }" /></i><strong>{{ point.accuracy }}%</strong></div>
              <p v-if="!analyticsData.weakPoints.length" class="bank-panel-empty compact">暂无知识点答题数据</p>
            </article>
            <article class="bank-card bank-rank-card">
              <div class="bank-card-head compact"><div><h2>薄弱知识点排行</h2><span>建议优先复习</span></div></div>
              <table class="bank-mini-table"><thead><tr><th>知识点</th><th>错题</th><th>正确率</th><th></th></tr></thead><tbody><tr v-for="row in analyticsData.weakPoints" :key="row.name"><td>{{ row.name }}</td><td>{{ row.wrongCount }}</td><td>{{ row.accuracy }}%</td><td><button type="button" @click="startPractice('weak', row.name)">练习</button></td></tr><tr v-if="!analyticsData.weakPoints.length"><td colspan="4">暂无薄弱项</td></tr></tbody></table>
            </article>
          </section>
        </template>

        <template v-else>
          <section class="bank-settings-grid">
            <article class="bank-card bank-settings-card">
              <div class="bank-card-head compact"><div><h2>我的题库</h2><span>基础信息与功能偏好</span></div><LineIcon name="database" /></div>
              <dl class="bank-info-list"><div><dt>题库名称</dt><dd><input v-model="settings.bankName" maxlength="100" /></dd></div><div><dt>题库描述</dt><dd><textarea v-model="settings.description" maxlength="500" /></dd></div><div><dt>创建时间</dt><dd>{{ formatDateTime(settings.createdAt) }}</dd></div><div><dt>题目总数</dt><dd>{{ questionStatsData.total }} 题</dd></div></dl>
              <div class="bank-toggle-list">
                <label><span><strong>允许成员新增题目</strong><small>编辑员可创建与修改题目</small></span><input v-model="settings.memberEdit" type="checkbox" role="switch" /></label>
                <label><span><strong>允许成员批量导出</strong><small>开放题目数据导出权限</small></span><input v-model="settings.memberExport" type="checkbox" role="switch" /></label>
                <label><span><strong>题目审核后发布</strong><small>新增题目默认进入待审核状态</small></span><input v-model="settings.reviewRequired" type="checkbox" role="switch" /></label>
              </div>
            </article>
            <article class="bank-card bank-settings-card">
              <div class="bank-card-head compact"><div><h2>默认设置</h2><span>控制练习和题目展示方式</span></div><LineIcon name="settings" /></div>
              <div class="bank-form-grid"><label>每次练习题目数<select v-model.number="settings.practiceCount"><option :value="5">5</option><option :value="10">10</option><option :value="20">20</option><option :value="30">30</option><option :value="50">50</option></select></label><label>默认难度<select v-model="settings.difficulty"><option>简单</option><option>中等</option><option>困难</option></select></label><label>题目排序<select v-model="settings.sort"><option>随机排序</option><option>创建时间</option><option>难度优先</option></select></label><label>答题后显示解析<select v-model="settings.showAnswer"><option>立即显示</option><option>完成后显示</option></select></label></div>
            </article>
          </section>
        </template>
      </main>
    </div>

    <div v-if="authModalOpen" class="bank-modal-backdrop" @click.self="closeAuthModal">
      <section class="bank-modal bank-auth-modal" role="dialog" aria-modal="true" aria-label="登录题库">
        <header>
          <div><span class="bank-brand-mark"><LineIcon name="book-open" /></span><div><h2>{{ authMode === 'login' ? '登录题库' : '创建学习账号' }}</h2><p>登录后即可保存和管理你的题目</p></div></div>
          <button type="button" aria-label="关闭" @click="closeAuthModal">×</button>
        </header>
        <div class="bank-auth-tabs">
          <button type="button" :class="{ active: authMode === 'login' }" @click="authMode = 'login'; authError = ''">登录</button>
          <button type="button" :class="{ active: authMode === 'register' }" @click="authMode = 'register'; authError = ''">注册</button>
        </div>
        <form class="bank-modal-form" @submit.prevent="handleAuthSubmit">
          <label><span>用户名</span><input v-model.trim="authForm.username" required minlength="3" maxlength="50" autocomplete="username" placeholder="请输入用户名" /></label>
          <label v-if="authMode === 'register'"><span>邮箱（选填）</span><input v-model.trim="authForm.email" type="email" autocomplete="email" placeholder="name@example.com" /></label>
          <label><span>密码</span><input v-model="authForm.password" required minlength="6" maxlength="72" type="password" :autocomplete="authMode === 'login' ? 'current-password' : 'new-password'" placeholder="请输入密码" /></label>
          <p v-if="authError" class="bank-form-error">{{ authError }}</p>
          <button class="bank-btn primary bank-submit" type="submit" :disabled="authSubmitting">
            <span v-if="authSubmitting" class="bank-spinner small" />{{ authSubmitting ? '处理中...' : authMode === 'login' ? '登录并进入题库' : '注册并登录' }}
          </button>
        </form>
      </section>
    </div>

    <div v-if="editorOpen" class="bank-modal-backdrop" @click.self="closeEditor">
      <section class="bank-modal bank-editor-modal" role="dialog" aria-modal="true" aria-label="题目编辑器">
        <header>
          <div><span class="bank-modal-icon"><LineIcon :name="editorMode === 'view' ? 'eye' : 'edit'" /></span><div><h2>{{ editorTitle }}</h2><p>{{ editorMode === 'view' ? '查看题目答案与解析' : '完善题干、答案和分类信息' }}</p></div></div>
          <button type="button" aria-label="关闭" @click="closeEditor">×</button>
        </header>
        <form class="bank-modal-form bank-editor-form" @submit.prevent="submitQuestion">
          <div class="bank-form-grid modal-grid">
            <label><span>题型 *</span><select v-model="questionForm.questionType" :disabled="editorMode === 'view'" required><option v-for="option in questionTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option></select></label>
            <label><span>难度 *</span><select v-model.number="questionForm.difficulty" :disabled="editorMode === 'view'" required><option v-for="level in 5" :key="level" :value="level">{{ level }} 星</option></select></label>
            <label><span>所属分类</span><select v-model="questionForm.categoryId" :disabled="editorMode === 'view'" @change="syncQuestionSubject"><option :value="null">暂不分类</option><option v-for="category in activeCategoryOptions" :key="category.id" :value="category.id">{{ categoryOptionLabel(category) }}</option></select></label>
            <label><span>科目</span><input v-model.trim="questionForm.subject" :disabled="editorMode === 'view'" maxlength="80" placeholder="选择分类后自动填充" /></label>
            <label><span>知识点</span><input v-model.trim="questionForm.knowledgePoint" :disabled="editorMode === 'view'" maxlength="80" placeholder="例如：事务管理" /></label>
          </div>
          <label><span>题目内容 *</span><textarea v-model.trim="questionForm.content" :disabled="editorMode === 'view'" required maxlength="10000" rows="4" placeholder="请输入题干" /></label>

          <div v-if="isChoiceQuestion" class="bank-option-editor">
            <div class="bank-option-head"><span>选项（至少两个）</span><button v-if="editorMode !== 'view'" type="button" @click="addOption">＋ 添加选项</button></div>
            <label v-for="(option, index) in questionForm.options" :key="index">
              <b>{{ String.fromCharCode(65 + index) }}</b>
              <input v-model.trim="questionForm.options[index]" :disabled="editorMode === 'view'" required maxlength="500" :placeholder="`选项 ${String.fromCharCode(65 + index)}`" />
              <button v-if="editorMode !== 'view' && questionForm.options.length > 2" type="button" aria-label="删除选项" @click="removeOption(index)">×</button>
            </label>
          </div>

          <div class="bank-form-grid modal-grid">
            <label><span>正确答案 *</span><input v-model.trim="questionForm.correctAnswer" :disabled="editorMode === 'view'" required maxlength="1000" placeholder="例如：A；多选可填写 A,B" /></label>
            <label><span>发布状态 *</span><select v-model="questionForm.status" :disabled="editorMode === 'view'" required><option value="DRAFT">草稿</option><option value="PUBLISHED">已发布</option></select></label>
          </div>
          <label><span>答案解析</span><textarea v-model.trim="questionForm.analysis" :disabled="editorMode === 'view'" maxlength="10000" rows="3" placeholder="请输入解题思路或知识点说明" /></label>
          <p v-if="editorError" class="bank-form-error">{{ editorError }}</p>
          <footer>
            <button class="bank-btn" type="button" @click="closeEditor">{{ editorMode === 'view' ? '关闭' : '取消' }}</button>
            <button v-if="editorMode === 'view'" class="bank-btn primary" type="button" @click="editorMode = 'edit'"><LineIcon name="edit" />编辑题目</button>
            <button v-else class="bank-btn primary" type="submit" :disabled="questionSaving"><span v-if="questionSaving" class="bank-spinner small" />{{ questionSaving ? '保存中...' : '保存题目' }}</button>
          </footer>
        </form>
      </section>
    </div>

    <div v-if="practiceModalOpen" class="bank-modal-backdrop" @click.self="closePractice">
      <section class="bank-modal bank-practice-modal" role="dialog" aria-modal="true" aria-label="在线练习">
        <header>
          <div><span class="bank-modal-icon"><LineIcon name="target" /></span><div><h2>在线练习</h2><p>{{ practiceModeLabel }} · 第 {{ practiceIndex + 1 }} / {{ practiceQuestions.length }} 题</p></div></div>
          <button type="button" aria-label="关闭" @click="closePractice">×</button>
        </header>
        <div class="bank-practice-progress"><i><b :style="{ width: `${practiceProgress}%` }" /></i><span>{{ practiceProgress }}%</span></div>
        <form v-if="currentPracticeQuestion" class="bank-practice-question" @submit.prevent="submitCurrentAnswer">
          <div class="bank-practice-meta"><span>{{ questionTypeLabel(currentPracticeQuestion.questionType) }}</span><span>{{ currentPracticeQuestion.subject || '未分类' }}</span><span>{{ difficultyStars(currentPracticeQuestion.difficulty) }}</span></div>
          <h3>{{ currentPracticeQuestion.content }}</h3>
          <div v-if="currentPracticeQuestion.options?.length" class="bank-answer-options">
            <button
              v-for="(option, index) in currentPracticeQuestion.options"
              :key="`${currentPracticeQuestion.id}-${index}`"
              type="button"
              :disabled="Boolean(practiceResult)"
              :class="{ selected: isPracticeOptionSelected(index) }"
              @click="togglePracticeOption(index)"
            >
              <b>{{ String.fromCharCode(65 + index) }}</b><span>{{ option }}</span>
            </button>
          </div>
          <label v-else class="bank-text-answer"><span>你的答案</span><textarea v-model.trim="practiceAnswer" :disabled="Boolean(practiceResult)" rows="4" maxlength="1000" placeholder="请输入答案" /></label>
          <section v-if="practiceResult" class="bank-answer-result" :class="{ correct: practiceResult.correct, wrong: !practiceResult.correct }">
            <h4>{{ practiceResult.correct ? '回答正确' : '回答错误' }}</h4>
            <p v-if="!practiceResult.correct"><strong>正确答案：</strong>{{ practiceResult.correctAnswer }}</p>
            <p><strong>答案解析：</strong>{{ practiceResult.analysis || '暂无解析' }}</p>
            <small v-if="practiceResult.mastered">这道错题已标记为掌握</small>
          </section>
          <p v-if="practiceError" class="bank-form-error">{{ practiceError }}</p>
          <footer>
            <button class="bank-btn" type="button" @click="closePractice">结束练习</button>
            <button v-if="!practiceResult" class="bank-btn primary" type="submit" :disabled="answerSubmitting || !practiceAnswer"><span v-if="answerSubmitting" class="bank-spinner small" />{{ answerSubmitting ? '判题中...' : '提交答案' }}</button>
            <button v-else class="bank-btn primary" type="button" @click="nextPracticeQuestion">{{ practiceIndex + 1 >= practiceQuestions.length ? '完成练习' : '下一题' }}</button>
          </footer>
        </form>
      </section>
    </div>

    <div v-if="classificationModalOpen" class="bank-modal-backdrop" @click.self="closeClassificationModal">
      <section class="bank-modal bank-classification-modal" role="dialog" aria-modal="true" :aria-label="classificationModalTitle">
        <header>
          <div><span class="bank-modal-icon"><LineIcon :name="classificationModalKind === 'tag' ? 'tag' : 'layers'" /></span><div><h2>{{ classificationModalTitle }}</h2><p>{{ classificationModalDescription }}</p></div></div>
          <button type="button" aria-label="关闭" @click="closeClassificationModal">×</button>
        </header>
        <form class="bank-modal-form" @submit.prevent="submitClassificationForm">
          <template v-if="classificationModalKind === 'category'">
            <div class="bank-form-grid modal-grid">
              <label><span>分类名称 *</span><input v-model.trim="classificationForm.name" required maxlength="80" placeholder="例如：数据库系统" /></label>
              <label><span>上级分类</span><select v-model="classificationForm.parentId"><option :value="null">一级分类</option><option v-for="category in availableParentCategories" :key="category.id" :value="category.id">{{ categoryOptionLabel(category) }}</option></select></label>
              <label><span>显示顺序</span><input v-model.number="classificationForm.sortOrder" type="number" min="0" max="9999" required /></label>
              <label><span>状态</span><select v-model="classificationForm.active"><option :value="true">启用</option><option :value="false">停用</option></select></label>
            </div>
            <label><span>分类描述</span><textarea v-model.trim="classificationForm.description" maxlength="255" rows="3" placeholder="说明该分类覆盖的课程或内容范围" /></label>
          </template>
          <template v-else-if="classificationModalKind === 'tag'">
            <label><span>标签名称 *</span><input v-model.trim="classificationForm.name" required maxlength="50" placeholder="例如：高频考点" /></label>
            <label><span>用途说明</span><textarea v-model.trim="classificationForm.description" maxlength="255" rows="3" placeholder="说明何时使用这个标签" /></label>
          </template>
          <template v-else>
            <div class="bank-form-grid modal-grid">
              <label><span>原知识点</span><input :value="classificationForm.oldName" disabled /></label>
              <label><span>所属科目</span><input :value="classificationForm.subject || '未分类'" disabled /></label>
            </div>
            <label><span>新知识点名称 *</span><input v-model.trim="classificationForm.name" required maxlength="80" placeholder="请输入新的知识点名称" /></label>
            <p class="bank-form-hint">重命名会同步更新该科目下所有关联题目。</p>
          </template>
          <p v-if="classificationFormError" class="bank-form-error">{{ classificationFormError }}</p>
          <footer class="bank-modal-actions">
            <button class="bank-btn" type="button" @click="closeClassificationModal">取消</button>
            <button class="bank-btn primary" type="submit" :disabled="classificationSaving"><span v-if="classificationSaving" class="bank-spinner small" />{{ classificationSaving ? '保存中...' : '确认保存' }}</button>
          </footer>
        </form>
      </section>
    </div>

    <Transition name="bank-toast">
      <div v-if="toastMessage" class="bank-toast"><LineIcon name="check-square" />{{ toastMessage }}</div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  getAuthToken,
  getCurrentUserProfile,
  loginUser,
  logoutUser,
  registerUser,
  resolveAssetUrl,
} from '../../../api'
import LineIcon from '../components/LineIcon.vue'
import {
  navigationItems,
  practiceModes,
} from '../data/questionBankWorkspaceMock'
import {
  clearKnowledgePoint,
  createCategory as createClassificationCategory,
  createTag as createClassificationTag,
  deleteCategory as deleteClassificationCategory,
  deleteTag as deleteClassificationTag,
  fetchClassificationOverview,
  renameKnowledgePoint,
  updateCategory as updateClassificationCategory,
  updateTag as updateClassificationTag,
} from '../classificationApi'
import {
  batchMoveQuestionsToRecycleBin,
  batchUpdateQuestionStatus,
  createQuestion,
  fetchQuestion,
  fetchQuestions,
  fetchQuestionStats,
  moveQuestionToRecycleBin,
  permanentlyDeleteQuestion,
  restoreQuestion,
  updateQuestion,
} from '../questionApi'
import {
  fetchPracticeDashboard,
  fetchQuestionBankAnalytics,
  fetchQuestionBankSettings,
  generatePractice,
  submitPracticeAnswer,
  updateQuestionBankSettings,
} from '../questionBankApi'

const emit = defineEmits(['back-home'])
const initialNav = window.location.hash.slice(1)
const activeNav = ref(navigationItems.some((item) => item.id === initialNav) ? initialNav : 'questions')
const sidebarCollapsed = ref(false)
const profileOpen = ref(false)
const globalKeyword = ref('')
const questionKeyword = ref('')
const questionTab = ref('全部题目')
const categoryKeyword = ref('')
const categoryTableKeyword = ref('')
const categoryTab = ref('科目管理')
const selectedCategory = ref(null)
const expandedCategoryIds = ref(new Set())
const classificationTreeInitialized = ref(false)
const activePracticeMode = ref('')
const selectedQuestionIds = ref([])
const toastMessage = ref('')
const authenticated = ref(Boolean(getAuthToken()))
const currentUsername = ref('访客')
const currentNickname = ref('')
const userAvatarUrl = ref('')
const authModalOpen = ref(!authenticated.value)
const authMode = ref('login')
const authSubmitting = ref(false)
const authError = ref('')
const questionsLoading = ref(false)
const questionRows = ref([])
const questionStatsData = reactive({ total: 0, published: 0, draft: 0, recycleBin: 0 })
const importInput = ref(null)
const editorOpen = ref(false)
const editorMode = ref('create')
const editingQuestionId = ref(null)
const questionSaving = ref(false)
const editorError = ref('')
const classificationLoading = ref(false)
const classificationModalOpen = ref(false)
const classificationModalKind = ref('category')
const editingClassificationId = ref(null)
const classificationSaving = ref(false)
const classificationFormError = ref('')
const categoryRows = ref([])
const categoryTree = ref([])
const knowledgePointRows = ref([])
const tagRows = ref([])
const difficultyRows = ref([])
const practiceLoading = ref(false)
const practiceModalOpen = ref(false)
const practiceQuestions = ref([])
const practiceIndex = ref(0)
const practiceAnswer = ref('')
const practiceResult = ref(null)
const practiceError = ref('')
const answerSubmitting = ref(false)
const settingsSaving = ref(false)
const classificationSummary = reactive({
  categoryCount: 0,
  knowledgePointCount: 0,
  tagCount: 0,
  classifiedQuestionCount: 0,
  unclassifiedQuestionCount: 0,
})
let toastTimer
let searchTimer

const filters = reactive({ subject: '', categoryId: null, type: '', difficulty: '' })
const pagination = reactive({ page: 1, size: 10, total: 0, totalPages: 1 })
const authForm = reactive({ username: '', password: '', email: '' })
const practiceConfig = reactive({ categoryId: null, count: 10 })
const practiceDashboard = reactive({
  streakDays: 0,
  recentAttempts: [],
  today: {
    practiceCount: 0,
    wrongReviewCount: 0,
    activeWrongCount: 0,
    estimatedMinutes: 0,
    goal: 10,
    progress: 0,
  },
  weakPoints: [],
})
const analyticsData = reactive({
  summary: {
    totalQuestions: 0,
    answeredQuestions: 0,
    accuracy: 0,
    activeWrongCount: 0,
    estimatedMinutes: 0,
  },
  trend: [],
  typeAccuracy: [],
  weakPoints: [],
})
const classificationForm = reactive({
  name: '',
  description: '',
  parentId: null,
  active: true,
  sortOrder: 0,
  oldName: '',
  subject: '',
})
const questionForm = reactive({
  content: '',
  questionType: 'SINGLE_CHOICE',
  options: ['', ''],
  correctAnswer: '',
  analysis: '',
  difficulty: 3,
  subject: '',
  knowledgePoint: '',
  status: 'DRAFT',
  categoryId: null,
})
const settings = reactive({
  bankName: '计算机基础题库',
  description: '涵盖计算机专业核心课程与常用知识点',
  memberEdit: true,
  memberExport: true,
  reviewRequired: false,
  practiceCount: 10,
  difficulty: '中等',
  sort: '随机排序',
  showAnswer: '立即显示',
  createdAt: null,
  updatedAt: null,
})

const pageContent = {
  questions: { eyebrow: 'QUESTION MANAGEMENT', title: '题目管理', description: '高效整理、筛选与维护你的全部题目' },
  categories: { eyebrow: 'CATEGORY RELATION', title: '分类关系', description: '构建清晰的科目、知识点和标签体系' },
  practice: { eyebrow: 'PRACTICE & REVIEW', title: '练习与复习', description: '智能选择练习方式，稳步提升知识掌握度' },
  analytics: { eyebrow: 'LEARNING ANALYTICS', title: '数据统计', description: '从学习数据中发现进步，也看见下一步方向' },
  settings: { eyebrow: 'BANK SETTINGS', title: '题库设置', description: '配置题库信息、共享权限与默认规则' },
}

const activeNavLabel = computed(() => navigationItems.find((item) => item.id === activeNav.value)?.label)
const pageMeta = computed(() => pageContent[activeNav.value])
const avatarSrc = computed(() => resolveAssetUrl(userAvatarUrl.value))
const displayName = computed(() => currentNickname.value || currentUsername.value)
const userInitial = computed(() => displayName.value.slice(0, 1).toUpperCase() || '访')
const questionStatCards = computed(() => [
  { label: '题目总数', value: questionStatsData.total.toLocaleString(), unit: '题', note: '当前题库有效题目', icon: 'book-open', tone: 'blue' },
  { label: '已发布', value: questionStatsData.published.toLocaleString(), unit: '题', note: ratioNote(questionStatsData.published), icon: 'check-square', tone: 'green' },
  { label: '草稿数', value: questionStatsData.draft.toLocaleString(), unit: '题', note: ratioNote(questionStatsData.draft), icon: 'edit', tone: 'orange' },
  { label: '回收站', value: questionStatsData.recycleBin.toLocaleString(), unit: '题', note: '支持恢复或永久删除', icon: 'trash', tone: 'purple' },
])
const currentPracticeQuestion = computed(() => practiceQuestions.value[practiceIndex.value] || null)
const practiceProgress = computed(() => {
  if (!practiceQuestions.value.length) return 0
  return Math.round(((practiceIndex.value + (practiceResult.value ? 1 : 0)) / practiceQuestions.value.length) * 100)
})
const practiceModeLabel = computed(() => practiceModes.find((mode) => mode.id === activePracticeMode.value)?.label || '专项练习')
const analyticsStatCards = computed(() => [
  { label: '题目总数', value: analyticsData.summary.totalQuestions.toLocaleString(), unit: '题', note: '当前有效题目', icon: 'book-open' },
  { label: '已练题目', value: analyticsData.summary.answeredQuestions.toLocaleString(), unit: '题', note: '去重统计', icon: 'check-square' },
  { label: '正确率', value: analyticsData.summary.accuracy, unit: '%', note: '全部答题记录', icon: 'target' },
  { label: '待复习错题', value: analyticsData.summary.activeWrongCount, unit: '题', note: '答对可标记掌握', icon: 'x-circle' },
  { label: '预计学习', value: analyticsData.summary.estimatedMinutes, unit: '分钟', note: '按每题 2 分钟估算', icon: 'clock' },
])
const trendLinePoints = computed(() => analyticsData.trend.map((point, index) => {
  const x = analyticsData.trend.length <= 1 ? 360 : (index * 720) / (analyticsData.trend.length - 1)
  const y = 220 - (point.accuracy / 100) * 200
  return `${x},${y}`
}).join(' '))
const trendAreaPoints = computed(() => {
  if (!trendLinePoints.value) return ''
  return `0,220 ${trendLinePoints.value} 720,220`
})
const typeAccuracySegments = computed(() => {
  const labels = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '简答题',
  }
  const colors = ['#5d7fea', '#58c8be', '#f0aa59', '#e66d80', '#8c6bd9']
  return analyticsData.typeAccuracy.map((item, index) => ({
    label: labels[item.questionType] || item.questionType,
    value: item.accuracy,
    color: colors[index % colors.length],
  }))
})
const allQuestionsSelected = computed(() => questionRows.value.length > 0 && questionRows.value.every((item) => selectedQuestionIds.value.includes(item.id)))
const visiblePages = computed(() => {
  const total = Math.max(pagination.totalPages, 1)
  const start = Math.max(1, Math.min(pagination.page - 1, total - 2))
  return Array.from({ length: Math.min(3, total) }, (_, index) => start + index)
})
const questionTypeOptions = [
  { value: 'SINGLE_CHOICE', label: '单选题' },
  { value: 'MULTIPLE_CHOICE', label: '多选题' },
  { value: 'TRUE_FALSE', label: '判断题' },
  { value: 'FILL_BLANK', label: '填空题' },
  { value: 'SHORT_ANSWER', label: '简答题' },
]
const classificationTabs = ['科目管理', '知识点', '标签', '难度']
const isChoiceQuestion = computed(() => ['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(questionForm.questionType))
const editorTitle = computed(() => {
  if (editorMode.value === 'view') return '题目详情'
  return editingQuestionId.value ? '编辑题目' : '新增题目'
})
const flattenedCategoryTree = computed(() => {
  const keyword = categoryKeyword.value.toLowerCase()
  const filtered = filterCategoryNodes(categoryTree.value, keyword)
  return flattenCategoryNodes(filtered, 0, Boolean(keyword))
})
const selectedCategoryRow = computed(() => categoryRows.value.find((row) => row.id === selectedCategory.value))
const selectedCategoryIds = computed(() => {
  if (selectedCategory.value === null) return null
  const result = new Set([selectedCategory.value])
  let changed = true
  while (changed) {
    changed = false
    categoryRows.value.forEach((category) => {
      if (result.has(category.parentId) && !result.has(category.id)) {
        result.add(category.id)
        changed = true
      }
    })
  }
  return result
})
const filteredCategoryRows = computed(() => {
  const keyword = categoryTableKeyword.value.toLowerCase()
  return categoryRows.value.filter((row) => {
    const matchesKeyword = !keyword
      || row.name.toLowerCase().includes(keyword)
      || (row.description || '').toLowerCase().includes(keyword)
      || (row.parentName || '').toLowerCase().includes(keyword)
    const matchesSelection = selectedCategoryIds.value === null || selectedCategoryIds.value.has(row.id)
    return matchesKeyword && matchesSelection
  })
})
const filteredKnowledgePointRows = computed(() => {
  const keyword = categoryTableKeyword.value.toLowerCase()
  const selectedName = selectedCategoryRow.value?.name
  return knowledgePointRows.value.filter((row) => {
    const matchesKeyword = !keyword
      || row.name.toLowerCase().includes(keyword)
      || (row.subject || '').toLowerCase().includes(keyword)
    const matchesSelection = !selectedName || row.subject === selectedName
    return matchesKeyword && matchesSelection
  })
})
const filteredTagRows = computed(() => {
  const keyword = categoryTableKeyword.value.toLowerCase()
  return tagRows.value.filter((row) => !keyword
    || row.name.toLowerCase().includes(keyword)
    || (row.description || '').toLowerCase().includes(keyword))
})
const filteredDifficultyRows = computed(() => {
  const keyword = categoryTableKeyword.value.toLowerCase()
  return difficultyRows.value.filter((row) => !keyword
    || row.label.toLowerCase().includes(keyword)
    || String(row.level).includes(keyword))
})
const activeCategoryOptions = computed(() => categoryRows.value.filter(
  (category) => category.active || category.id === questionForm.categoryId,
))
const availableParentCategories = computed(() => {
  if (!editingClassificationId.value) return categoryRows.value
  const forbidden = collectDescendantCategoryIds(editingClassificationId.value)
  forbidden.add(editingClassificationId.value)
  return categoryRows.value.filter((category) => !forbidden.has(category.id))
})
const currentClassificationRowCount = computed(() => {
  if (categoryTab.value === '科目管理') return filteredCategoryRows.value.length
  if (categoryTab.value === '知识点') return filteredKnowledgePointRows.value.length
  if (categoryTab.value === '标签') return filteredTagRows.value.length
  return filteredDifficultyRows.value.length
})
const classificationSearchPlaceholder = computed(() => ({
  科目管理: '搜索分类、描述或上级分类',
  知识点: '搜索知识点或所属科目',
  标签: '搜索标签名称或用途',
  难度: '搜索难度等级',
}[categoryTab.value]))
const classificationModalTitle = computed(() => {
  if (classificationModalKind.value === 'knowledge') return '重命名知识点'
  if (classificationModalKind.value === 'tag') return editingClassificationId.value ? '编辑标签' : '新增标签'
  return editingClassificationId.value ? '编辑分类' : '新增分类'
})
const classificationModalDescription = computed(() => ({
  category: '设置清晰的科目层级，让题目更容易检索',
  tag: '创建可复用的内容标记',
  knowledge: '同步更新所有关联题目的知识点名称',
}[classificationModalKind.value]))

onMounted(initializeSession)

watch(
  [questionTab, () => filters.subject, () => filters.categoryId, () => filters.type, () => filters.difficulty],
  () => {
    pagination.page = 1
    selectedQuestionIds.value = []
    loadQuestions()
  },
)

watch(
  () => pagination.size,
  () => {
    pagination.page = 1
    loadQuestions()
  },
)

watch([questionKeyword, globalKeyword], () => {
  window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    pagination.page = 1
    loadQuestions()
  }, 350)
})

function selectNav(id) {
  activeNav.value = id
  profileOpen.value = false
  window.history.replaceState(null, '', `${window.location.pathname}${window.location.search}#${id}`)
  if (id === 'questions' && authenticated.value) {
    refreshQuestionData()
  } else if (id === 'categories' && authenticated.value) {
    loadClassificationOverview()
  } else if (id === 'practice' && authenticated.value) {
    loadPracticeDashboard()
  } else if (id === 'analytics' && authenticated.value) {
    loadAnalytics()
  } else if (id === 'settings' && authenticated.value) {
    Promise.all([loadQuestionBankSettings(), loadQuestionStats()])
  }
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function filterCategoryNodes(nodes, keyword) {
  if (!keyword) return nodes
  return nodes.map((node) => {
    const children = filterCategoryNodes(node.children || [], keyword)
    return { ...node, children }
  }).filter((node) => node.name.toLowerCase().includes(keyword) || node.children.length)
}

function flattenCategoryNodes(nodes, depth = 0, forceExpanded = false) {
  return nodes.flatMap((node) => [
    { ...node, depth },
    ...(forceExpanded || expandedCategoryIds.value.has(node.id)
      ? flattenCategoryNodes(node.children || [], depth + 1, forceExpanded)
      : []),
  ])
}

function isCategoryExpanded(categoryId) {
  return expandedCategoryIds.value.has(categoryId)
}

function toggleCategoryNode(node) {
  selectedCategory.value = node.id
  if (!node.children?.length) return
  const next = new Set(expandedCategoryIds.value)
  if (next.has(node.id)) next.delete(node.id)
  else next.add(node.id)
  expandedCategoryIds.value = next
}

function openCategoryQuestionsById(categoryId) {
  const category = categoryRows.value.find((item) => item.id === categoryId)
  if (category) openCategoryQuestions(category)
}

function openCategoryQuestions(category) {
  filters.categoryId = category.id
  filters.subject = ''
  questionKeyword.value = ''
  globalKeyword.value = ''
  questionTab.value = '全部题目'
  pagination.page = 1
  selectNav('questions')
  notify(`正在查看“${category.name}”分类下的题目`)
}

function collectDescendantCategoryIds(categoryId) {
  const result = new Set()
  let changed = true
  while (changed) {
    changed = false
    categoryRows.value.forEach((category) => {
      if ((category.parentId === categoryId || result.has(category.parentId)) && !result.has(category.id)) {
        result.add(category.id)
        changed = true
      }
    })
  }
  return result
}

function difficultyStars(level) {
  return `${'★'.repeat(level)}${'☆'.repeat(5 - level)}`
}

function toggleAllQuestions(event) {
  const ids = questionRows.value.map((item) => item.id)
  selectedQuestionIds.value = event.target.checked
    ? [...new Set([...selectedQuestionIds.value, ...ids])]
    : selectedQuestionIds.value.filter((id) => !ids.includes(id))
}

function clearQuestionFilters() {
  filters.subject = ''
  filters.categoryId = null
  filters.type = ''
  filters.difficulty = ''
  questionKeyword.value = ''
  globalKeyword.value = ''
  questionTab.value = '全部题目'
}

function ratioNote(value) {
  if (!questionStatsData.total) return '占总数 0%'
  return `占总数 ${((value / questionStatsData.total) * 100).toFixed(1)}%`
}

function questionTypeLabel(type) {
  return questionTypeOptions.find((item) => item.value === type)?.label ?? type
}

function questionStatusLabel(status) {
  return status === 'PUBLISHED' ? '已发布' : '草稿'
}

function formatDate(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
    .format(new Date(value))
}

function formatDateTime(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}

function formatShortDateTime(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}

function formatMonthDay(value) {
  if (!value) return '—'
  const [, month, day] = value.split('-')
  return `${month}-${day}`
}

function knowledgePublishRate(row) {
  if (!row.questionCount) return 0
  return Math.round((row.publishedCount / row.questionCount) * 100)
}

function categoryOptionLabel(category) {
  const names = [category.name]
  let parentId = category.parentId
  const visited = new Set([category.id])
  while (parentId && !visited.has(parentId)) {
    visited.add(parentId)
    const parent = categoryRows.value.find((item) => item.id === parentId)
    if (!parent) break
    names.unshift(parent.name)
    parentId = parent.parentId
  }
  return names.join(' / ')
}

function categoryTotalQuestionCount(categoryId) {
  const stack = [...categoryTree.value]
  while (stack.length) {
    const node = stack.shift()
    if (node.id === categoryId) return node.totalQuestionCount
    stack.push(...(node.children || []))
  }
  return categoryRows.value.find((category) => category.id === categoryId)?.questionCount || 0
}

function syncQuestionSubject() {
  const category = categoryRows.value.find((item) => item.id === questionForm.categoryId)
  if (category) questionForm.subject = category.name
}

function readApiError(error, fallback = '操作失败，请稍后重试') {
  if (!error.response) return '无法连接后端服务，请确认 8080 端口已启动'
  return error.response?.data?.message || fallback
}

async function initializeSession() {
  if (!authenticated.value) {
    authModalOpen.value = true
    return
  }
  try {
    const response = await getCurrentUserProfile()
    applyProfile(response.data?.data)
    await Promise.all([
      refreshQuestionData(),
      loadPracticeDashboard(),
      loadAnalytics(),
      loadQuestionBankSettings(),
    ])
  } catch (error) {
    handleApiFailure(error)
  }
}

function handleApiFailure(error, fallback) {
  if (error.response?.status === 401) {
    logoutUser()
    authenticated.value = false
    currentUsername.value = '访客'
    currentNickname.value = ''
    userAvatarUrl.value = ''
    questionRows.value = []
    categoryRows.value = []
    categoryTree.value = []
    knowledgePointRows.value = []
    tagRows.value = []
    difficultyRows.value = []
    expandedCategoryIds.value = new Set()
    classificationTreeInitialized.value = false
    filters.categoryId = null
    authModalOpen.value = true
    authError.value = '登录状态已失效，请重新登录'
    return
  }
  notify(readApiError(error, fallback))
}

function handleUserButton() {
  if (!authenticated.value) {
    authModalOpen.value = true
    return
  }
  profileOpen.value = !profileOpen.value
}

function closeAuthModal() {
  authModalOpen.value = false
  authError.value = ''
}

async function handleAuthSubmit() {
  authSubmitting.value = true
  authError.value = ''
  try {
    if (authMode.value === 'register') {
      await registerUser({
        username: authForm.username,
        password: authForm.password,
        email: authForm.email || null,
      })
    }
    const response = await loginUser({
      username: authForm.username,
      password: authForm.password,
    })
    currentUsername.value = response.data?.data?.user?.username || authForm.username
    currentNickname.value = currentUsername.value
    userAvatarUrl.value = ''
    authenticated.value = true
    authModalOpen.value = false
    authForm.password = ''
    notify(authMode.value === 'register' ? '注册并登录成功' : '登录成功')
    await initializeSession()
  } catch (error) {
    authError.value = readApiError(error, authMode.value === 'register' ? '注册失败' : '登录失败')
  } finally {
    authSubmitting.value = false
  }
}

function applyProfile(profile = {}) {
  currentUsername.value = profile.username || '学习者'
  currentNickname.value = profile.nickname || profile.username || '学习者'
  userAvatarUrl.value = profile.avatarUrl || profile.avatar || ''
}

function handleLogout() {
  logoutUser()
  authenticated.value = false
  currentUsername.value = '访客'
  currentNickname.value = ''
  userAvatarUrl.value = ''
  profileOpen.value = false
  questionRows.value = []
  practiceQuestions.value = []
  practiceModalOpen.value = false
  Object.assign(practiceDashboard, {
    streakDays: 0,
    recentAttempts: [],
    today: { practiceCount: 0, wrongReviewCount: 0, activeWrongCount: 0, estimatedMinutes: 0, goal: 10, progress: 0 },
    weakPoints: [],
  })
  selectedQuestionIds.value = []
  Object.assign(questionStatsData, { total: 0, published: 0, draft: 0, recycleBin: 0 })
  authModalOpen.value = true
  notify('已退出登录')
}

async function refreshQuestionData() {
  if (!authenticated.value) return
  await Promise.all([loadQuestions(), loadQuestionStats(), loadClassificationOverview()])
}

async function loadClassificationOverview() {
  if (!authenticated.value) return
  classificationLoading.value = true
  try {
    const response = await fetchClassificationOverview()
    const data = response.data?.data || {}
    categoryRows.value = data.categories || []
    categoryTree.value = data.categoryTree || []
    knowledgePointRows.value = data.knowledgePoints || []
    tagRows.value = data.tags || []
    difficultyRows.value = data.difficulties || []
    Object.assign(classificationSummary, data.summary || {
      categoryCount: 0,
      knowledgePointCount: 0,
      tagCount: 0,
      classifiedQuestionCount: 0,
      unclassifiedQuestionCount: 0,
    })
    const validIds = new Set(categoryRows.value.map((category) => category.id))
    expandedCategoryIds.value = new Set(
      [...expandedCategoryIds.value].filter((categoryId) => validIds.has(categoryId)),
    )
    if (!classificationTreeInitialized.value && categoryTree.value.length) {
      expandedCategoryIds.value = new Set(categoryTree.value.map((category) => category.id))
      classificationTreeInitialized.value = true
    }
    if (selectedCategory.value !== null
      && !categoryRows.value.some((category) => category.id === selectedCategory.value)) {
      selectedCategory.value = null
    }
  } catch (error) {
    handleApiFailure(error, '分类关系加载失败')
  } finally {
    classificationLoading.value = false
  }
}

async function loadQuestions() {
  if (!authenticated.value || activeNav.value !== 'questions') return
  questionsLoading.value = true
  try {
    const statusMap = { 已发布: 'PUBLISHED', 草稿: 'DRAFT' }
    const response = await fetchQuestions({
      page: pagination.page,
      size: pagination.size,
      keyword: questionKeyword.value || globalKeyword.value || undefined,
      subject: filters.subject || undefined,
      categoryId: filters.categoryId || undefined,
      questionType: filters.type || undefined,
      difficulty: filters.difficulty || undefined,
      status: statusMap[questionTab.value],
      deleted: questionTab.value === '回收站',
      sort: 'updatedAt',
      direction: 'desc',
    })
    const data = response.data?.data
    questionRows.value = data?.records || []
    pagination.total = data?.total || 0
    pagination.totalPages = Math.max(data?.totalPages || 1, 1)
    if (pagination.page > pagination.totalPages) {
      pagination.page = pagination.totalPages
      return loadQuestions()
    }
  } catch (error) {
    questionRows.value = []
    handleApiFailure(error, '题目加载失败')
  } finally {
    questionsLoading.value = false
  }
}

async function loadQuestionStats() {
  if (!authenticated.value) return
  try {
    const response = await fetchQuestionStats()
    Object.assign(questionStatsData, response.data?.data || {})
  } catch (error) {
    handleApiFailure(error, '题目统计加载失败')
  }
}

async function loadPracticeDashboard() {
  if (!authenticated.value) return
  try {
    const response = await fetchPracticeDashboard()
    Object.assign(practiceDashboard, response.data?.data || {})
  } catch (error) {
    handleApiFailure(error, '练习数据加载失败')
  }
}

async function loadAnalytics() {
  if (!authenticated.value) return
  try {
    const response = await fetchQuestionBankAnalytics()
    Object.assign(analyticsData, response.data?.data || {})
  } catch (error) {
    handleApiFailure(error, '统计数据加载失败')
  }
}

async function loadQuestionBankSettings() {
  if (!authenticated.value) return
  try {
    const response = await fetchQuestionBankSettings()
    Object.assign(settings, response.data?.data || {})
    practiceConfig.count = settings.practiceCount
  } catch (error) {
    handleApiFailure(error, '题库设置加载失败')
  }
}

async function startPractice(mode, knowledgePoint = null) {
  if (!requireAuthentication()) return
  activePracticeMode.value = mode
  practiceLoading.value = true
  practiceError.value = ''
  try {
    const response = await generatePractice({
      mode,
      count: practiceConfig.count,
      categoryId: practiceConfig.categoryId,
      knowledgePoint: knowledgePoint || null,
    })
    practiceQuestions.value = response.data?.data || []
    practiceIndex.value = 0
    practiceAnswer.value = ''
    practiceResult.value = null
    practiceModalOpen.value = true
  } catch (error) {
    notify(readApiError(error, '练习生成失败'))
  } finally {
    practiceLoading.value = false
  }
}

function practiceOptionValue(index) {
  if (currentPracticeQuestion.value?.questionType === 'TRUE_FALSE') {
    return currentPracticeQuestion.value.options[index]
  }
  return String.fromCharCode(65 + index)
}

function isPracticeOptionSelected(index) {
  return practiceAnswer.value.split(',').includes(practiceOptionValue(index))
}

function togglePracticeOption(index) {
  const value = practiceOptionValue(index)
  if (currentPracticeQuestion.value.questionType !== 'MULTIPLE_CHOICE') {
    practiceAnswer.value = value
    return
  }
  const selected = new Set(practiceAnswer.value ? practiceAnswer.value.split(',') : [])
  if (selected.has(value)) selected.delete(value)
  else selected.add(value)
  practiceAnswer.value = [...selected].sort().join(',')
}

async function submitCurrentAnswer() {
  if (!practiceAnswer.value || !currentPracticeQuestion.value) return
  answerSubmitting.value = true
  practiceError.value = ''
  try {
    const response = await submitPracticeAnswer({
      questionId: currentPracticeQuestion.value.id,
      userAnswer: practiceAnswer.value,
      mode: activePracticeMode.value,
    })
    practiceResult.value = response.data?.data
    await Promise.all([loadPracticeDashboard(), loadAnalytics()])
  } catch (error) {
    practiceError.value = readApiError(error, '提交答案失败')
  } finally {
    answerSubmitting.value = false
  }
}

function nextPracticeQuestion() {
  if (practiceIndex.value + 1 >= practiceQuestions.value.length) {
    practiceModalOpen.value = false
    notify('本次练习已完成')
    return
  }
  practiceIndex.value += 1
  practiceAnswer.value = ''
  practiceResult.value = null
  practiceError.value = ''
}

function closePractice() {
  if (answerSubmitting.value) return
  practiceModalOpen.value = false
}

async function openQuestionFromAttempt(questionId) {
  try {
    const response = await fetchQuestion(questionId)
    openViewEditor(response.data?.data)
  } catch (error) {
    handleApiFailure(error, '题目详情加载失败')
  }
}

async function saveQuestionBankSettings() {
  if (!requireAuthentication()) return
  settingsSaving.value = true
  try {
    const response = await updateQuestionBankSettings({
      bankName: settings.bankName,
      description: settings.description || null,
      memberEdit: settings.memberEdit,
      memberExport: settings.memberExport,
      reviewRequired: settings.reviewRequired,
      practiceCount: Number(settings.practiceCount),
      difficulty: settings.difficulty,
      sort: settings.sort,
      showAnswer: settings.showAnswer,
    })
    Object.assign(settings, response.data?.data || {})
    practiceConfig.count = settings.practiceCount
    notify('题库设置已保存')
    await loadPracticeDashboard()
  } catch (error) {
    handleApiFailure(error, '题库设置保存失败')
  } finally {
    settingsSaving.value = false
  }
}

function exportAnalytics() {
  const rows = [
    ['指标', '数值'],
    ['题目总数', analyticsData.summary.totalQuestions],
    ['已练题目', analyticsData.summary.answeredQuestions],
    ['正确率', `${analyticsData.summary.accuracy}%`],
    ['待复习错题', analyticsData.summary.activeWrongCount],
    ['预计学习分钟', analyticsData.summary.estimatedMinutes],
    [],
    ['日期', '答题数', '正确率'],
    ...analyticsData.trend.map((item) => [item.date, item.attempts, `${item.accuracy}%`]),
  ]
  const csv = `\uFEFF${rows.map((row) => row.map((cell) => `"${cell ?? ''}"`).join(',')).join('\n')}`
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `题库学习统计-${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
  notify('统计报表已导出')
}

function goToPage(page) {
  if (page < 1 || page > pagination.totalPages || page === pagination.page) return
  pagination.page = page
  selectedQuestionIds.value = []
  loadQuestions()
}

function resetQuestionForm(question = null) {
  Object.assign(questionForm, {
    content: question?.content || '',
    questionType: question?.questionType || 'SINGLE_CHOICE',
    options: question?.options?.length ? [...question.options] : ['', ''],
    correctAnswer: question?.correctAnswer || '',
    analysis: question?.analysis || '',
    difficulty: question?.difficulty || 3,
    subject: question?.subject || '',
    knowledgePoint: question?.knowledgePoint || '',
    status: question?.status || 'DRAFT',
    categoryId: question?.categoryId ?? null,
  })
}

function requireAuthentication() {
  if (authenticated.value) return true
  authModalOpen.value = true
  authError.value = '请先登录后再管理题目'
  return false
}

function resetClassificationForm() {
  Object.assign(classificationForm, {
    name: '',
    description: '',
    parentId: null,
    active: true,
    sortOrder: 0,
    oldName: '',
    subject: '',
  })
  editingClassificationId.value = null
  classificationFormError.value = ''
}

function openCategoryEditor(category = null, parentId = null) {
  if (!requireAuthentication()) return
  resetClassificationForm()
  classificationModalKind.value = 'category'
  if (category) {
    editingClassificationId.value = category.id
    Object.assign(classificationForm, {
      name: category.name,
      description: category.description || '',
      parentId: category.parentId,
      active: category.active,
      sortOrder: category.sortOrder,
    })
  } else {
    classificationForm.parentId = parentId
  }
  classificationModalOpen.value = true
}

function openTagEditor(tag = null) {
  if (!requireAuthentication()) return
  resetClassificationForm()
  classificationModalKind.value = 'tag'
  if (tag) {
    editingClassificationId.value = tag.id
    classificationForm.name = tag.name
    classificationForm.description = tag.description || ''
  }
  classificationModalOpen.value = true
}

function openKnowledgeEditor(knowledgePoint) {
  if (!requireAuthentication()) return
  resetClassificationForm()
  classificationModalKind.value = 'knowledge'
  Object.assign(classificationForm, {
    name: knowledgePoint.name,
    oldName: knowledgePoint.name,
    subject: knowledgePoint.subject || '',
  })
  classificationModalOpen.value = true
}

function closeClassificationModal() {
  if (classificationSaving.value) return
  classificationModalOpen.value = false
  classificationFormError.value = ''
}

async function submitClassificationForm() {
  classificationSaving.value = true
  classificationFormError.value = ''
  try {
    if (classificationModalKind.value === 'category') {
      const payload = {
        name: classificationForm.name,
        description: classificationForm.description || null,
        parentId: classificationForm.parentId,
        active: classificationForm.active,
        sortOrder: classificationForm.sortOrder,
      }
      if (editingClassificationId.value) {
        await updateClassificationCategory(editingClassificationId.value, payload)
        notify('分类修改成功')
      } else {
        await createClassificationCategory(payload)
        notify('分类创建成功')
      }
    } else if (classificationModalKind.value === 'tag') {
      const payload = {
        name: classificationForm.name,
        description: classificationForm.description || null,
      }
      if (editingClassificationId.value) {
        await updateClassificationTag(editingClassificationId.value, payload)
        notify('标签修改成功')
      } else {
        await createClassificationTag(payload)
        notify('标签创建成功')
      }
    } else {
      await renameKnowledgePoint({
        oldName: classificationForm.oldName,
        newName: classificationForm.name,
        subject: classificationForm.subject || null,
      })
      notify('知识点已同步重命名')
    }
    classificationModalOpen.value = false
    await loadClassificationOverview()
  } catch (error) {
    if (error.response?.status === 401) {
      classificationModalOpen.value = false
      handleApiFailure(error)
    } else {
      classificationFormError.value = readApiError(error, '保存失败')
    }
  } finally {
    classificationSaving.value = false
  }
}

async function handleDeleteCategory(category) {
  if (!window.confirm(`确定删除分类“${category.name}”吗？有关联题目或子分类时系统会阻止删除。`)) return
  try {
    await deleteClassificationCategory(category.id)
    notify('分类删除成功')
    await loadClassificationOverview()
  } catch (error) {
    handleApiFailure(error, '分类删除失败')
  }
}

async function handleDeleteTag(tag) {
  if (!window.confirm(`确定删除标签“${tag.name}”吗？`)) return
  try {
    await deleteClassificationTag(tag.id)
    notify('标签删除成功')
    await loadClassificationOverview()
  } catch (error) {
    handleApiFailure(error, '标签删除失败')
  }
}

async function handleClearKnowledgePoint(knowledgePoint) {
  if (!window.confirm(`确定从 ${knowledgePoint.questionCount} 道题中移除知识点“${knowledgePoint.name}”吗？题目本身不会被删除。`)) return
  try {
    await clearKnowledgePoint(knowledgePoint.name, knowledgePoint.subject)
    notify('知识点已从关联题目中移除')
    await loadClassificationOverview()
  } catch (error) {
    handleApiFailure(error, '知识点移除失败')
  }
}

function openCreateEditor() {
  if (!requireAuthentication()) return
  editingQuestionId.value = null
  editorMode.value = 'create'
  editorError.value = ''
  resetQuestionForm()
  editorOpen.value = true
}

function openEditEditor(question) {
  editingQuestionId.value = question.id
  editorMode.value = 'edit'
  editorError.value = ''
  resetQuestionForm(question)
  editorOpen.value = true
}

function openViewEditor(question) {
  editingQuestionId.value = question.id
  editorMode.value = 'view'
  editorError.value = ''
  resetQuestionForm(question)
  editorOpen.value = true
}

function openDuplicateEditor(question) {
  editingQuestionId.value = null
  editorMode.value = 'create'
  editorError.value = ''
  resetQuestionForm({ ...question, content: `${question.content}（副本）`, status: 'DRAFT' })
  editorOpen.value = true
}

function closeEditor() {
  if (questionSaving.value) return
  editorOpen.value = false
  editorError.value = ''
}

function addOption() {
  if (questionForm.options.length < 8) questionForm.options.push('')
}

function removeOption(index) {
  if (questionForm.options.length > 2) questionForm.options.splice(index, 1)
}

async function submitQuestion() {
  if (isChoiceQuestion.value && questionForm.options.filter(Boolean).length < 2) {
    editorError.value = '选择题至少需要两个有效选项'
    return
  }
  questionSaving.value = true
  editorError.value = ''
  const payload = {
    content: questionForm.content,
    questionType: questionForm.questionType,
    options: isChoiceQuestion.value ? questionForm.options.filter(Boolean) : [],
    correctAnswer: questionForm.correctAnswer,
    analysis: questionForm.analysis || null,
    difficulty: questionForm.difficulty,
    subject: questionForm.subject || null,
    knowledgePoint: questionForm.knowledgePoint || null,
    status: questionForm.status,
    categoryId: questionForm.categoryId,
  }
  try {
    if (editingQuestionId.value) {
      await updateQuestion(editingQuestionId.value, payload)
      notify('题目修改成功')
    } else {
      await createQuestion(payload)
      notify('题目新增成功')
    }
    editorOpen.value = false
    await refreshQuestionData()
  } catch (error) {
    if (error.response?.status === 401) {
      editorOpen.value = false
      handleApiFailure(error)
    } else {
      editorError.value = readApiError(error, '题目保存失败')
    }
  } finally {
    questionSaving.value = false
  }
}

async function handleDeleteQuestion(question) {
  if (!window.confirm(`确定将“${question.content}”移入回收站吗？`)) return
  try {
    await moveQuestionToRecycleBin(question.id)
    notify('题目已移入回收站')
    await refreshQuestionData()
  } catch (error) {
    handleApiFailure(error, '删除失败')
  }
}

async function handleRestoreQuestion(question) {
  try {
    await restoreQuestion(question.id)
    notify('题目已恢复')
    await refreshQuestionData()
  } catch (error) {
    handleApiFailure(error, '恢复失败')
  }
}

async function handlePermanentDelete(question) {
  if (!window.confirm(`永久删除“${question.content}”？此操作无法撤销。`)) return
  try {
    await permanentlyDeleteQuestion(question.id)
    notify('题目已永久删除')
    await refreshQuestionData()
  } catch (error) {
    handleApiFailure(error, '永久删除失败')
  }
}

async function handleBatchDelete() {
  if (!window.confirm(`确定将选中的 ${selectedQuestionIds.value.length} 道题移入回收站吗？`)) return
  try {
    await batchMoveQuestionsToRecycleBin(selectedQuestionIds.value)
    selectedQuestionIds.value = []
    notify('批量删除成功')
    await refreshQuestionData()
  } catch (error) {
    handleApiFailure(error, '批量删除失败')
  }
}

async function handleBatchStatus(status) {
  try {
    await batchUpdateQuestionStatus(selectedQuestionIds.value, status)
    selectedQuestionIds.value = []
    notify(status === 'PUBLISHED' ? '题目已批量发布' : '题目已转为草稿')
    await refreshQuestionData()
  } catch (error) {
    handleApiFailure(error, '批量修改状态失败')
  }
}

async function handleBatchRestore() {
  try {
    await Promise.all(selectedQuestionIds.value.map((id) => restoreQuestion(id)))
    selectedQuestionIds.value = []
    notify('题目已批量恢复')
    await refreshQuestionData()
  } catch (error) {
    handleApiFailure(error, '批量恢复失败')
  }
}

function triggerImport() {
  if (!requireAuthentication()) return
  importInput.value?.click()
}

async function handleImportFile(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  try {
    const raw = JSON.parse(await file.text())
    if (!Array.isArray(raw) || !raw.length) throw new Error('EMPTY')
    if (raw.length > 100) throw new Error('TOO_MANY')
    const results = await Promise.allSettled(raw.map((item) => createQuestion(normalizeImportedQuestion(item))))
    const successCount = results.filter((result) => result.status === 'fulfilled').length
    const failedCount = results.length - successCount
    notify(`导入完成：成功 ${successCount} 题${failedCount ? `，失败 ${failedCount} 题` : ''}`)
    await refreshQuestionData()
  } catch (error) {
    if (error.message === 'TOO_MANY') {
      notify('单次最多导入100道题目')
    } else if (error.response?.status === 401) {
      handleApiFailure(error)
    } else {
      notify('导入失败：请选择题目对象数组格式的 JSON 文件')
    }
  }
}

function normalizeImportedQuestion(item) {
  const typeMap = {
    选择题: 'SINGLE_CHOICE',
    单选题: 'SINGLE_CHOICE',
    多选题: 'MULTIPLE_CHOICE',
    判断题: 'TRUE_FALSE',
    填空题: 'FILL_BLANK',
    简答题: 'SHORT_ANSWER',
  }
  const statusMap = { 已发布: 'PUBLISHED', 草稿: 'DRAFT' }
  return {
    content: item.content,
    questionType: typeMap[item.questionType || item.type] || item.questionType || item.type,
    options: item.options || [],
    correctAnswer: item.correctAnswer || item.answer,
    analysis: item.analysis || null,
    difficulty: Number(item.difficulty || 3),
    subject: item.subject || null,
    knowledgePoint: item.knowledgePoint || item.knowledge || null,
    status: statusMap[item.status] || item.status || 'DRAFT',
    categoryId: item.categoryId || null,
  }
}

function notify(message) {
  toastMessage.value = message
  window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => { toastMessage.value = '' }, 2200)
}
</script>
