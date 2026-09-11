import { Navigate, Route, Routes, useLocation } from 'react-router'

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
        본문 폭을 44rem(704px)에서 끊는다. 화면이 넓다고 본문까지 넓히면 한 줄이 길어지고,
        줄이 길수록 다음 줄 첫 글자를 찾는 눈의 왕복 거리가 늘어 읽기가 급격히 피곤해진다.
        남는 좌우 공간은 낭비가 아니라 본문을 화면 가운데로 고정해 주는 장치다.

        pb-32: 아래에 넉넉한 공백을 둔다. 마지막 요소가 화면 바닥에 붙으면 "여기서 끝"이
        아니라 "잘렸다"로 읽힌다.
      */}
      <main className="mx-auto w-full max-w-[44rem] px-6 pt-14 pb-32 sm:px-8 sm:pt-20">
        {/*
          key에 현재 경로를 준다. 경로가 바뀌면 React가 이 div를 버리고 새로 만들기 때문에
          animate-rise(6px 아래에서 떠오르며 나타나기)가 페이지를 옮길 때마다 다시 실행된다.
          라우터에는 전환 애니메이션 기능이 없어서, key 를 이용한 재생성이 가장 단순한 방법이다.
        */}
        <div key={location.pathname} className="animate-rise">
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
        </div>
      </main>
    </div>
  )
}

export default App
