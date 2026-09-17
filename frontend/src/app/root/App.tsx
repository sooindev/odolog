import { Navigate, Route, Routes, useLocation } from 'react-router'

import { AuthLayout } from '@/app/layout/AuthLayout'
import { HomePage } from '@/app/home/HomePage'
import { ProtectedRoute } from '@/app/routing/ProtectedRoute'
import { Header } from '@/app/layout/Header'
import { LoginPage } from '@/features/auth/pages/login/LoginPage'
import { ProfilePage } from '@/features/auth/pages/profile/ProfilePage'
import { SignUpPage } from '@/features/auth/pages/signup/SignUpPage'
import { VehicleDetailPage } from '@/features/vehicles/pages/detail/VehicleDetailPage'
import { VehicleListPage } from '@/features/vehicles/pages/list/VehicleListPage'
import { VehicleNewPage } from '@/features/vehicles/pages/new/VehicleNewPage'

function App() {
  const location = useLocation()

  return (
    // dvh — 모바일 주소창 접힘에 따라 변함. 100vh 는 그걸 무시해 아래가 잘림
    // padding-inline — 노치 기기 가로 모드 대응. viewport-fit=cover 라 없으면 내용이 깎임
    <div className="min-h-dvh [padding-inline:env(safe-area-inset-left)_env(safe-area-inset-right)]">
      <Header />

      {/*
        본문 폭 76rem. 한 줄을 늘리는 게 아니라 그 안에서 열을 나눈다는 뜻 — 글이 담기는 열은 700px 이하
        아래 여백은 넉넉히. 마지막 요소가 바닥에 붙으면 "끝"이 아니라 "잘림"으로 읽힘
      */}
      <main className="mx-auto w-full max-w-[76rem] px-5 pt-10 pb-[calc(7rem+env(safe-area-inset-bottom))] sm:px-8 sm:pt-20 sm:pb-40 lg:px-10">
        {/*
          key 가 바뀌면 React 가 div 를 새로 만들어 전환 연출이 재실행. 라우터에 전환 기능이 없어 이게 가장 단순
          페이드만, 그것도 짧게 — 매번 보는 것이라 길면 화면 뜨기를 기다리는 시간이 됨
        */}
        <div key={location.pathname} className="animate-fade">
          <Routes>
            {/* 한 주소가 세 얼굴을 갖는다: 비로그인 → 소개, 로그인+0대 → 등록 권유,
                로그인+차량 있음 → 통계. 갈림은 HomePage 안에 있다. */}
            <Route path="/" element={<HomePage />} />

            {/* path 없는 라우트 = 자식들을 감싸는 울타리. 아래 ProtectedRoute와 같은 구조다. */}
            <Route element={<AuthLayout />}>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/signup" element={<SignUpPage />} />
            </Route>

            <Route element={<ProtectedRoute />}>
              <Route path="/vehicles" element={<VehicleListPage />} />
              {/* 'new' 가 ':vehicleId' 보다 먼저 매칭 — 라우터가 구체적인 경로를 우선 */}
              <Route path="/vehicles/new" element={<VehicleNewPage />} />
              <Route path="/vehicles/:vehicleId" element={<VehicleDetailPage />} />
              <Route path="/me" element={<ProfilePage />} />
            </Route>

            {/* 404 는 소개 화면으로. /vehicles 면 비로그인 사용자가 거기서 또 튕김 */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </div>
      </main>
    </div>
  )
}

export default App
