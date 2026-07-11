import fs from 'node:fs/promises';
import path from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const JSZip = require('../frontend/node_modules/jszip');

const root = process.cwd();
const outDir = path.join(root, 'output', 'project-report');
const templatePath = path.join(outDir, 'ToveloKno-项目开发总结报告2026-工作稿.docx');
const finalPath = path.join(outDir, 'ToveloKno-项目开发总结报告2026-已填写.docx');
const loginShot = path.join(outDir, 'login-screenshot.png');
const homeShot = path.join(outDir, 'home-screenshot.png');
const imgDir = path.join(outDir, 'generated-diagrams');

const W_NS = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main';
const R_NS = 'http://schemas.openxmlformats.org/officeDocument/2006/relationships';
const WP_NS = 'http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing';
const A_NS = 'http://schemas.openxmlformats.org/drawingml/2006/main';
const PIC_NS = 'http://schemas.openxmlformats.org/drawingml/2006/picture';
const REL_NS = 'http://schemas.openxmlformats.org/package/2006/relationships';

const page = {
  portraitWidth: 11906,
  portraitHeight: 16838,
  landscapeWidth: 16838,
  landscapeHeight: 11906,
  margin: 1440,
};

const usablePortrait = page.portraitWidth - page.margin * 2;
const usableLandscape = page.landscapeWidth - 1080 * 2;

