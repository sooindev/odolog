import { Navigate, Route, Routes, useLocation } from 'react-router'

import { AuthLayout } from '@/app/layout/AuthLayout'
import { Footer } from '@/app/layout/Footer'
import { LegalPage } from '@/app/LegalPage'
import { HomePage } from '@/app/HomePage'
import { ProtectedRoute } from '@/app/ProtectedRoute'
import { ForgotPasswordPage } from '@/features/auth/pages/ForgotPasswordPage'
import { ResetPasswordPage } from '@/features/auth/pages/ResetPasswordPage'
import { Header } from '@/app/layout/Header'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { ProfilePage } from '@/features/account/ProfilePage'
import { SignUpPage } from '@/features/auth/pages/SignUpPage'
import { VehicleDetailPage } from '@/features/vehicles/pages/VehicleDetailPage'
import { VehicleListPage } from '@/features/vehicles/pages/VehicleListPage'
import { VehicleNewPage } from '@/features/vehicles/pages/VehicleNewPage'

function App() {
  const location = useLocation()

  return (
    // dvh: 모바일 주소창 변화 반영
    // padding-inline: 노치 가로 모드 대응(viewport-fit=cover)
    <div className="min-h-dvh [padding-inline:env(safe-area-inset-left)_env(safe-area-inset-right)]">
      <Header />

      {/*
        본문 폭 76rem. 넓은 화면은 열 분할, 글 열은 700px 이하
        넉넉한 아래 여백
      */}
      <main className="mx-auto w-full max-w-[76rem] px-5 pt-10 pb-28 sm:px-8 sm:pt-20 sm:pb-40 lg:px-10">
        {/*
          pathname key 로 전환 연출 재실행
          0.18s 페이드만
        */}
        <div key={location.pathname} className="animate-fade">
          <Routes>
            {/* 세 얼굴의 갈림은 HomePage 안에 */}
            <Route path="/" element={<HomePage />} />
            {/* 로그인과 무관한 문서 */}
            <Route path="/privacy" element={<LegalPage kind="privacy" />} />
            <Route path="/terms" element={<LegalPage kind="terms" />} />

            {/* path 없는 라우트 = 자식을 감싸는 레이아웃 */}
            <Route element={<AuthLayout />}>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/signup" element={<SignUpPage />} />
              <Route path="/forgot-password" element={<ForgotPasswordPage />} />
              <Route path="/reset-password" element={<ResetPasswordPage />} />
            </Route>

            <Route element={<ProtectedRoute />}>
              <Route path="/vehicles" element={<VehicleListPage />} />
              {/* 'new' 가 ':vehicleId' 보다 우선 */}
              <Route path="/vehicles/new" element={<VehicleNewPage />} />
              <Route path="/vehicles/:vehicleId" element={<VehicleDetailPage />} />
              <Route path="/me" element={<ProfilePage />} />
            </Route>

            {/* 없는 주소는 소개 화면으로 */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </div>
      </main>

      {/* 노치 아래 여백은 푸터가 담당 */}
      <Footer />
    </div>
  )
}

export default App
