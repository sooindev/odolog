import { Navigate, Outlet, useLocation } from 'react-router'

import { LoadingText } from '@/shared/ui/feedback/state'
import { useAuth } from '@/features/auth/context/definition/AuthContext'

/** 로그인한 사용자만 통과. 아니면 /login 으로 */
export function ProtectedRoute() {
  const { user, loading } = useAuth()
  const location = useLocation()

  // 세션 복구 전에 판단하면 로그인 상태에서도 로그인 화면이 한 번 깜빡임
  if (loading) {
    return <LoadingText className="justify-center py-24" />
  }

  if (user === null) {
    // replace — 뒤로가기로 돌아와 다시 튕기는 반복 방지
    // state — 로그인 후 원래 가려던 곳으로 복귀용
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <Outlet />
}
