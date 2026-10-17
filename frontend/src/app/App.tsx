import { Suspense, lazy, useEffect } from 'react'
import { Navigate, Route, Routes, useLocation, useNavigationType, useParams } from 'react-router'

import { AuthLayout } from '@/app/layout/AuthLayout'
import { Footer } from '@/app/layout/Footer'
import { HomePage } from '@/app/HomePage'
import { ProtectedRoute } from '@/app/ProtectedRoute'
import { ScreenErrorBoundary } from '@/app/ScreenErrorBoundary'
import { Header } from '@/app/layout/Header'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { LoadingText } from '@/shared/ui/state'

// 첫 화면(소개·홈·로그인)만 처음 번들에. 나머지 화면은 들어갈 때 받음. 한 덩어리면 500kB 를 넘음
// 화면 파일은 이름으로 내보내서 lazy 가 원하는 default 로 바꿔 줌
const LegalPage = lazy(() => import('@/app/LegalPage').then((m) => ({ default: m.LegalPage })))
const SignUpPage = lazy(() => import('@/features/auth/pages/SignUpPage').then((m) => ({ default: m.SignUpPage })))
const ForgotPasswordPage = lazy(() =>
  import('@/features/auth/pages/ForgotPasswordPage').then((m) => ({ default: m.ForgotPasswordPage })),
)
const ResetPasswordPage = lazy(() =>
  import('@/features/auth/pages/ResetPasswordPage').then((m) => ({ default: m.ResetPasswordPage })),
)
const ProfilePage = lazy(() => import('@/features/account/ProfilePage').then((m) => ({ default: m.ProfilePage })))
const VehicleListPage = lazy(() =>
  import('@/features/vehicles/pages/VehicleListPage').then((m) => ({ default: m.VehicleListPage })),
)
const VehicleNewPage = lazy(() =>
  import('@/features/vehicles/pages/VehicleNewPage').then((m) => ({ default: m.VehicleNewPage })),
)
const VehicleDetailPage = lazy(() =>
  import('@/features/vehicles/pages/VehicleDetailPage').then((m) => ({ default: m.VehicleDetailPage })),
)

/**
 * 주소의 차량이 바뀌면 상세 화면을 새로. 정비 목록 쪽·필터·409 문구·동작 실패 문구가 다음 차량으로 넘어가지 않게
 * 아래 페이드용 key 에 기대지 않음. 그 key 를 지우면 같은 문제가 조용히 되살아나서
 */
function VehicleDetailByKey() {
  const { vehicleId } = useParams()
  return <VehicleDetailPage key={vehicleId} />
}

function App() {
  const location = useLocation()
  const navigationType = useNavigationType()

  // 새 화면은 맨 위에서. 뒤로·앞으로(POP)는 브라우저가 기억한 위치 그대로
  useEffect(() => {
    if (navigationType !== 'POP') {
      window.scrollTo(0, 0)
    }
  }, [location.pathname, navigationType])

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
          {/* 화면 조각을 못 받으면 헤더·푸터는 두고 본문만 안내로. 위 key 덕에 주소가 바뀌면 다시 시도 */}
          <ScreenErrorBoundary>
            {/* 화면 조각을 받는 동안. 보호 라우트의 세션 복구 대기와 같은 모양 */}
            <Suspense fallback={<LoadingText className="justify-center py-24" />}>
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
                  <Route path="/vehicles/:vehicleId" element={<VehicleDetailByKey />} />
                  <Route path="/me" element={<ProfilePage />} />
                </Route>

                {/* 없는 주소는 소개 화면으로 */}
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </Suspense>
          </ScreenErrorBoundary>
        </div>
      </main>

      {/* 노치 아래 여백은 푸터가 담당 */}
      <Footer />
    </div>
  )
}

export default App
