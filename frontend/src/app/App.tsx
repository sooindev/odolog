import { Navigate, Route, Routes, useLocation } from 'react-router'

import { AuthLayout } from '@/app/AuthLayout'
import { LandingPage } from '@/app/LandingPage'
import { ProtectedRoute } from '@/app/ProtectedRoute'
import { Header } from '@/app/Header'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { ProfilePage } from '@/features/auth/pages/ProfilePage'
import { SignUpPage } from '@/features/auth/pages/SignUpPage'
import { VehicleDetailPage } from '@/features/vehicles/pages/VehicleDetailPage'
import { VehicleListPage } from '@/features/vehicles/pages/VehicleListPage'
import { VehicleNewPage } from '@/features/vehicles/pages/VehicleNewPage'

function App() {
  const location = useLocation()

  return (
    // min-h-dvh: 모바일 브라우저의 주소창이 접히고 펴질 때 높이가 같이 변하는 단위.
    // min-h-screen(100vh)은 주소창 높이를 무시해서 화면 아래가 잘린다.
    <div className="min-h-dvh">
      <Header />

      {/*
        본문 폭 76rem(1216px). 한 줄을 1216px까지 늘리겠다는 뜻이 아니라,
        그 폭 안에서 **열을 나눠 쓰겠다**는 뜻이다. 실제로 글이 담기는 열은 어디서도
        700px를 넘지 않는다 — 줄이 길수록 다음 줄 첫 글자를 찾는 눈의 왕복 거리가 늘어
        읽기가 급격히 피곤해지기 때문이다.

        pb-32: 아래에 넉넉한 공백을 둔다. 마지막 요소가 화면 바닥에 붙으면 "여기서 끝"이
        아니라 "잘렸다"로 읽힌다.
      */}
      <main className="mx-auto w-full max-w-[76rem] px-6 pt-14 pb-32 sm:px-8 sm:pt-16 lg:px-10">
        {/*
          key에 현재 경로를 준다. 경로가 바뀌면 React가 이 div를 버리고 새로 만들기 때문에
          animate-rise(6px 아래에서 떠오르며 나타나기)가 페이지를 옮길 때마다 다시 실행된다.
          라우터에는 전환 애니메이션 기능이 없어서, key 를 이용한 재생성이 가장 단순한 방법이다.
        */}
        <div key={location.pathname} className="animate-rise">
          <Routes>
            {/* 로그인 전에 처음 만나는 화면. 로그인해도 막지 않는다 — 다시 볼 수 있어야 한다. */}
            <Route path="/" element={<LandingPage />} />

            {/* path 없는 라우트 = 자식들을 감싸는 울타리. 아래 ProtectedRoute와 같은 구조다. */}
            <Route element={<AuthLayout />}>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/signup" element={<SignUpPage />} />
            </Route>

            <Route element={<ProtectedRoute />}>
              <Route path="/vehicles" element={<VehicleListPage />} />
              {/* 'new'(리터럴)가 ':vehicleId'(변수)보다 먼저 매칭된다 — 라우터가 구체적인 경로를 우선한다 */}
              <Route path="/vehicles/new" element={<VehicleNewPage />} />
              <Route path="/vehicles/:vehicleId" element={<VehicleDetailPage />} />
              <Route path="/me" element={<ProfilePage />} />
            </Route>

            {/* 어디에도 안 맞는 주소는 소개 화면으로. 예전에는 /vehicles 로 보냈는데,
                로그인 안 한 사람은 거기서 다시 /login 으로 튕겨 두 번 이동했다. */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </div>
      </main>
    </div>
  )
}

export default App