function esc(value = '') {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function attr(value = '') {
  return esc(value).replace(/\n/g, ' ');
}

function textRuns(text, { bold = false, size = 21, color = '000000', font = '宋体' } = {}) {
  const normalized = String(text ?? '').split('\n');
  return normalized.map((part, index) => {
    const br = index ? '<w:br/>' : '';
    return `${br}<w:r><w:rPr>${bold ? '<w:b/>' : ''}<w:color w:val="${color}"/><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:eastAsia="${font}"/><w:sz w:val="${size}"/><w:szCs w:val="${size}"/></w:rPr><w:t xml:space="preserve">${esc(part)}</w:t></w:r>`;
  }).join('');
}

function paragraph(text = '', opts = {}) {
  const {
    style,
    align = 'left',
    size = 21,
    bold = false,
    color = '000000',
    before = 0,
    after = 80,
    line = 300,
    indentFirstLine = 0,
    keepNext = false,
  } = opts;
  const pPr = [
    style ? `<w:pStyle w:val="${style}"/>` : '',
    keepNext ? '<w:keepNext/>' : '',
    `<w:jc w:val="${align}"/>`,
    `<w:spacing w:before="${before}" w:after="${after}" w:line="${line}" w:lineRule="auto"/>`,
    indentFirstLine ? `<w:ind w:firstLine="${indentFirstLine}"/>` : '',
  ].join('');
  return `<w:p><w:pPr>${pPr}</w:pPr>${textRuns(text, { bold, size, color })}</w:p>`;
}

function heading(text, level = 1) {
  const sizes = { 1: 28, 2: 24, 3: 22 };
  return paragraph(text, {
    bold: true,
    size: sizes[level] || 22,
    before: level === 1 ? 260 : 180,
    after: 120,
    line: 360,
    keepNext: true,
  });
}

function caption(text) {
  return paragraph(text, { align: 'center', size: 18, after: 180, line: 260 });
}

function bullet(items) {
  return items.map((item) => paragraph(`• ${item}`, { size: 21, after: 45, line: 300, indentFirstLine: 420 })).join('');
}

function pageBreak() {
  return '<w:p><w:r><w:br w:type="page"/></w:r></w:p>';
}

function sectPrPortrait() {
  return `<w:sectPr><w:pgSz w:w="${page.portraitWidth}" w:h="${page.portraitHeight}"/><w:pgMar w:top="1440" w:right="1800" w:bottom="1440" w:left="1800" w:header="851" w:footer="992" w:gutter="0"/><w:cols w:space="425"/><w:docGrid w:type="lines" w:linePitch="312"/></w:sectPr>`;
}

function sectPrLandscape() {
  return `<w:sectPr><w:pgSz w:w="${page.landscapeWidth}" w:h="${page.landscapeHeight}" w:orient="landscape"/><w:pgMar w:top="1440" w:right="1080" w:bottom="1440" w:left="1080" w:header="851" w:footer="992" w:gutter="0"/><w:cols w:space="425"/><w:docGrid w:type="lines" w:linePitch="312"/></w:sectPr>`;
}

function sectionBreakLandscape() {
  return `<w:p><w:pPr>${sectPrPortrait()}</w:pPr></w:p>`;
}

function sectionBreakPortrait() {
  return `<w:p><w:pPr>${sectPrLandscape()}</w:pPr></w:p>`;
}

function cell(text = '', opts = {}) {
  const {
    width = 1000,
    shade,
    bold = false,
    size = 20,
    align = 'left',
    vAlign = 'center',
  } = opts;
  const fill = shade ? `<w:shd w:fill="${shade}"/>` : '';
  return `<w:tc><w:tcPr><w:tcW w:w="${width}" w:type="dxa"/>${fill}<w:vAlign w:val="${vAlign}"/><w:tcMar><w:top w:w="90" w:type="dxa"/><w:left w:w="110" w:type="dxa"/><w:bottom w:w="90" w:type="dxa"/><w:right w:w="110" w:type="dxa"/></w:tcMar></w:tcPr>${paragraph(text, { bold, size, align, after: 0, line: 260 })}</w:tc>`;
}

function table(rows, widths, opts = {}) {
  const { headerRows = 1, fontSize = 19, tableWidth = widths.reduce((a, b) => a + b, 0), shade = 'EAF1FF' } = opts;
  const grid = widths.map((w) => `<w:gridCol w:w="${w}"/>`).join('');
  const body = rows.map((row, rIndex) => {
    const isHeader = rIndex < headerRows;
    const trPr = isHeader ? '<w:trPr><w:tblHeader/></w:trPr>' : '<w:trPr/>';
    return `<w:tr>${trPr}${row.map((value, cIndex) => cell(value, {
      width: widths[cIndex] || widths[widths.length - 1],
      shade: isHeader ? shade : undefined,
      bold: isHeader,
      size: fontSize,
      align: cIndex === 0 && !isHeader ? 'center' : 'left',
    })).join('')}</w:tr>`;
  }).join('');
  return `<w:tbl><w:tblPr><w:tblW w:w="${tableWidth}" w:type="dxa"/><w:tblInd w:w="0" w:type="dxa"/><w:tblBorders><w:top w:val="single" w:sz="4" w:color="9CB4D8"/><w:left w:val="single" w:sz="4" w:color="9CB4D8"/><w:bottom w:val="single" w:sz="4" w:color="9CB4D8"/><w:right w:val="single" w:sz="4" w:color="9CB4D8"/><w:insideH w:val="single" w:sz="4" w:color="D5E0F2"/><w:insideV w:val="single" w:sz="4" w:color="D5E0F2"/></w:tblBorders><w:tblCellMar><w:top w:w="90" w:type="dxa"/><w:left w:w="110" w:type="dxa"/><w:bottom w:w="90" w:type="dxa"/><w:right w:w="110" w:type="dxa"/></w:tblCellMar></w:tblPr><w:tblGrid>${grid}</w:tblGrid>${body}</w:tbl>`;
}

function drawing(rId, { name, widthDxa, heightDxa }) {
  const cx = Math.round(widthDxa * 635);
  const cy = Math.round(heightDxa * 635);
  const id = Math.floor(Math.random() * 100000) + 1000;
  return `<w:p><w:pPr><w:jc w:val="center"/><w:spacing w:before="80" w:after="80"/></w:pPr><w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="${cx}" cy="${cy}"/><wp:effectExtent l="0" t="0" r="0" b="0"/><wp:docPr id="${id}" name="${attr(name)}"/><wp:cNvGraphicFramePr><a:graphicFrameLocks xmlns:a="${A_NS}" noChangeAspect="1"/></wp:cNvGraphicFramePr><a:graphic xmlns:a="${A_NS}"><a:graphicData uri="${PIC_NS}"><pic:pic xmlns:pic="${PIC_NS}"><pic:nvPicPr><pic:cNvPr id="0" name="${attr(name)}"/><pic:cNvPicPr/></pic:nvPicPr><pic:blipFill><a:blip r:embed="${rId}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="${cx}" cy="${cy}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>`;
}

function sketchSvg({ title, width = 1200, height = 720, boxes = [], arrows = [], notes = [] }) {
  const bg = `<defs>
    <filter id="rough"><feTurbulence type="fractalNoise" baseFrequency="0.018" numOctaves="2" result="noise"/><feDisplacementMap in="SourceGraphic" in2="noise" scale="1.8"/></filter>
    <marker id="arrow" markerWidth="12" markerHeight="12" refX="9" refY="4" orient="auto"><path d="M0,0 L10,4 L0,8" fill="#202124"/></marker>
  </defs>
  <rect width="100%" height="100%" fill="#fbfcff"/>
  <path d="M20 25 C250 4, 420 28, 620 18 S980 8, ${width - 24} 28" fill="none" stroke="#dce7ff" stroke-width="4" opacity=".65"/>
  <path d="M10 ${height - 42} C250 ${height - 18}, 470 ${height - 55}, 760 ${height - 28} S1040 ${height - 18}, ${width - 30} ${height - 44}" fill="none" stroke="#ffe7ef" stroke-width="5" opacity=".7"/>`;
  const boxEls = boxes.map((b, i) => {
    const fill = b.fill || (i % 3 === 0 ? '#eef5ff' : i % 3 === 1 ? '#fff4fa' : '#eefbf8');
    return `<g filter="url(#rough)">
      <rect x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="${b.rx ?? 14}" fill="${fill}" stroke="#202124" stroke-width="${b.stroke ?? 3}"/>
      <text x="${b.x + b.w / 2}" y="${b.y + b.h / 2 - (b.sub ? 8 : 0)}" dominant-baseline="middle" text-anchor="middle" font-family="KaiTi, STKaiti, SimSun" font-size="${b.size || 28}" font-weight="${b.bold ? 700 : 500}" fill="#202124">${esc(b.text)}</text>
      ${b.sub ? `<text x="${b.x + b.w / 2}" y="${b.y + b.h / 2 + 24}" dominant-baseline="middle" text-anchor="middle" font-family="KaiTi, STKaiti, SimSun" font-size="${b.subSize || 18}" fill="#4b5568">${esc(b.sub)}</text>` : ''}
    </g>`;
  }).join('');
  const arrowEls = arrows.map((a) => `<path filter="url(#rough)" d="${a.d}" fill="none" stroke="#202124" stroke-width="${a.width || 3}" stroke-linecap="round" stroke-linejoin="round" marker-end="url(#arrow)"/>`).join('');
  const noteEls = notes.map((n) => `<text x="${n.x}" y="${n.y}" font-family="KaiTi, STKaiti, SimSun" font-size="${n.size || 24}" fill="${n.color || '#202124'}">${esc(n.text)}</text>`).join('');
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">
    ${bg}
    <text x="42" y="58" font-family="KaiTi, STKaiti, SimSun" font-size="34" font-weight="700" fill="#202124">${esc(title)}</text>
    ${boxEls}
    ${arrowEls}
    ${noteEls}
  </svg>`;
}

function writeSvg(name, svg) {
  const file = path.join(imgDir, `${name}.svg`);
  return fs.writeFile(file, svg, 'utf8').then(() => file);
}

async function makeDiagrams() {
  await fs.mkdir(imgDir, { recursive: true });
  const diagrams = {};
  diagrams.function = await writeSvg('function-architecture', sketchSvg({
    title: 'ToveloKno 功能架构草图',
    boxes: [
      { text: 'ToveloKno', sub: 'to love knowledge', x: 470, y: 72, w: 260, h: 76, fill: '#f7fbff', bold: true },
      { text: '用户与个人主页', x: 70, y: 220, w: 220, h: 76 },
      { text: '学习资料', x: 340, y: 220, w: 190, h: 76 },
      { text: '题库练习', x: 590, y: 220, w: 190, h: 76 },
      { text: '学习计划', x: 840, y: 220, w: 190, h: 76 },
      { text: '资源分享', x: 210, y: 420, w: 190, h: 76 },
      { text: '学习论坛', x: 485, y: 420, w: 190, h: 76 },
      { text: 'AI 辅助', x: 760, y: 420, w: 190, h: 76 },
      { text: '错题本/复盘', x: 485, y: 565, w: 220, h: 70, fill: '#fff8e8' },
    ],
    arrows: [
      { d: 'M600 150 C585 180, 250 185, 180 220' },
      { d: 'M610 150 C590 185, 445 190, 430 220' },
      { d: 'M640 150 C640 180, 685 190, 690 220' },
      { d: 'M680 150 C730 175, 900 185, 930 220' },
      { d: 'M430 296 C405 345, 320 375, 305 420' },
      { d: 'M690 296 C675 350, 620 385, 585 420' },
      { d: 'M930 296 C905 354, 860 382, 850 420' },
      { d: 'M685 296 C665 390, 615 495, 595 565' },
      { d: 'M570 496 C570 520, 580 540, 590 565' },
    ],
    notes: [
      { text: '资料可抽题、截图可生成知识卡片', x: 300, y: 340, size: 22 },
      { text: '错题自动进入复盘计划', x: 705, y: 545, size: 22 },
    ],
  }));

  diagrams.tech = await writeSvg('technical-architecture', sketchSvg({
    title: '技术架构草图',
    boxes: [
      { text: '浏览器 / Edge / Chrome', sub: 'Vue 3 + Vite 单页应用', x: 70, y: 105, w: 310, h: 90 },
      { text: '前端模块层', sub: 'App.vue + modules/* Workspace', x: 450, y: 105, w: 320, h: 90 },
      { text: 'Axios API 层', sub: 'Token 自动注入 / 统一错误处理', x: 840, y: 105, w: 300, h: 90 },
      { text: 'Spring Boot Controller', sub: 'RESTful /api/*', x: 110, y: 300, w: 300, h: 90 },
      { text: 'Service 业务层', sub: '判题、资料抽题、论坛互动、AI调用', x: 470, y: 300, w: 330, h: 90 },
      { text: 'Repository / JPA', sub: '实体映射与数据访问', x: 865, y: 300, w: 270, h: 90 },
      { text: 'MySQL 数据库', sub: '用户、资源、题库、错题、论坛', x: 225, y: 520, w: 300, h: 88 },
      { text: '本地上传目录', sub: '资料文件、封面、头像、首页图', x: 610, y: 520, w: 300, h: 88 },
      { text: 'OpenAI API', sub: 'AI辅助分析/抽题建议', x: 965, y: 520, w: 190, h: 88 },
    ],
    arrows: [
      { d: 'M380 150 C405 150, 420 150, 450 150' },
      { d: 'M770 150 C805 150, 815 150, 840 150' },
      { d: 'M990 195 C900 245, 325 245, 260 300' },
      { d: 'M410 345 C435 345, 445 345, 470 345' },
      { d: 'M800 345 C830 345, 840 345, 865 345' },
      { d: 'M950 390 C850 455, 450 460, 380 520' },
      { d: 'M650 390 C650 450, 715 470, 740 520' },
      { d: 'M690 390 C800 435, 1020 450, 1060 520' },
    ],
  }));

  diagrams.practiceFlow = await writeSvg('question-practice-flow', sketchSvg({
    title: '题库自动判题流程',
    width: 1120,
    height: 720,
    boxes: [
      { text: '选择练习模式', sub: '自由/错题/薄弱/挑战', x: 70, y: 110, w: 220, h: 80 },
      { text: '生成练习卷', sub: '按分类、知识点、难度筛选', x: 420, y: 110, w: 250, h: 80 },
      { text: '用户作答', sub: '选择题或文本答案', x: 800, y: 110, w: 220, h: 80 },
      { text: '提交后端判题', sub: 'normalize + answersEqual', x: 420, y: 300, w: 260, h: 86, fill: '#fff4fa' },
      { text: '记录答题结果', sub: 'answer_record', x: 95, y: 500, w: 240, h: 80 },
      { text: '错误进入错题本', sub: 'wrong_question', x: 450, y: 500, w: 240, h: 80 },
      { text: '刷新统计与复盘', sub: 'dashboard / analytics', x: 805, y: 500, w: 240, h: 80 },
    ],
    arrows: [
      { d: 'M290 150 C345 150, 370 150, 420 150' },
      { d: 'M670 150 C720 150, 755 150, 800 150' },
      { d: 'M910 190 C900 260, 720 285, 680 320' },
      { d: 'M420 345 C340 375, 245 450, 215 500' },
      { d: 'M550 386 C550 430, 565 460, 570 500' },
      { d: 'M680 345 C750 390, 880 430, 925 500' },
    ],
  }));

  diagrams.resourceFlow = await writeSvg('resource-card-flow', sketchSvg({
    title: '学习资料与知识卡片流程',
    width: 1120,
    height: 720,
    boxes: [
      { text: '上传资料', sub: 'PDF / 图片 / 视频 / 文档', x: 75, y: 120, w: 230, h: 86 },
      { text: '资料阅读器', sub: '进度、批注、页码、视频播放', x: 430, y: 120, w: 270, h: 86 },
      { text: '截图加入卡片', sub: '选区截图 + 摘要', x: 815, y: 120, w: 230, h: 86 },
      { text: 'AI/规则抽题', sub: '资料内容生成题库题目', x: 250, y: 355, w: 250, h: 86, fill: '#fff8e8' },
      { text: '知识卡片库', sub: '单独打开、收藏、复习', x: 630, y: 355, w: 250, h: 86, fill: '#eefbf8' },
      { text: '首页联动', sub: '统计栏与最近学习', x: 438, y: 560, w: 250, h: 76 },
    ],
    arrows: [
      { d: 'M305 163 C355 163, 385 163, 430 163' },
      { d: 'M700 163 C750 163, 775 163, 815 163' },
      { d: 'M530 206 C495 265, 420 315, 375 355' },
      { d: 'M925 206 C880 265, 820 315, 755 355' },
      { d: 'M375 441 C410 500, 500 520, 535 560' },
      { d: 'M755 441 C725 500, 625 525, 575 560' },
    ],
  }));

  diagrams.communityFlow = await writeSvg('community-flow', sketchSvg({
    title: '资源分享与论坛互动流程',
    width: 1120,
    height: 720,
    boxes: [
      { text: '用户上传资源', sub: '标题/分类/文件/链接/封面', x: 70, y: 120, w: 240, h: 84 },
      { text: '资源广场', sub: '分类筛选、播放、收藏、点赞', x: 430, y: 120, w: 260, h: 84 },
      { text: '收藏页面', sub: 'shared_resource_favorite', x: 820, y: 120, w: 240, h: 84 },
      { text: '发布主题', sub: 'NGA 风格板块与帖子', x: 110, y: 355, w: 250, h: 84, fill: '#fff4fa' },
      { text: '回复 / 浏览 / 点赞', sub: 'reply_count / like_count', x: 455, y: 355, w: 250, h: 84, fill: '#eefbf8' },
      { text: '个人学习资产', sub: '账号维度统一关联', x: 790, y: 355, w: 250, h: 84, fill: '#fff8e8' },
      { text: '首页与学习中枢', sub: '最近资料、错题、计划', x: 420, y: 560, w: 280, h: 78 },
    ],
    arrows: [
      { d: 'M310 162 C350 162, 385 162, 430 162' },
      { d: 'M690 162 C735 162, 775 162, 820 162' },
      { d: 'M240 204 C235 275, 245 310, 270 355' },
      { d: 'M360 397 C400 397, 420 397, 455 397' },
      { d: 'M705 397 C740 397, 760 397, 790 397' },
      { d: 'M915 439 C820 515, 690 520, 630 560' },
      { d: 'M580 439 C575 500, 570 520, 565 560' },
    ],
  }));

  diagrams.er = await writeSvg('er-diagram', sketchSvg({
    title: 'ToveloKno 核心 ER 图',
    width: 1400,
    height: 850,
    boxes: [
      { text: 'user', sub: 'id, username, profile...', x: 570, y: 80, w: 230, h: 78, fill: '#fff8e8' },
      { text: 'learning_resource', sub: 'user_id, file_url, progress', x: 80, y: 250, w: 260, h: 82 },
      { text: 'question', sub: 'created_by, category_id, answer', x: 400, y: 250, w: 250, h: 82 },
      { text: 'category / tag', sub: 'created_by, parent_id', x: 720, y: 250, w: 240, h: 82 },
      { text: 'study_plan', sub: 'user_id, target_type, status', x: 1050, y: 250, w: 250, h: 82 },
      { text: 'answer_record', sub: 'user_id, question_id, correct', x: 245, y: 470, w: 260, h: 82, fill: '#eefbf8' },
      { text: 'wrong_question', sub: 'user_id, question_id, wrong_count', x: 565, y: 470, w: 260, h: 82, fill: '#fff4fa' },
      { text: 'shared_resource', sub: 'owner_id, category, file_url', x: 890, y: 470, w: 260, h: 82 },
      { text: 'forum_thread', sub: 'user_id, board_id, counts', x: 250, y: 675, w: 260, h: 82 },
      { text: 'forum_reply', sub: 'thread_id, user_id, content', x: 565, y: 675, w: 260, h: 82 },
      { text: 'favorite / like', sub: 'resource_id / thread_id', x: 890, y: 675, w: 260, h: 82, fill: '#eef5ff' },
    ],
    arrows: [
      { d: 'M600 158 C470 210, 300 210, 220 250' },
      { d: 'M650 158 C595 205, 550 215, 525 250' },
      { d: 'M735 158 C770 195, 815 210, 840 250' },
      { d: 'M800 150 C950 185, 1110 205, 1175 250' },
      { d: 'M525 332 C470 385, 410 425, 380 470' },
      { d: 'M540 332 C605 385, 650 425, 695 470' },
      { d: 'M1045 552 C990 600, 955 635, 1020 675' },
      { d: 'M380 552 C365 610, 365 635, 380 675' },
      { d: 'M695 552 C695 610, 695 635, 695 675' },
      { d: 'M510 716 C535 716, 545 716, 565 716' },
      { d: 'M825 716 C850 716, 865 716, 890 716' },
      { d: 'M1020 552 C940 590, 815 605, 695 675' },
    ],
    notes: [
      { text: '所有学习资产都以 user_id / created_by 隔离到当前账号', x: 465, y: 810, size: 24, color: '#4b5568' },
    ],
  }));

  diagrams.source = await writeSvg('source-structure', sketchSvg({
    title: '源码结构草图',
    width: 1120,
    height: 720,
    boxes: [
      { text: 'frontend/src', sub: 'Vue 3 + Vite', x: 90, y: 110, w: 250, h: 80 },
      { text: 'App.vue', sub: '登录、首页、个人主页、模块入口', x: 430, y: 92, w: 290, h: 72 },
      { text: 'modules/*', sub: '资料/题库/计划/论坛/分享/AI', x: 430, y: 190, w: 290, h: 72 },
      { text: 'api.js', sub: 'Axios + Token + 统一接口', x: 800, y: 145, w: 250, h: 80 },
      { text: 'backend/src/main/java', sub: 'Spring Boot', x: 90, y: 405, w: 300, h: 80 },
      { text: 'controller', sub: 'REST API', x: 470, y: 345, w: 210, h: 70 },
      { text: 'service', sub: '业务逻辑', x: 470, y: 445, w: 210, h: 70 },
      { text: 'entity/repository', sub: 'JPA 实体与持久化', x: 760, y: 395, w: 270, h: 80 },
      { text: 'uploads / database', sub: '文件与 MySQL 数据', x: 430, y: 610, w: 310, h: 72, fill: '#fff8e8' },
    ],
    arrows: [
      { d: 'M340 150 C375 140, 395 130, 430 125' },
      { d: 'M340 150 C385 180, 405 205, 430 225' },
      { d: 'M720 130 C755 140, 770 150, 800 170' },
      { d: 'M930 225 C875 300, 720 330, 650 345' },
      { d: 'M390 445 C425 420, 445 395, 470 380' },
      { d: 'M390 445 C425 450, 445 465, 470 480' },
      { d: 'M680 480 C720 475, 735 450, 760 435' },
      { d: 'M605 515 C600 555, 585 580, 585 610' },
    ],
  }));
  return rasterizeDiagrams(diagrams);
}

async function rasterizeDiagrams(diagrams) {
  const edge = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';
  const { spawn } = await import('node:child_process');
  const result = {};
  for (const [key, svgPath] of Object.entries(diagrams)) {
    const pngPath = svgPath.replace(/\.svg$/i, '.png');
    const svgText = await fs.readFile(svgPath, 'utf8');
    const width = Number(svgText.match(/<svg[^>]*\swidth="(\d+)"/)?.[1] || 1200);
    const height = Number(svgText.match(/<svg[^>]*\sheight="(\d+)"/)?.[1] || 720);
    await new Promise((resolve, reject) => {
      const child = spawn(edge, [
        '--headless',
        '--disable-gpu',
        '--hide-scrollbars',
        `--window-size=${width},${height}`,
        `--screenshot=${pngPath}`,
        `file:///${svgPath.replace(/\\/g, '/')}`,
      ], { stdio: 'ignore' });
      child.on('error', reject);
      child.on('exit', (code) => code === 0 ? resolve() : reject(new Error(`Edge screenshot failed: ${code}`)));
    });
    result[key] = pngPath;
  }
  return result;
}

