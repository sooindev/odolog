import { Navigate, Outlet, useLocation } from 'react-router'

import { useAuth } from '@/auth/AuthContext'

/** 로그인한 사용자만 통과시키는 라우트. 아니면 /login 으로 보낸다. */
export function ProtectedRoute() {
  const { user, loading } = useAuth()
  const location = useLocation()

  // 세션 복구가 끝나기 전에 판단하면, 로그인돼 있는데도 로그인 화면이 한 번 깜빡인다.
  if (loading) {
    return <div className="text-muted-foreground p-8 text-sm">불러오는 중…</div>
  }

  if (user === null) {
    // replace: 뒤로가기로 이 보호 페이지에 되돌아와 다시 튕기는 반복을 막는다.
    // state: 로그인 후 원래 가려던 곳으로 돌려보내기 위해 기억해 둔다.
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <Outlet />
}
