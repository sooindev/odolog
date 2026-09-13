import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'

import App from '@/app/root/App'
import { AuthProvider } from '@/features/auth/context/provider/AuthProvider'
import { ThemeProvider } from '@/shared/theme/provider/ThemeProvider'
import '@/index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    {/* ThemeProvider를 가장 바깥에 둔다. 테마는 로그인 여부나 현재 주소와 무관하게
        앱 전체에 걸리는 것이라, 안쪽에 두면 감싸는 범위가 실제보다 좁다고 말하는 셈이 된다. */}
    <ThemeProvider>
      <BrowserRouter>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  </StrictMode>,
)