const databaseTables = [
  {
    cn: '用户信息表',
    en: 'user',
    rows: [
      ['id', 'bigint', '', '主键', '', '用户ID'],
      ['username', 'varchar', '80', '', '', '登录用户名'],
      ['password', 'varchar', '255', '', '', 'BCrypt加密密码'],
      ['email', 'varchar', '120', '', '', '邮箱'],
      ['nickname', 'varchar', '80', '', '', '昵称'],
      ['avatar', 'varchar', '500', '', '', '头像地址'],
      ['profile_background', 'varchar', '500', '', '', '个人主页背景'],
      ['status', 'int', '', '', '', '账号状态'],
      ['created_at', 'datetime', '', '', '', '创建时间'],
      ['updated_at', 'datetime', '', '', '', '更新时间'],
    ],
  },
  {
    cn: '学习资料表',
    en: 'learning_resource',
    rows: [
      ['id', 'bigint', '', '主键', '', '资料ID'],
      ['user_id', 'bigint', '', '外键', 'user', '所属用户'],
      ['name', 'varchar', '160', '', '', '资料名称'],
      ['type', 'varchar', '40', '', '', '资料类型'],
      ['file_url', 'varchar', '500', '', '', '文件访问地址'],
      ['file_size', 'bigint', '', '', '', '文件大小'],
      ['favorite', 'boolean', '', '', '', '是否收藏'],
      ['progress_percent', 'int', '', '', '', '学习进度'],
      ['annotations_json', 'text', '', '', '', '批注与截图卡片信息'],
      ['last_studied_at', 'datetime', '', '', '', '最近学习时间'],
    ],
  },
  {
    cn: '题库题目表',
    en: 'question',
    rows: [
      ['id', 'bigint', '', '主键', '', '题目ID'],
      ['content', 'text', '', '', '', '题干内容'],
      ['question_type', 'varchar', '40', '', '', '题型'],
      ['options_json', 'text', '', '', '', '选项JSON'],
      ['correct_answer', 'varchar', '1000', '', '', '标准答案'],
      ['analysis', 'text', '', '', '', '答案解析'],
      ['difficulty', 'int', '', '', '', '难度等级'],
      ['category_id', 'bigint', '', '外键', 'category', '所属分类'],
      ['source_resource_id', 'bigint', '', '外键', 'learning_resource', '来源资料'],
      ['created_by', 'bigint', '', '外键', 'user', '创建者'],
    ],
  },
  {
    cn: '分类表',
    en: 'category',
    rows: [
      ['id', 'bigint', '', '主键', '', '分类ID'],
      ['name', 'varchar', '80', '', '', '分类名称'],
      ['description', 'varchar', '255', '', '', '说明'],
      ['parent_id', 'bigint', '', '外键', 'category', '父分类'],
      ['active', 'boolean', '', '', '', '是否启用'],
      ['sort_order', 'int', '', '', '', '排序号'],
      ['created_by', 'bigint', '', '外键', 'user', '创建者'],
      ['created_at', 'datetime', '', '', '', '创建时间'],
      ['updated_at', 'datetime', '', '', '', '更新时间'],
    ],
  },
  {
    cn: '答题记录表',
    en: 'answer_record',
    rows: [
      ['id', 'bigint', '', '主键', '', '记录ID'],
      ['user_id', 'bigint', '', '外键', 'user', '答题用户'],
      ['question_id', 'bigint', '', '外键', 'question', '题目ID'],
      ['user_answer', 'varchar', '1000', '', '', '用户答案'],
      ['is_correct', 'boolean', '', '', '', '是否正确'],
      ['practice_mode', 'varchar', '30', '', '', '练习模式'],
      ['answered_at', 'datetime', '', '', '', '答题时间'],
    ],
  },
  {
    cn: '错题本表',
    en: 'wrong_question',
    rows: [
      ['id', 'bigint', '', '主键', '', '错题记录ID'],
      ['user_id', 'bigint', '', '外键', 'user', '所属用户'],
      ['question_id', 'bigint', '', '外键', 'question', '题目ID'],
      ['wrong_count', 'int', '', '', '', '错误次数'],
      ['mastered', 'boolean/int', '', '', '', '是否掌握'],
      ['last_wrong_at', 'datetime', '', '', '', '最近答错时间'],
      ['last_reviewed_at', 'datetime', '', '', '', '最近复习时间'],
    ],
  },
  {
    cn: '学习计划表',
    en: 'study_plan',
    rows: [
      ['id', 'bigint', '', '主键', '', '计划ID'],
      ['user_id', 'bigint', '', '外键', 'user', '所属用户'],
      ['title', 'varchar', '150', '', '', '计划标题'],
      ['content', 'text', '', '', '', '计划内容'],
      ['plan_date', 'date', '', '', '', '计划日期'],
      ['target_type', 'varchar', '40', '', '', '关联对象类型'],
      ['target_id', 'bigint', '', '', '', '关联对象ID'],
      ['status', 'varchar', '30', '', '', 'pending/completed/cancelled'],
      ['completed_at', 'datetime', '', '', '', '完成时间'],
    ],
  },
  {
    cn: '资源分享表',
    en: 'shared_resource',
    rows: [
      ['id', 'bigint', '', '主键', '', '分享资源ID'],
      ['owner_id', 'bigint', '', '外键', 'user', '上传者'],
      ['title', 'varchar', '120', '', '', '标题'],
      ['category', 'varchar', '40', '', '', '分类'],
      ['kind', 'varchar', '40', '', '', '资源形态'],
      ['file_url', 'varchar', '500', '', '', '文件地址'],
      ['cover_url', 'varchar', '500', '', '', '视频封面/封面图'],
      ['view_count', 'int', '', '', '', '浏览数'],
      ['like_count', 'int', '', '', '', '收藏/喜欢数'],
    ],
  },
  {
    cn: '论坛主题表',
    en: 'forum_thread',
    rows: [
      ['id', 'bigint', '', '主键', '', '主题ID'],
      ['user_id', 'bigint', '', '外键', 'user', '发帖用户'],
      ['board_id', 'varchar', '40', '', '', '板块ID'],
      ['title', 'varchar', '120', '', '', '主题标题'],
      ['content', 'text', '', '', '', '主题内容'],
      ['tags', 'varchar', '300', '', '', '标签'],
      ['view_count', 'int', '', '', '', '浏览数'],
      ['like_count', 'int', '', '', '', '点赞数'],
      ['reply_count', 'int', '', '', '', '回复数'],
    ],
  },
  {
    cn: '论坛回复表',
    en: 'forum_reply',
    rows: [
      ['id', 'bigint', '', '主键', '', '回复ID'],
      ['thread_id', 'bigint', '', '外键', 'forum_thread', '所属主题'],
      ['user_id', 'bigint', '', '外键', 'user', '回复用户'],
      ['content', 'text', '', '', '', '回复内容'],
      ['created_at', 'datetime', '', '', '', '回复时间'],
    ],
  },
];

