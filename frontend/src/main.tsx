import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'

import App from '@/app/root/App'
import { AuthProvider } from '@/features/auth/context/provider/AuthProvider'
import { ThemeProvider } from '@/shared/theme/provider/ThemeProvider'
import '@/index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    {/* ThemeProvider 가 가장 바깥. 테마는 로그인·주소와 무관하게 앱 전체 */}
    <ThemeProvider>
      <BrowserRouter>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  </StrictMode>,
)
