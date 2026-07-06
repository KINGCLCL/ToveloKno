export const navigationItems = [
  { id: 'questions', label: '题目管理', icon: 'list' },
  { id: 'categories', label: '分类关系', icon: 'layers' },
  { id: 'practice', label: '练习与复习', icon: 'shuffle' },
  { id: 'analytics', label: '数据统计', icon: 'pie-chart' },
  { id: 'settings', label: '题库设置', icon: 'settings' },
]

export const questionStats = [
  { label: '题目总数', value: '1,286', unit: '题', note: '较上月 +42', icon: 'book-open', tone: 'blue' },
  { label: '已发布', value: '856', unit: '题', note: '占总数 66.6%', icon: 'check-square', tone: 'green' },
  { label: '草稿数', value: '286', unit: '题', note: '占总数 22.2%', icon: 'edit', tone: 'orange' },
  { label: '回收站', value: '144', unit: '题', note: '本周新增 11', icon: 'trash', tone: 'purple' },
]

export const questions = [
  { id: 1, content: '下列关于数据库事务的说法正确的是？', subject: '数据库', knowledge: '事务管理', type: '选择题', difficulty: 4, status: '已发布', updatedAt: '2026-07-03' },
  { id: 2, content: 'SQL 中，以下哪个语句用于删除表中的数据？', subject: '数据库', knowledge: 'SQL 基础', type: '选择题', difficulty: 3, status: '已发布', updatedAt: '2026-07-02' },
  { id: 3, content: '请说明进程和线程的主要区别。', subject: '操作系统', knowledge: '进程与线程', type: '简答题', difficulty: 3, status: '草稿', updatedAt: '2026-07-02' },
  { id: 4, content: '以下哪个排序算法的平均时间复杂度为 O(n log n)？', subject: '数据结构', knowledge: '排序算法', type: '选择题', difficulty: 4, status: '已发布', updatedAt: '2026-07-01' },
  { id: 5, content: '二次函数的顶点坐标公式是 ______。', subject: '高等数学', knowledge: '函数基础', type: '填空题', difficulty: 2, status: '已发布', updatedAt: '2026-06-30' },
  { id: 6, content: '简述 TCP 三次握手的过程与作用。', subject: '计算机网络', knowledge: '传输层', type: '简答题', difficulty: 4, status: '草稿', updatedAt: '2026-06-29' },
]

export const categoryTree = [
  {
    id: 'computer',
    name: '计算机类',
    count: 842,
    children: [
      { id: 'database', name: '数据库', count: 286 },
      { id: 'os', name: '操作系统', count: 214 },
      { id: 'network', name: '计算机网络', count: 186 },
      { id: 'structure', name: '数据结构', count: 156 },
    ],
  },
  {
    id: 'language',
    name: '编程语言',
    count: 326,
    children: [
      { id: 'python', name: 'Python', count: 138 },
      { id: 'java', name: 'Java', count: 112 },
      { id: 'c', name: 'C 语言', count: 76 },
    ],
  },
  {
    id: 'math',
    name: '数学基础',
    count: 118,
    children: [{ id: 'calculus', name: '高等数学', count: 118 }],
  },
]

export const categoryRows = [
  { id: 1, name: '计算机基础', type: '科目', parent: '—', count: 456, status: '启用', updatedAt: '2026-07-03 10:30' },
  { id: 2, name: '操作系统', type: '知识点', parent: '计算机基础', count: 128, status: '启用', updatedAt: '2026-07-02 18:10' },
  { id: 3, name: '数据库', type: '知识点', parent: '计算机基础', count: 156, status: '启用', updatedAt: '2026-07-02 10:20' },
  { id: 4, name: '计算机网络', type: '知识点', parent: '计算机基础', count: 172, status: '启用', updatedAt: '2026-07-01 10:22' },
  { id: 5, name: 'Python', type: '知识点', parent: '编程语言', count: 98, status: '启用', updatedAt: '2026-07-01 11:00' },
  { id: 6, name: 'Java', type: '知识点', parent: '编程语言', count: 112, status: '启用', updatedAt: '2026-06-30 11:05' },
  { id: 7, name: 'C 语言', type: '知识点', parent: '编程语言', count: 76, status: '停用', updatedAt: '2026-06-30 11:08' },
]

export const practiceModes = [
  { id: 'free', label: '自由练习', description: '按科目、知识点自由选题', icon: 'target', accent: 'blue' },
  { id: 'chapter', label: '章节练习', description: '按照章节逐步巩固', icon: 'book-open', accent: 'violet' },
  { id: 'random', label: '随机练习', description: '随机组题，全面练习', icon: 'shuffle', accent: 'cyan' },
  { id: 'wrong', label: '错题重练', description: '针对错题，巩固薄弱点', icon: 'x-circle', accent: 'red' },
  { id: 'challenge', label: '高难挑战', description: '集中练习四星、五星题', icon: 'star', accent: 'gold' },
]

export const recentPractice = [
  { title: '章节练习 · 数据库系统', meta: '20/20 题 · 正确率 85%', date: '07-03 14:30', icon: 'clipboard' },
  { title: '随机练习 · 操作系统', meta: '16/20 题 · 正确率 72%', date: '07-02 20:15', icon: 'shuffle' },
  { title: '错题重练 · SQL 基础', meta: '15/15 题 · 正确率 93%', date: '07-01 19:40', icon: 'x-circle' },
]

export const weakPoints = [
  { name: '数据库索引', accuracy: 45 },
  { name: '事务管理', accuracy: 52 },
  { name: 'SQL 查询', accuracy: 68 },
  { name: '进程同步', accuracy: 40 },
  { name: '网络协议', accuracy: 55 },
]

export const analyticsStats = [
  { label: '题目总数', value: '1,286', unit: '题', note: '+42 本月', icon: 'book-open' },
  { label: '已做题', value: '856', unit: '题', note: '覆盖率 66.6%', icon: 'check-square' },
  { label: '正确率', value: '78', unit: '%', note: '+3.2% 较上周', icon: 'target' },
  { label: '错题数', value: '186', unit: '题', note: '待巩固 48 题', icon: 'x-circle' },
  { label: '学习时长', value: '36.5', unit: '小时', note: '近 30 天', icon: 'clock' },
]

export const trendPoints = [42, 61, 48, 74, 58, 82, 69, 76]

export const difficultySegments = [
  { label: '选择题', value: 82, color: '#5d7fea' },
  { label: '填空题', value: 74, color: '#58c8be' },
  { label: '简答题', value: 68, color: '#f0aa59' },
  { label: '编程题', value: 65, color: '#e66d80' },
  { label: '判断题', value: 78, color: '#8c6bd9' },
]

export const rankRows = [
  { name: '事务管理', count: 24, accuracy: 56 },
  { name: 'SQL 查询', count: 21, accuracy: 62 },
  { name: '进程同步', count: 18, accuracy: 59 },
  { name: '网络协议', count: 16, accuracy: 60 },
  { name: '索引优化', count: 14, accuracy: 65 },
]

export const sharedBanks = [
  { name: '计算机二级题库', count: 952, creator: '张三', joined: true },
  { name: '数据结构精选', count: 786, creator: '李四', joined: false },
  { name: '操作系统题库', count: 654, creator: '王五', joined: false },
]

export const permissionRows = [
  { role: '林同学（我）', identity: '管理员', permission: '全部权限' },
  { role: '张三', identity: '编辑员', permission: '编辑、批量、导出' },
  { role: '李四', identity: '只读员', permission: '仅查看' },
]
