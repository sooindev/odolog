import { Navigate, Route, Routes } from 'react-router'

import { ProtectedRoute } from '@/auth/ProtectedRoute'
import { Header } from '@/components/Header'
import { LoginPage } from '@/pages/LoginPage'
import { ProfilePage } from '@/pages/ProfilePage'
import { SignUpPage } from '@/pages/SignUpPage'
import { VehicleListPage } from '@/pages/VehicleListPage'

function App() {
  return (
    <div className="bg-muted/40 min-h-screen">
      <Header />
      <main className="mx-auto max-w-3xl px-4 py-6">
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignUpPage />} />

          {/* element에 ProtectedRoute만 두고 path가 없는 라우트 = 자식들을 감싸는 울타리 */}
          <Route element={<ProtectedRoute />}>
            <Route path="/vehicles" element={<VehicleListPage />} />
            <Route path="/me" element={<ProfilePage />} />
          </Route>

          {/* 어디에도 안 맞는 주소는 차량 목록으로 (로그인 안 했으면 거기서 다시 /login으로) */}
          <Route path="*" element={<Navigate to="/vehicles" replace />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
