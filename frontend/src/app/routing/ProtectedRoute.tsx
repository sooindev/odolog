import { Navigate, Outlet, useLocation } from 'react-router'

import { LoadingText } from '@/shared/ui/feedback/state'
import { useAuth } from '@/features/auth/context/definition/AuthContext'

/** 로그인 사용자만 통과. 아니면 /login */
export function ProtectedRoute() {
  const { user, loading } = useAuth()
  const location = useLocation()

  // 세션 복구 전 판단 금지. 로그인 화면 깜빡임 방지
  if (loading) {
    return <LoadingText className="justify-center py-24" />
  }

  if (user === null) {
    // replace: 뒤로가기 반복 방지
    // state.from: 로그인 후 복귀용. 쿼리·해시까지 보존
    const from = `${location.pathname}${location.search}${location.hash}`

    return <Navigate to="/login" replace state={{ from }} />
  }

  return <Outlet />
}
