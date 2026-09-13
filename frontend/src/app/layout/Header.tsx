import { Link, useLocation, useNavigate } from 'react-router'

import { useAuth } from '@/features/auth/context/definition/AuthContext'
import { ThemeToggle } from '@/shared/theme/toggle/ThemeToggle'
import { Button } from '@/shared/ui/base/button'
import { GaugeMark } from '@/shared/ui/brand/mark'

/**
 * 화면 맨 위에 붙어 따라다니는 바.
 *
 * 전에는 배경 60% + backdrop-blur(20px) 짜리 '유리'였다. 스크롤할 때 내용이 뒤로
 * 흐릿하게 지나가는 연출이었는데 걷어냈다 — 바 뒤로 글자가 비쳐 지나가면
 * **바 위의 글자와 아래 글자가 한순간 겹쳐 읽힌다.**
 *
 * 지금은 불투명한 바닥색 + 아래쪽 1px 괘선뿐이다. 페이지의 다른 모든 괘선과
 * 같은 두께·같은 색이라, 헤더가 '떠 있는 다른 층'이 아니라 **지면의 첫 칸**으로 읽힌다.
 */
export function Header() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  // 로그인 화면에서 "로그인" 버튼을 또 보여주면 지금 있는 곳을 가리키는 버튼이 된다.
  const onAuthPage = location.pathname === '/login' || location.pathname === '/signup'

  // logout() 은 서버 요청이 실패해도 던지지 않는다(AuthProvider 가 삼키고 상태만 비운다).
  // 그래서 여기서는 navigate 가 항상 실행된다 — 여기에 try/catch 를 또 두지 않는 이유다.
  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="sticky top-0 z-50 border-b border-border bg-background">
      <div className="mx-auto flex h-[4.5rem] max-w-[76rem] items-center justify-between gap-3 px-6 sm:px-8 lg:px-10">
        {/* 로그인 여부와 상관없이 항상 홈(소개 화면)으로 간다.
            로그인했다고 목적지가 달라지면, 같은 버튼이 어디로 갈지 매번 예측해야 한다.
            로그인한 사람이 차량 목록으로 가는 길은 홈의 "내 차량 보기" 버튼이다. */}
        <Link
          to="/"
          className="flex shrink-0 items-center gap-2.5 transition-opacity duration-200 ease-apple hover:opacity-70"
        >
          <GaugeMark />
          <span className="text-[0.9375rem] font-semibold tracking-[-0.03em] text-strong">오도로그</span>
        </Link>

        <div className="flex min-w-0 items-center gap-1.5">
          {/* 로그인 전에도 보여준다. 로그인 화면을 눈부신 흰 화면으로 마주해야 할 이유가 없다. */}
          <ThemeToggle className="shrink-0" />

          {user === null && !onAuthPage && (
            <Button variant="ghost" size="sm" className="shrink-0" render={<Link to="/login" />}>
              로그인
            </Button>
          )}

          {user !== null && (
            <>
              {/* 닉네임은 최대 30자까지 가능하다. 폭을 안 막으면 긴 닉네임 하나가
                  헤더를 밀어내 로그아웃 버튼이 화면 밖으로 나간다. */}
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
