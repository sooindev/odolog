import { defineConfig, devices } from '@playwright/test'

// 실제 브라우저로 핵심 흐름을 따라가는 테스트. `npm run e2e`
// 백엔드는 테스트 설정(odolog_test, 뜰 때마다 빈 스키마)으로 18080, 화면은 5174
// 평소 개발 서버(8080·5173)·운영 DB 와 섞이지 않게 포트를 따로 쓰고, 이미 떠 있는 서버를 재사용하지 않는다
export default defineConfig({
  testDir: './e2e',
  // 한 백엔드·한 DB 를 공유. 흐름끼리 데이터가 섞이지 않게 계정은 테스트마다 새로 만든다
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? 'list' : [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://localhost:5174',
    locale: 'ko-KR',
    timezoneId: 'Asia/Seoul',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      command: './gradlew bootTestRun',
      cwd: '..',
      url: 'http://localhost:18080/api/users/me',
      reuseExistingServer: false,
      timeout: 180_000,
      stdout: 'ignore',
    },
    {
      command: 'npx vite --port 5174 --strictPort',
      url: 'http://localhost:5174',
      env: { VITE_API_BASE_URL: 'http://localhost:18080' },
      reuseExistingServer: false,
      timeout: 60_000,
    },
  ],
})
