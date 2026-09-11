import { Link, useLocation, useNavigate } from 'react-router'

import { useAuth } from '@/features/auth/context/AuthContext'
import { ThemeToggle } from '@/shared/theme/ThemeToggle'
import { Button } from '@/shared/ui/button'
import { GaugeMark } from '@/shared/ui/mark'

/**
 * 화면 맨 위에 붙어 따라다니는 유리 바.
 *
 * 불투명하게 칠하지 않고 배경 60% + backdrop-blur(20px) 로 둔다.
 * 스크롤하면 아래 내용이 바 뒤로 흐릿하게 비쳐 지나가서, 바가 화면 위에 떠 있는
 * 다른 층이라는 게 드러난다. 불투명하면 그냥 잘린 것처럼 보인다.
 *
 * backdrop-saturate-150 은 애플이 실제로 쓰는 보정이다. blur만 걸면 뒤 색이
 * 물 빠진 듯 탁해지는데, 채도를 올려 그걸 되돌린다.
 */
export function Header() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  // 로그인 화면에서 "로그인" 버튼을 또 보여주면 지금 있는 곳을 가리키는 버튼이 된다.
  const onAuthPage = location.pathname === '/login' || location.pathname === '/signup'

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="sticky top-0 z-50 border-b border-border bg-background/60 backdrop-blur-[20px] backdrop-saturate-150">
      <div className="mx-auto flex h-16 max-w-[76rem] items-center justify-between gap-3 px-6 sm:px-8 lg:px-10">
        {/* 로고가 가리키는 곳이 로그인 여부에 따라 다르다. 로그인한 사람에게 홈은 소개 화면이
            아니라 자기 차량 목록이다. */}
        <Link
          to={user === null ? '/' : '/vehicles'}
          className="flex shrink-0 items-center gap-2.5 transition-opacity duration-200 ease-apple hover:opacity-70"
        >
          <GaugeMark />
          <span className="text-[0.9375rem] font-semibold tracking-[-0.03em] text-strong">
            오도로그
          </span>
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
