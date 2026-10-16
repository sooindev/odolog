import { Navigate, Outlet, useLocation } from 'react-router'

import { LoadingText } from '@/shared/ui/state'
import { useAuth } from '@/features/auth/context/AuthContext'

/** 로그인 사용자만 통과. 아니면 /login */
export function ProtectedRoute() {
  const { user, loading, sessionEnded, endedEmail } = useAuth()
  const location = useLocation()

  // 세션 복구 전 판단 금지. 로그인 화면 깜빡임 방지
  if (loading) {
    return <LoadingText className="justify-center py-24" />
  }

  if (user === null) {
    // replace: 뒤로가기 반복 방지
    // state.from: 로그인 후 복귀용. 쿼리·해시까지 보존
    // 서버가 세션을 끊었으면 이유 안내. 말없이 로그인 화면이면 버그로 보임
    // owner: 그 주소가 누구의 것이었는지. 다른 계정으로 로그인하면 from 대신 목록으로
    const from = `${location.pathname}${location.search}${location.hash}`
    const notice = sessionEnded ? 'sessionEnded' : undefined
    const owner = sessionEnded ? (endedEmail ?? undefined) : undefined

    return <Navigate to="/login" replace state={{ from, notice, owner }} />
  }

  return <Outlet />
}
