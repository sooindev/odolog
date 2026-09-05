import { Navigate, Route, Routes } from 'react-router'

import { ProtectedRoute } from '@/features/auth/ProtectedRoute'
import { Header } from '@/shared/layout/Header'
import { LoginPage } from '@/features/auth/LoginPage'
import { ProfilePage } from '@/features/auth/ProfilePage'
import { SignUpPage } from '@/features/auth/SignUpPage'
import { VehicleDetailPage } from '@/features/vehicles/VehicleDetailPage'
import { VehicleListPage } from '@/features/vehicles/VehicleListPage'
import { VehicleNewPage } from '@/features/vehicles/VehicleNewPage'

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
            {/* 'new'(리터럴)가 ':vehicleId'(변수)보다 먼저 매칭된다 — 라우터가 구체적인 경로를 우선한다 */}
            <Route path="/vehicles/new" element={<VehicleNewPage />} />
            <Route path="/vehicles/:vehicleId" element={<VehicleDetailPage />} />
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
