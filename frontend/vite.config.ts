import { fileURLToPath, URL } from 'node:url'

import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
// vitest/config 의 defineConfig — vite 의 것을 그대로 감싸고 test 키만 더함
// 별도 vitest.config.ts 를 두지 않는 이유는 '@' 별칭이 두 곳으로 갈리기 때문
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  // 문서가 안내하는 npm run preview 가 실제로 동작하게 한다
  // 빌드 산출물의 BASE_URL 은 '' 라 /api 로 나가고, 그걸 백엔드로 넘겨준다
  preview: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    // useCountUp 이 requestAnimationFrame·matchMedia 를 씀. node 환경에는 없음
    environment: 'jsdom',
    // 대상 파일 옆에 둔다. 백엔드가 테스트 경로를 대상과 맞추는 것과 같은 방식
    include: ['src/**/*.test.{ts,tsx}'],
  },
})