const aiRows = [
  ['序号', '项目阶段', 'AI占比', '具体用途'],
  ['1', '需求分析', '35%', '辅助梳理学习平台的用户角色、资料管理、题库练习、错题复盘、资源分享、论坛与AI助手等需求边界。'],
  ['2', '数据库设计', '30%', '参考实体关系和业务流转，检查 user、learning_resource、question、wrong_question、study_plan、forum_thread 等表之间的外键关系。'],
  ['3', '编码开发', '25%', '用于生成局部代码思路、接口调用示例、前端交互方案和样式调整建议，最终代码结合项目结构人工修改。'],
  ['4', 'Bug调试', '35%', '协助定位上传失败、视频播放、分类不显示、自动判题、个人信息保存等问题，并根据日志与代码逐项修复。'],
  ['5', '测试', '20%', '生成模块测试清单，辅助检查前后端构建、接口连通、数据库字段、账号隔离和模块联动。'],
  ['6', '文档撰写', '45%', '辅助归纳项目背景、功能说明、技术架构、数据库设计和AI使用案例，图表由项目实际结构校对。'],
  ['7', '系统部署', '10%', '主要由人工完成，AI仅辅助整理启动命令和环境注意事项。'],
];

const apiRows = [
  ['序号', '模块名称', '数据表', '功能点', 'API接口', '请求方式', '对应页面', '前端开发', '后端开发'],
  ['1', '用户与权限管理', 'user, role, operation_log', '用户注册', '/api/users/register', 'POST', 'App.vue 登录/注册页', '', ''],
  ['', '', '', '用户登录与Token保存', '/api/users/login', 'POST', 'App.vue 登录页', '', ''],
  ['', '', '', '查询当前用户资料', '/api/users/me', 'GET', 'App.vue 个人主页', '', ''],
  ['', '', '', '修改个人资料', '/api/users/me/profile', 'PUT', 'App.vue 个人主页编辑', '', ''],
  ['', '', '', '上传头像/背景', '/api/users/me/profile-image', 'POST', 'App.vue 个人主页编辑', '', ''],
  ['', '', '', '修改密码', '/api/users/me/password', 'PUT', 'App.vue 设置区', '', ''],
  ['2', '首页推荐与学习中枢', 'home_banner, learning_resource, question, study_plan, wrong_question', '读取首页画幅', '/api/home-banners', 'GET', 'App.vue 推荐首页', '', ''],
  ['', '', '', '保存首页画幅', '/api/home-banners', 'PUT', 'App.vue 首页画幅编辑', '', ''],
  ['', '', '', '上传首页图片', '/api/home-banners/image', 'POST', 'App.vue 首页画幅编辑', '', ''],
  ['', '', '', '学习联动概览', '/api/study-links/overview', 'GET', 'App.vue 推荐首页', '', ''],
  ['3', '学习资料模块', 'learning_resource, question', '资料列表与筛选', '/api/resources', 'GET', 'LearningResourceWorkspace.vue', '', ''],
  ['', '', '', '上传学习资料', '/api/resources', 'POST', 'LearningResourceWorkspace.vue', '', ''],
  ['', '', '', '查看资料详情', '/api/resources/{id}', 'GET', 'LearningResourceWorkspace.vue', '', ''],
  ['', '', '', '保存学习进度', '/api/resources/{id}/progress', 'PUT', 'LearningResourceWorkspace.vue', '', ''],
  ['', '', '', '保存批注与知识卡片', '/api/resources/{id}/annotations', 'PUT', 'LearningResourceWorkspace.vue', '', ''],
  ['', '', '', '从资料生成题目', '/api/resources/{id}/questions', 'POST', 'LearningResourceWorkspace.vue', '', ''],
  ['', '', '', '图片截图生成题目', '/api/resources/{id}/questions/image', 'POST', 'LearningResourceWorkspace.vue', '', ''],
  ['', '', '', '批量抽取题目', '/api/resources/{id}/questions/extract', 'POST', 'LearningResourceWorkspace.vue', '', ''],
  ['4', '题库与分类模块', 'question, category, tag, question_bank_setting', '题目分页查询', '/api/questions', 'GET', 'QuestionBankWorkspace.vue', '', ''],
  ['', '', '', '新增题目', '/api/questions', 'POST', 'QuestionBankWorkspace.vue', '', ''],
  ['', '', '', '修改题目', '/api/questions/{id}', 'PUT', 'QuestionBankWorkspace.vue', '', ''],
  ['', '', '', '题目移入回收站', '/api/questions/{id}', 'DELETE', 'QuestionBankWorkspace.vue', '', ''],
  ['', '', '', '恢复题目', '/api/questions/{id}/restore', 'PUT', 'QuestionBankWorkspace.vue', '', ''],
  ['', '', '', '分类树与知识点概览', '/api/classifications/overview', 'GET', 'QuestionBankWorkspace.vue', '', ''],
  ['', '', '', '新增/修改/删除分类', '/api/classifications/categories', 'POST/PUT/DELETE', 'QuestionBankWorkspace.vue', '', ''],
  ['5', '练习与错题模块', 'answer_record, wrong_question, question', '生成练习卷', '/api/question-bank/practice/questions', 'POST', 'QuestionBankWorkspace.vue', '', ''],
  ['', '', '', '提交答案自动判题', '/api/question-bank/practice/answers', 'POST', 'QuestionBankWorkspace.vue', '', ''],
  ['', '', '', '练习仪表盘', '/api/question-bank/dashboard', 'GET', 'QuestionBankWorkspace.vue', '', ''],
  ['', '', '', '题库统计分析', '/api/question-bank/analytics', 'GET', 'QuestionBankWorkspace.vue', '', ''],
  ['', '', '', '错题列表', '/api/wrong-questions', 'GET', 'WrongQuestionWorkspace.vue', '', ''],
  ['', '', '', '标记已掌握/删除错题', '/api/wrong-questions/{id}', 'PUT/DELETE', 'WrongQuestionWorkspace.vue', '', ''],
  ['6', '学习计划模块', 'study_plan', '计划列表', '/api/study-plans', 'GET', 'StudyPlanWorkspace.vue', '', ''],
  ['', '', '', '新增学习计划', '/api/study-plans', 'POST', 'StudyPlanWorkspace.vue', '', ''],
  ['', '', '', '修改计划', '/api/study-plans/{id}', 'PUT', 'StudyPlanWorkspace.vue', '', ''],
  ['', '', '', '删除计划', '/api/study-plans/{id}', 'DELETE', 'StudyPlanWorkspace.vue', '', ''],
  ['', '', '', '更新计划状态', '/api/study-plans/{id}/status', 'PUT', 'StudyPlanWorkspace.vue', '', ''],
  ['7', '资源分享模块', 'shared_resource, shared_resource_favorite', '资源广场列表', '/api/shared-resources', 'GET', 'ResourceShareWorkspace.vue', '', ''],
  ['', '', '', '上传/发布资源', '/api/shared-resources', 'POST', 'ResourceShareWorkspace.vue', '', ''],
  ['', '', '', '记录浏览', '/api/shared-resources/{id}/view', 'PUT', 'ResourceShareWorkspace.vue', '', ''],
  ['', '', '', '收藏资源', '/api/shared-resources/{id}/favorite', 'PUT', 'ResourceShareWorkspace.vue', '', ''],
  ['8', '学习论坛模块', 'forum_thread, forum_reply, forum_thread_like, forum_thread_favorite', '主题列表', '/api/forum/threads', 'GET', 'StudyForumWorkspace.vue', '', ''],
  ['', '', '', '发布主题', '/api/forum/threads', 'POST', 'StudyForumWorkspace.vue', '', ''],
  ['', '', '', '主题浏览记录', '/api/forum/threads/{id}/view', 'PUT', 'StudyForumWorkspace.vue', '', ''],
  ['', '', '', '回复主题', '/api/forum/threads/{id}/replies', 'POST', 'StudyForumWorkspace.vue', '', ''],
  ['', '', '', '收藏/点赞主题', '/api/forum/threads/{id}/favorite, /like', 'PUT', 'StudyForumWorkspace.vue', '', ''],
  ['9', 'AI辅助模块', 'learning_resource, question', 'AI服务状态', '/api/ai-assistant/status', 'GET', 'AiAssistantWorkspace.vue', '', ''],
  ['', '', '', 'AI分析与抽题建议', '/api/ai-assistant/analyze', 'POST', 'AiAssistantWorkspace.vue', '', ''],
];

