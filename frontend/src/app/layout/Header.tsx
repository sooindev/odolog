import { Link, useLocation, useNavigate } from 'react-router'

import { useAuth } from '@/features/auth/context/definition/AuthContext'
import { ThemeToggle } from '@/shared/theme/toggle/ThemeToggle'
import { Button } from '@/shared/ui/base/button'
import { GaugeMark } from '@/shared/ui/brand/mark'

/**
 * 화면 맨 위에 붙어 따라다니는 바. 불투명한 바닥색 + 아래쪽 1px 괘선뿐이다.
 * 반투명하게 두면 스크롤할 때 바 위의 글자와 아래 글자가 한순간 겹쳐 읽힌다.
 */
export function Header() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  // 로그인 화면에서 "로그인" 버튼을 또 보여주면 지금 있는 곳을 가리키는 버튼이 된다.
  const onAuthPage = location.pathname === '/login' || location.pathname === '/signup'

  // logout() 은 실패해도 던지지 않는다(AuthProvider 가 상태만 비운다).
  // 그래서 navigate 가 항상 실행된다. 여기에 try/catch 를 또 두지 않는 이유.
  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="sticky top-0 z-50 border-b border-border bg-background">
      <div className="mx-auto flex h-14 max-w-[76rem] items-center justify-between gap-3 px-5 sm:h-[4.5rem] sm:px-8 lg:px-10">
        {/* 로그인 여부와 상관없이 항상 홈으로. 같은 버튼이 상황에 따라 다른 곳으로 가면
            누를 때마다 어디로 갈지 예측해야 한다. */}
        <Link
          to="/"
          className="flex shrink-0 items-center gap-2.5 transition-opacity duration-200 ease-apple hover:opacity-70"
        >
          <GaugeMark />
          <span className="text-[0.9375rem] font-semibold tracking-[-0.03em] text-strong">오도로그</span>
        </Link>

        <div className="flex min-w-0 items-center gap-1.5">
          {/*
            좁은 화면에서는 숨긴다. 375px 에서 로고·토글·닉네임·로그아웃을 모두 넣으면
            폭이 60px 모자라 닉네임이 말줄임만 남는다. 화면 모드는 프로필의 '화면' 구역에도
            있으므로 기능이 사라지는 것은 아니다.
            로그인 전에도 보여준다. 로그인 화면을 흰 화면으로 마주할 이유가 없다.
          */}
          <ThemeToggle className="hidden shrink-0 sm:flex" />

          {user === null && !onAuthPage && (
            <Button variant="ghost" size="sm" className="shrink-0" render={<Link to="/login" />}>
              로그인
            </Button>
          )}

          {user !== null && (
            <>
              {/* 닉네임은 30자까지 가능하다. 폭을 안 막으면 로그아웃 버튼이 화면 밖으로 밀린다. */}
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
