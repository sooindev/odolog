import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'

import App from '@/app/App'
import { I18nProvider } from '@/app/I18nProvider'
import { AuthProvider } from '@/features/auth/context/AuthProvider'
import { ThemeProvider } from '@/shared/theme/ThemeProvider'
import '@/index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    {/* ThemeProvider 가 가장 바깥. 테마는 로그인·주소와 무관하게 앱 전체 */}
    <ThemeProvider>
      <BrowserRouter>
        <AuthProvider>
          {/* 계정 설정을 읽어야 해서 AuthProvider 안쪽 */}
          <I18nProvider>
            <App />
          </I18nProvider>
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  </StrictMode>,
)