function databaseSection() {
  let xml = '';
  databaseTables.forEach((item, index) => {
    xml += paragraph(`表6-${index + 1}  ${item.cn}`, { align: 'center', size: 18, after: 60 });
    xml += table([
      ['表名（中文）', item.cn],
      ['表名（英文）', item.en],
      ['字段名', '类型', '长度', '主键/外键', '参照表', '说明'],
      ...item.rows,
    ], [1500, 1150, 800, 1200, 1350, 3026], { headerRows: 0, fontSize: 18, tableWidth: usablePortrait });
    xml += paragraph('', { after: 120 });
  });
  return xml;
}

function titlePage() {
  return [
    paragraph('组号：', { align: 'center', size: 24, after: 420 }),
    paragraph('《ToveloKno 智能学习资源与题库平台》', { align: 'center', bold: true, size: 34, after: 280 }),
    paragraph('项目类型：Web 学习平台 / 前后端分离应用', { align: 'center', size: 24, after: 140 }),
    paragraph('2025-2026学年夏季学期', { align: 'center', size: 24, after: 140 }),
    paragraph('项目开发总结报告', { align: 'center', bold: true, size: 30, after: 520 }),
    paragraph('说明：本文已填写项目相关的非个人信息内容；成员学号、姓名、个人成绩、个人贡献度等个人信息栏保留空白，便于后续由小组自行补充。', { align: 'center', size: 20, color: '666666', after: 260 }),
    pageBreak(),
  ].join('');
}

