import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'node:path'

// Vite 配置文件：启用 Vue 单文件组件支持。
export default defineConfig({
  plugins: [vue()],
  // 使用相对资源路径，便于课程项目在子目录或静态服务器中部署。
  base: './',
  build: {
    // 保留小组原有首页，同时为题库模块提供独立构建入口。
    rollupOptions: {
      input: {
        main: resolve(__dirname, 'index.html'),
        questionBank: resolve(__dirname, 'question-bank.html'),
      },
    },
  },
})
