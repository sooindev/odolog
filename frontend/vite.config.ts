import { fileURLToPath, URL } from 'node:url'

import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
// vitest 설정도 여기에. '@' 별칭 중복 방지
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  // npm run preview 용 /api 프록시
  preview: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    // requestAnimationFrame·matchMedia 가 필요해 jsdom
    environment: 'jsdom',
    // 테스트는 대상 파일 옆
    include: ['src/**/*.test.{ts,tsx}'],
  },
})