function evaluationTables() {
  const memberRows = [
    ['学号（从小到大）', '班级', '姓名', '个人成绩'],
    ['', '', '', ''],
    ['', '', '', ''],
    ['', '', '', ''],
    ['', '', '', ''],
    ['', '', '', ''],
    ['项目成绩', '', '', ''],
  ];
  const scoreRows = [
    ['指标', '细则', '等级', '得分'],
    ['报告（10分）', '内容详细度', '□好 □较好 □一般 □较差 □差', ''],
    ['', '格式规范度', '□好 □较好 □一般 □较差 □差', ''],
    ['', '绘图标准度', '□好 □较好 □一般 □较差 □差', ''],
    ['数据（10分）', '基本数据完善度', '□好 □较好 □一般 □较差 □差', ''],
    ['', '关联关系正确度', '□好 □较好 □一般 □较差 □差', ''],
    ['', 'ER图与数据表', '□好 □较好 □一般 □较差 □差', ''],
    ['页面（10分）', '布局、配色、使用便捷、流畅', '□好 □较好 □一般 □较差 □差', ''],
    ['AI工具（10分）', '使用合理、有限度、高效', '□好 □较好 □一般 □较差 □差', ''],
    ['功能（60分）', '功能完整性', '□好 □较好 □一般 □较差 □差', ''],
    ['', '功能实用性', '□好 □较好 □一般 □较差 □差', ''],
    ['', '创新性', '□好 □较好 □一般 □较差 □差', ''],
    ['', '系统稳定性', '□好 □较好 □一般 □较差 □差', ''],
  ];
  return [
    heading('项目评价', 1),
    paragraph('以下评价表保留为空白，供课程验收和教师评分时填写。', { size: 21 }),
    table(memberRows, [2300, 1800, 1800, 1800], { fontSize: 19 }),
    paragraph('', { after: 180 }),
    table(scoreRows, [1600, 3000, 3200, 1200], { fontSize: 18 }),
    pageBreak(),
  ].join('');
}

