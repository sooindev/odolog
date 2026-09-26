import { Link, useLocation, useNavigate } from 'react-router'

import { useAuth } from '@/features/auth/context/definition/AuthContext'
import { ThemeToggle } from '@/shared/theme/toggle/ThemeToggle'
import { Button } from '@/shared/ui/base/button'
import { GaugeMark } from '@/shared/ui/brand/mark'

/** 상단 고정 바. 불투명 바닥색 + 아래 1px 괘선 */
export function Header() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  // 로그인·가입 화면에서는 로그인 버튼 숨김
  const onAuthPage = location.pathname === '/login' || location.pathname === '/signup'

  // logout() 은 실패해도 던지지 않아 navigate 항상 실행
  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="sticky top-0 z-50 border-b border-border bg-background">
      <div className="mx-auto flex h-14 max-w-[76rem] items-center justify-between gap-3 px-5 sm:h-[4.5rem] sm:px-8 lg:px-10">
        {/* 로고는 로그인 여부와 무관하게 항상 홈 */}
        <Link
          to="/"
          className="flex shrink-0 items-center gap-2.5 transition-opacity duration-200 ease-apple hover:opacity-70"
        >
          <GaugeMark />
          <span className="text-body font-semibold tracking-[-0.03em] text-strong">오도로그</span>
        </Link>

        <div className="flex min-w-0 items-center gap-1.5">
          {/*
            좁은 화면에서는 테마 토글 숨김(프로필에도 있음)
            로그인 전에도 표시
          */}
          <ThemeToggle className="hidden shrink-0 sm:flex" />

          {user === null && !onAuthPage && (
            <Button variant="ghost" size="sm" className="shrink-0" render={<Link to="/login" />}>
              로그인
            </Button>
          )}

          {user !== null && (
            <>
              {/* 닉네임 폭 제한. 로그아웃 버튼 밀림 방지 */}
              <Button variant="ghost" size="sm" className="max-w-24" render={<Link to="/me" />}>
                <span className="min-w-0 truncate">{user.nickname}</span>
              </Button>
              <Button variant="ghost" size="sm" className="shrink-0" onClick={handleLogout}>
                로그아웃
              </Button>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
