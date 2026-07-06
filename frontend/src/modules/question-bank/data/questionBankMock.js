export const navigationGroups = [
  {
    label: '',
    items: [{ id: 'overview', label: '概览首页', icon: 'home' }],
  },
  {
    label: '题目管理',
    items: [
      { id: 'question-list', label: '题目列表', icon: 'list' },
      { id: 'question-create', label: '新增题目', icon: 'plus-circle' },
      { id: 'question-import', label: '批量导入', icon: 'upload' },
      { id: 'recycle-bin', label: '回收站', icon: 'trash' },
    ],
  },
  {
    label: '分类管理',
    items: [
      { id: 'subject', label: '科目管理', icon: 'folder' },
      { id: 'knowledge', label: '知识点管理', icon: 'messages' },
      { id: 'tag', label: '标签管理', icon: 'tag' },
      { id: 'difficulty', label: '难度管理', icon: 'bar-chart' },
    ],
  },
  {
    label: '练习与复习',
    items: [
      { id: 'practice', label: '在线练习', icon: 'edit' },
      { id: 'wrong-book', label: '错题本', icon: 'notebook' },
      { id: 'favorites', label: '收藏夹', icon: 'star' },
      { id: 'paper', label: '组卷管理', icon: 'clipboard' },
    ],
  },
  {
    label: '数据统计',
    items: [
      { id: 'study-records', label: '学习记录', icon: 'clock' },
      { id: 'analytics', label: '数据分析', icon: 'pie-chart' },
      { id: 'weaknesses', label: '薄弱知识点', icon: 'alert' },
      { id: 'recommendations', label: '推荐题目', icon: 'thumbs-up' },
    ],
  },
  {
    label: '题库设置',
    items: [
      { id: 'my-bank', label: '我的题库', icon: 'database' },
      { id: 'shared-bank', label: '共享题库', icon: 'users' },
      { id: 'permissions', label: '权限管理', icon: 'shield' },
    ],
  },
]

export const overviewStats = [
  { label: '题目总数', value: '1286', unit: '道', icon: 'book-open' },
  { label: '已做题目', value: '856', unit: '道', note: '占总数 66.6%', icon: 'check-square' },
  { label: '错题数', value: '128', unit: '道', note: '占已做 15.0%', icon: 'x-square' },
  { label: '收藏题', value: '72', unit: '道', icon: 'star' },
  { label: '组卷数', value: '15', unit: '套', icon: 'clipboard' },
  { label: '今日练习', value: '23', unit: '道', note: '正确率 78%', icon: 'trending-up' },
]

export const quickActions = [
  { id: 'add', label: '新增题目', icon: 'plus-circle' },
  { id: 'practice', label: '在线练习', icon: 'edit' },
  { id: 'wrong', label: '错题本', icon: 'x-circle' },
  { id: 'favorite', label: '收藏夹', icon: 'star' },
  { id: 'paper', label: '组卷练习', icon: 'file-text' },
  { id: 'import', label: '批量导入', icon: 'upload' },
]

export const questions = [
  {
    id: 1,
    content: '下列关于数据库事务的说法正确的是？',
    subject: '数据库',
    knowledge: '事务管理',
    type: '选择题',
    difficulty: 3,
    status: '已发布',
  },
  {
    id: 2,
    content: 'SQL中，以下哪个语句用于删除表中的数据？',
    subject: '数据库',
    knowledge: 'SQL基础',
    type: '选择题',
    difficulty: 2,
    status: '已发布',
  },
  {
    id: 3,
    content: '在操作系统中，进程和线程的区别是？',
    subject: '操作系统',
    knowledge: '进程与线程',
    type: '简答题',
    difficulty: 3,
    status: '草稿',
  },
  {
    id: 4,
    content: '以下哪个排序算法的时间复杂度为 O(n log n)？',
    subject: '数据结构',
    knowledge: '排序算法',
    type: '选择题',
    difficulty: 4,
    status: '已发布',
  },
  {
    id: 5,
    content: '二次函数的顶点坐标公式是？',
    subject: '数学',
    knowledge: '二次函数',
    type: '填空题',
    difficulty: 2,
    status: '已发布',
  },
]

export const practiceModes = [
  { id: 'free', label: '自由练习', description: '自选题目，随时练习', icon: 'edit' },
  { id: 'random', label: '随机练习', description: '系统随机抽题练习', icon: 'shuffle' },
  { id: 'chapter', label: '章节练习', description: '按知识点/章节练习', icon: 'book-open' },
  { id: 'wrong', label: '错题重练', description: '重点突破错题', icon: 'x-circle' },
  { id: 'favorite', label: '收藏练习', description: '练习收藏的题目', icon: 'star' },
  { id: 'mock', label: '模拟考试', description: '限时模考，检验水平', icon: 'clock' },
]

export const recentRecords = [
  { title: '数据库系统概论', type: '章节练习', progress: '20/20 题', accuracy: '85%', date: '2026-07-01' },
  { title: '操作系统', type: '错题重练', progress: '15/15 题', accuracy: '73%', date: '2026-06-30' },
  { title: '数据结构', type: '模拟考试', progress: '40/40 题', accuracy: '78%', date: '2026-06-29' },
  { title: 'SQL基础', type: '随机练习', progress: '25/25 题', accuracy: '92%', date: '2026-06-28' },
]

export const weakPoints = [
  { name: 'SQL多表查询', accuracy: 52 },
  { name: '事务管理', accuracy: 60 },
  { name: '进程调度', accuracy: 65 },
  { name: '排序算法', accuracy: 68 },
  { name: '范式理论', accuracy: 70 },
]