function bodyContent(imageMap) {
  let xml = '';
  xml += heading('1  引言', 1);
  xml += heading('1.1 项目背景', 2);
  xml += paragraph('ToveloKno = "to love knowledge"，项目定位为面向个人学习场景的智能学习资源与题库平台。系统围绕“资料整理、题库练习、错题复盘、学习计划、资源分享、论坛交流、AI辅助”形成闭环，解决学习资料分散、题目难以沉淀、错题复盘缺少计划、资源与讨论无法互通等问题。', { indentFirstLine: 420 });
  xml += paragraph('项目采用前后端分离方式实现，前端提供具有淡蓝、粉色、线稿插画风格的学习控制台，后端提供基于账号的数据隔离、文件上传、题库判题、计划管理和社区互动等接口。', { indentFirstLine: 420 });
  xml += heading('1.2 术语和缩写词', 2);
  xml += bullet([
    'API：Application Programming Interface，前后端之间调用的接口。',
    'JWT/Token：用户登录后保存的访问凭证，用于接口鉴权。',
    'CRUD：Create、Read、Update、Delete，即增删改查。',
    'ER图：Entity Relationship Diagram，实体关系图。',
    'Ebbinghaus：艾宾浩斯遗忘曲线，用于安排复习节奏。',
    'SPA：Single Page Application，单页前端应用。',
  ]);

  xml += heading('2  项目概述', 1);
  xml += heading('2.1 项目目标', 2);
  xml += bullet([
    '建设统一的个人学习控制台，支持登录注册、个人主页、首页推荐画幅和学习统计展示。',
    '实现学习资料上传、预览、进度记录、批注、截图生成知识卡片和从资料生成题目的能力。',
    '实现题库管理、分类树、知识点管理、自动判题、错题本和统计分析。',
    '实现学习计划与错题、资料的联动，并引入艾宾浩斯复习节奏帮助安排复盘。',
    '实现资源分享与论坛模块，支持用户上传资源、分类浏览、收藏点赞、发帖回复。',
    '接入AI辅助能力，用于资料分析、题目生成建议和开发过程中的需求/调试辅助。',
  ]);
  xml += heading('2.2 业务需求', 2);
  xml += heading('2.2.1 业务指标', 3);
  xml += bullet([
    '学习资料、题库题目、错题、计划、论坛内容均与当前账号绑定，避免跨用户数据混淆。',
    '资料上传上限支持大文件学习资源，资源分享模块支持视频封面截取与收藏页。',
    '题库练习支持提交后自动判断对错，错误题目自动进入错题本。',
    '首页展示学习资料、卡片复习、练习正确率、计划进度等统计，形成模块间联动。',
  ]);
  xml += heading('2.2.2 技术指标', 3);
  xml += bullet([
    '前端主要使用 Vue 3、Vite、Axios，实现组件化页面和统一接口调用。',
    '后端主要使用 Spring Boot、Spring MVC、Spring Data JPA、MySQL，实现REST接口和持久化。',
    '接口采用 Bearer Token 鉴权，后端拦截器解析当前用户并进行数据隔离。',
    '文件存储采用本地 uploads 目录，并通过静态资源映射提供访问。',
    '常规列表接口采用分页或筛选参数，保证资料、题目、错题等数据量增长后的可用性。',
  ]);

  xml += heading('3  需求分析', 1);
  xml += heading('3.1 目标用户分析', 2);
  xml += paragraph('系统目标用户为需要长期整理知识与复盘错题的学习者。用户在系统中既是资料上传者，也是题库维护者、练习者和社区参与者。管理员角色主要负责系统配置、基础数据维护和异常数据处理。', { indentFirstLine: 420 });
  xml += bullet([
    '普通用户：上传资料、生成知识卡片、维护题库、做题、复盘错题、制定计划、分享资源、参与论坛。',
    '管理员/维护者：维护系统运行环境、处理用户反馈、检查数据表和接口日志。',
    'AI辅助工具：作为外部能力辅助资料分析、题目生成、需求梳理和调试，不直接替代用户决策。',
  ]);
  xml += heading('3.2 功能需求', 2);
  xml += heading('3.2.1 用户与首页功能需求', 3);
  xml += paragraph('用户可以注册、登录、维护个人资料、上传头像和主页背景。登录后进入推荐首页，首页展示推荐画幅、学习统计、资料/错题/计划联动入口，并提供到各个学习模块的导航。', { indentFirstLine: 420 });
  xml += heading('3.2.2 学习资料与知识卡片功能需求', 3);
  xml += paragraph('学习资料模块支持上传PDF、图片、视频和文档，记录学习进度、当前页、学习时长、批注和收藏状态。用户可从资料截图生成知识卡片，卡片可以单独打开，用于复习和沉淀知识。', { indentFirstLine: 420 });
  xml += heading('3.2.3 题库、错题和学习计划功能需求', 3);
  xml += paragraph('题库模块支持题目增删改查、分类树、知识点、标签、回收站、批量归档和练习生成。练习过程中系统根据标准答案自动判断对错，错误题目进入错题本；学习计划模块支持将资料或错题加入复习安排，并以艾宾浩斯曲线辅助复盘。', { indentFirstLine: 420 });
  xml += heading('3.2.4 资源分享、论坛与AI辅助功能需求', 3);
  xml += paragraph('资源分享模块允许用户自行上传资源、设置分类、标签、封面和说明，其他用户可以浏览、收藏。论坛模块参考NGA式板块和帖子结构，支持发帖、回复、浏览、点赞和收藏。AI辅助模块用于对学习资料或用户输入进行分析，返回学习建议和题目生成结果。', { indentFirstLine: 420 });
  xml += heading('3.3 性能需求', 2);
  xml += bullet([
    '普通页面交互应在1秒内完成主要反馈，上传和AI分析等耗时操作显示加载状态。',
    '资源上传、视频播放和题目抽取应避免阻塞页面主流程。',
    '题库和错题统计应按账号隔离并可重复刷新，避免首页统计一会儿为0一会儿为1的错乱体验。',
    '系统应在后端接口异常时给出明确错误提示，避免用户误认为数据已经保存。',
  ]);

  xml += heading('4 系统总体设计', 1);
  xml += heading('4.1 功能架构设计', 2);
  xml += paragraph('系统以首页学习控制台为入口，将资料、题库、错题、计划、社区和AI辅助串联起来。资料可生成题目和卡片，练习结果可进入错题本，错题和资料可进一步形成复习计划。', { indentFirstLine: 420 });
  xml += drawing(imageMap.function, { name: '功能架构图', widthDxa: 7600, heightDxa: 4560 });
  xml += caption('图4-1 ToveloKno功能架构图');
  xml += heading('4.2 技术架构设计', 2);
  xml += paragraph('前端通过 Vue 3 与 Axios 调用后端REST接口；后端通过 Controller、Service、Repository 分层处理业务，并将用户、资源、题库、错题、计划和论坛等数据保存到 MySQL。上传文件保存到本地目录并暴露静态访问地址。', { indentFirstLine: 420 });
  xml += drawing(imageMap.tech, { name: '技术架构图', widthDxa: 7600, heightDxa: 4560 });
  xml += caption('图4-2 ToveloKno技术架构图');

  xml += heading('5 系统详细设计', 1);
  xml += heading('5.1 用户登录与首页模块', 2);
  xml += heading('5.1.1 模块描述', 3);
  xml += paragraph('该模块负责用户进入系统的第一步体验。登录页采用淡色背景、线稿几何图形和柔和渐变输入框；首页保留同一视觉语言，通过侧边导航、推荐画幅和顶部学习统计展示当前账号的学习状态。', { indentFirstLine: 420 });
  xml += heading('5.1.2 界面设计', 3);
  xml += drawing(imageMap.login, { name: '登录界面截图', widthDxa: 7600, heightDxa: 4280 });
  xml += caption('图5-1 登录界面');
  xml += drawing(imageMap.home, { name: '首页截图', widthDxa: 7600, heightDxa: 4280 });
  xml += caption('图5-2 推荐首页界面');
  xml += heading('5.2 题库练习与错题模块', 2);
  xml += paragraph('题库练习模块按照练习模式生成题目。用户提交答案后，后端对选择题、判断题、填空题和简答题进行归一化比较，生成答题记录；若答错则写入错题表并刷新统计。', { indentFirstLine: 420 });
  xml += drawing(imageMap.practiceFlow, { name: '题库自动判题流程图', widthDxa: 7400, heightDxa: 4760 });
  xml += caption('图5-3 题库自动判题流程图');
  xml += heading('5.3 学习资料与知识卡片模块', 2);
  xml += paragraph('学习资料模块负责上传和阅读资料，保存进度、页码、学习时长与批注。用户可将资料截图加入知识卡片，也可从资料内容抽取题目进入题库，形成“资料-卡片-题库”的学习链路。', { indentFirstLine: 420 });
  xml += drawing(imageMap.resourceFlow, { name: '学习资料知识卡片流程图', widthDxa: 7400, heightDxa: 4760 });
  xml += caption('图5-4 学习资料与知识卡片流程图');
  xml += heading('5.4 资源分享与论坛模块', 2);
  xml += paragraph('资源分享模块提供类视频网站/资料站的资源广场，支持上传文件、链接、封面、分类和收藏。论坛模块采用板块、主题、回复、点赞、收藏的结构，使学习资源和讨论可以围绕同一账号体系互通。', { indentFirstLine: 420 });
  xml += drawing(imageMap.communityFlow, { name: '资源分享论坛流程图', widthDxa: 7400, heightDxa: 4760 });
  xml += caption('图5-5 资源分享与论坛互动流程图');

  xml += heading('6 数据库设计', 1);
  xml += heading('6.1 概念结构设计', 2);
  xml += paragraph('数据库以 user 为核心实体，学习资料、题库题目、答题记录、错题、学习计划、分享资源、论坛主题等均通过 user_id、created_by 或 owner_id 与当前账号关联。题目可关联分类和学习资料，答题记录与错题表共同支撑自动判题和复盘功能。', { indentFirstLine: 420 });
  xml += drawing(imageMap.er, { name: 'ER图', widthDxa: 7800, heightDxa: 4730 });
  xml += caption('图6-1 ToveloKno核心ER图');
  xml += heading('6.2 逻辑结构设计', 2);
  xml += paragraph('本系统核心数据模型可概括为：用户（用户ID，用户名，密码，邮箱，头像，个人资料）；学习资料（资料ID，用户ID，文件地址，进度，批注）；题目（题目ID，题干，题型，答案，分类ID，来源资料）；答题记录（用户ID，题目ID，答案，是否正确）；错题（用户ID，题目ID，错误次数）；学习计划（用户ID，计划日期，关联对象）；资源分享与论坛数据。', { indentFirstLine: 420 });
  xml += databaseSection();

  xml += heading('7 完成情况', 1);
  xml += heading('7.1 系统描述', 2);
  xml += paragraph('项目已完成前端主要页面与后端主要接口联通，包含登录注册、首页学习中枢、个人主页、学习资料、知识卡片、题库、分类、自动判题、错题本、学习计划、资源分享、论坛和AI辅助等模块。项目结构如下图所示。', { indentFirstLine: 420 });
  xml += drawing(imageMap.source, { name: '源码结构图', widthDxa: 7400, heightDxa: 4760 });
  xml += caption('图7-1 项目源码结构图');
  xml += heading('7.2 功能展示', 2);
  xml += bullet([
    '用户登录：输入用户名和密码后获取Token，进入首页，后续请求自动携带Authorization。',
    '学习资料：支持上传、播放/预览、进度保存、批注和截图卡片。',
    '题库练习：题目可按分类管理，练习提交后后端自动判题并记录到答题记录。',
    '错题复盘：答错题目进入错题本，可标记掌握或生成学习计划。',
    '资源分享与论坛：用户可以发布资源和主题，进行收藏、点赞和回复。',
  ]);
  xml += heading('7.3 遗留问题', 2);
  xml += bullet([
    '知识卡片当前仍以本地存储为主，后续可增加独立后端表实现跨设备同步。',
    'AI辅助依赖外部模型服务，需要在部署环境配置API Key并做好费用控制。',
    '资源文件目前存储在本地目录，后续可迁移到对象存储以提升扩展性。',
  ]);

  xml += heading('8 AI辅助', 1);
  xml += heading('8.1 AI工具介绍', 2);
  xml += paragraph('本项目开发过程中主要使用 OpenAI API / Codex 类AI辅助工具。使用方式包括：需求拆解、接口与数据表设计检查、前端交互方案参考、错误日志分析、局部代码改造建议、测试清单生成和报告文字整理。AI输出均需要结合项目代码、数据库结构和实际运行效果进行人工确认。', { indentFirstLine: 420 });
  xml += heading('8.2 AI工具主要用途', 2);
  xml += table(aiRows, [800, 1800, 1100, 5200], { fontSize: 18, tableWidth: usablePortrait });
  xml += paragraph('具体案例包括：分析资料上传失败原因、补充视频封面截取逻辑、检查学习资料与题库联动、完善题库分类显示、为学习计划加入艾宾浩斯复习说明、设计论坛模块、实现题目自动判题、整理数据库ER图和接口明细表。', { indentFirstLine: 420 });

  xml += heading('9 经验总结', 1);
  xml += paragraph('本项目最大的经验是：学习平台不能只做独立页面，必须把资料、题库、错题、计划和社区内容用同一账号体系串起来。否则用户会感觉功能是“假联动”。因此后端接口和数据库设计要优先考虑用户ID、来源ID、目标类型等关联字段。', { indentFirstLine: 420 });
  xml += paragraph('技术上，前后端联调时需要同时关注界面效果、接口返回、数据库落库和刷新后的状态一致性。上传、视频播放、自动判题、分类树等功能都容易出现“看起来有按钮但数据没保存”的问题，必须用真实接口和数据库验证。AI工具适合帮助发现思路和生成候选方案，但最终仍要以实际代码、构建结果和用户体验为准。', { indentFirstLine: 420 });

  return xml;
}

