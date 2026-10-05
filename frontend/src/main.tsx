import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import { QueryClientProvider } from '@tanstack/react-query'

import App from '@/app/App'
import { I18nProvider } from '@/app/I18nProvider'
import { AuthProvider } from '@/features/auth/context/AuthProvider'
import { ThemeProvider } from '@/shared/theme/ThemeProvider'
import { createQueryClient } from '@/shared/api/queryClient'
import '@/index.css'

// 앱 수명 동안 하나. 계정이 바뀌면 AuthProvider 가 비움
const queryClient = createQueryClient()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    {/* ThemeProvider 가 가장 바깥. 테마는 로그인·주소와 무관하게 앱 전체 */}
    <ThemeProvider>
      {/* AuthProvider 바깥. 로그인·로그아웃 때 캐시를 비워야 해서 */}
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <AuthProvider>
            {/* 계정 설정을 읽어야 해서 AuthProvider 안쪽 */}
            <I18nProvider>
              <App />
            </I18nProvider>
          </AuthProvider>
        </BrowserRouter>
      </QueryClientProvider>
    </ThemeProvider>
  </StrictMode>,
)
