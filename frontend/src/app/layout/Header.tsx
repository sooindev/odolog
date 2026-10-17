import { WifiOff } from 'lucide-react'
import { Link, useLocation, useNavigate } from 'react-router'

import { useAuth } from '@/features/auth/context/AuthContext'
import { useI18n } from '@/shared/i18n/I18nContext'
import { useOnline } from '@/shared/lib/hooks/useOnline'
import { ThemeToggle } from '@/shared/theme/ThemeToggle'
import { Button } from '@/shared/ui/base/button'
import { GaugeMark } from '@/shared/ui/mark'

/** 상단 고정 바. 불투명 바닥색 + 아래 1px 괘선 */
export function Header() {
  const { user, logout } = useAuth()
  const { t } = useI18n()
  const navigate = useNavigate()
  const location = useLocation()
  const online = useOnline()

  // 로그인·가입 화면에서는 로그인 버튼 숨김
  const onAuthPage = location.pathname === '/login' || location.pathname === '/signup'

  // 서버 세션을 못 끊었으면 로그인 상태 그대로 두고 알림. 공용 PC 에서 로그아웃된 줄 알고 떠나지 않게
  async function handleLogout() {
    try {
      await logout()
    } catch {
      window.alert(t.header.logoutFailed)
      return
    }
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
          <span className="text-body font-semibold tracking-[-0.03em] text-strong">{t.app.name}</span>
        </Link>

        <div className="flex min-w-0 items-center gap-1.5">
          {/*
            좁은 화면에서는 테마 토글 숨김(프로필에도 있음)
            로그인 전에도 표시
          */}
          <ThemeToggle className="hidden shrink-0 sm:flex" />

          {user === null && !onAuthPage && (
            <Button variant="ghost" size="sm" className="shrink-0" render={<Link to="/login" />}>
              {t.header.login}
            </Button>
          )}

          {user !== null && (
            <>
              {/* 닉네임 폭 제한. 로그아웃 버튼 밀림 방지 */}
              <Button variant="ghost" size="sm" className="max-w-24" render={<Link to="/me" />}>
                <span className="min-w-0 truncate">{user.nickname}</span>
              </Button>
              <Button variant="ghost" size="sm" className="shrink-0" onClick={handleLogout}>
                {t.header.logout}
              </Button>
            </>
          )}
        </div>
      </div>

      {/*
        오프라인 안내. 조회가 실패하지 않고 멈추므로 버튼이 안 먹는 것처럼 보이지 않게
        상자는 늘 두고 글만 바꿈. 처음부터 있던 status 라야 읽어 줌
      */}
      <div role="status">
        {!online && (
          <div className="border-t border-border bg-sunken">
            <p className="mx-auto flex max-w-[76rem] items-center gap-2 px-5 py-2.5 text-caption text-foreground sm:px-8 lg:px-10">
              <WifiOff className="size-3.5 shrink-0 text-muted-foreground" aria-hidden="true" />
              {t.header.offline}
            </p>
          </div>
        )}
      </div>
    </header>
  )
}