function appendixContent() {
  let xml = '';
  xml += sectionBreakLandscape();
  xml += heading('附录A：模块及接口明细', 1);
  xml += paragraph('总计：系统功能模块9个，核心功能点45个以上，核心数据表约16张，主要前端页面/工作台10个以上。技术实现点包括 Vue 3、Vite、Axios、Spring Boot、Spring MVC、Spring Data JPA、MySQL、Token鉴权、文件上传、静态资源映射、OpenAI API 辅助分析等。', { size: 20 });
  xml += table(apiRows, [620, 1350, 2200, 1900, 2580, 820, 2050, 640, 640], { fontSize: 13, tableWidth: usableLandscape });
  xml += sectionBreakPortrait();
  xml += heading('个人工作量统计', 1);
  xml += paragraph('以下表格涉及学号、姓名和贡献度，属于个人信息，请由小组成员按实际情况自行填写。', { size: 21 });
  xml += table([
    ['学号', '姓名', '前端页面', '功能点（个）', '文档（%）', '其它', '贡献度（%）'],
    ['', '', '', '', '', '', ''],
    ['', '', '', '', '', '', ''],
    ['', '', '', '', '', '', ''],
    ['', '', '', '', '', '', ''],
    ['', '', '', '', '', '', ''],
  ], [1100, 900, 2400, 1000, 900, 1700, 1026], { fontSize: 18, tableWidth: usablePortrait });
  xml += heading('附录B：AI功能模块代码', 1);
  xml += paragraph('AI辅助模块后端核心类包括 AiAssistantController、AiAssistantService、AiAssistantDtos，并复用 QuestionExtractionService 对学习资料内容进行题目候选提取。前端对应 AiAssistantWorkspace.vue，提供输入、分析、结果展示与题库联动入口。', { indentFirstLine: 420 });
  xml += paragraph('主要接口：GET /api/ai-assistant/status 用于检查AI配置状态；POST /api/ai-assistant/analyze 用于提交资料片段或用户输入，返回摘要、学习建议和题目候选。', { indentFirstLine: 420 });
  return xml;
}

function documentXml(imageMap) {
  const body = [
    titlePage(),
    evaluationTables(),
    bodyContent(imageMap),
    appendixContent(),
    sectPrPortrait(),
  ].join('');
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:wpc="http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas" xmlns:cx="http://schemas.microsoft.com/office/drawing/2014/chartex" xmlns:cx1="http://schemas.microsoft.com/office/drawing/2015/9/8/chartex" xmlns:cx2="http://schemas.microsoft.com/office/drawing/2015/10/21/chartex" xmlns:cx3="http://schemas.microsoft.com/office/drawing/2016/5/9/chartex" xmlns:cx4="http://schemas.microsoft.com/office/drawing/2016/5/10/chartex" xmlns:cx5="http://schemas.microsoft.com/office/drawing/2016/5/11/chartex" xmlns:cx6="http://schemas.microsoft.com/office/drawing/2016/5/12/chartex" xmlns:cx7="http://schemas.microsoft.com/office/drawing/2016/5/13/chartex" xmlns:cx8="http://schemas.microsoft.com/office/drawing/2016/5/14/chartex" xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006" xmlns:aink="http://schemas.microsoft.com/office/drawing/2016/ink" xmlns:am3d="http://schemas.microsoft.com/office/drawing/2017/model3d" xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:r="${R_NS}" xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math" xmlns:v="urn:schemas-microsoft-com:vml" xmlns:wp14="http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing" xmlns:wp="${WP_NS}" xmlns:w10="urn:schemas-microsoft-com:office:word" xmlns:w="${W_NS}" xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml" xmlns:w15="http://schemas.microsoft.com/office/word/2012/wordml" xmlns:w16cex="http://schemas.microsoft.com/office/word/2018/wordml/cex" xmlns:w16cid="http://schemas.microsoft.com/office/word/2016/wordml/cid" xmlns:w16="http://schemas.microsoft.com/office/word/2018/wordml" xmlns:w16du="http://schemas.microsoft.com/office/word/2023/wordml/word16du" xmlns:w16sdtdh="http://schemas.microsoft.com/office/word/2020/wordml/sdtdatahash" xmlns:w16se="http://schemas.microsoft.com/office/word/2015/wordml/symex" xmlns:wpg="http://schemas.microsoft.com/office/word/2010/wordprocessingGroup" xmlns:wpi="http://schemas.microsoft.com/office/word/2010/wordprocessingInk" xmlns:wne="http://schemas.microsoft.com/office/word/2006/wordml" xmlns:wps="http://schemas.microsoft.com/office/word/2010/wordprocessingShape" mc:Ignorable="w14 w15 w16se w16cid w16 w16cex w16sdtdh w16du wp14"><w:body>${body}</w:body></w:document>`;
}

function relsXml(imageFiles) {
  const rels = [
    ['rId1', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles', 'styles.xml'],
    ['rId2', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/numbering', 'numbering.xml'],
    ['rId3', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings', 'settings.xml'],
    ['rId4', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/webSettings', 'webSettings.xml'],
    ['rId5', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/footnotes', 'footnotes.xml'],
    ['rId6', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/endnotes', 'endnotes.xml'],
    ['rId7', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/fontTable', 'fontTable.xml'],
    ['rId8', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme', 'theme/theme1.xml'],
  ];
  imageFiles.forEach((img, index) => {
    rels.push([`rId${100 + index}`, 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/image', `media/${path.basename(img)}`]);
  });
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="${REL_NS}">${rels.map(([id, type, target]) => `<Relationship Id="${id}" Type="${type}" Target="${target}"/>`).join('')}</Relationships>`;
}

async function build() {
  await fs.mkdir(outDir, { recursive: true });
  const diagrams = await makeDiagrams();
  const imageFiles = [
    loginShot,
    homeShot,
    diagrams.function,
    diagrams.tech,
    diagrams.practiceFlow,
    diagrams.resourceFlow,
    diagrams.communityFlow,
    diagrams.er,
    diagrams.source,
  ];
  const imageMap = {
    login: 'rId100',
    home: 'rId101',
    function: 'rId102',
    tech: 'rId103',
    practiceFlow: 'rId104',
    resourceFlow: 'rId105',
    communityFlow: 'rId106',
    er: 'rId107',
    source: 'rId108',
  };

  const zip = await JSZip.loadAsync(await fs.readFile(templatePath));
  zip.file('word/document.xml', documentXml(imageMap));
  zip.file('word/_rels/document.xml.rels', relsXml(imageFiles));
  Object.keys(zip.files)
    .filter((name) => name.startsWith('word/media/') && name !== 'word/media/')
    .forEach((name) => zip.remove(name));
  for (const img of imageFiles) {
    zip.file(`word/media/${path.basename(img)}`, await fs.readFile(img));
  }
  const content = await zip.generateAsync({ type: 'nodebuffer', compression: 'DEFLATE' });
  await fs.writeFile(finalPath, content);
  console.log(finalPath);
}

build().catch((error) => {
  console.error(error);
  process.exit(1);
});
