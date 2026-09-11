import { Link, useNavigate } from 'react-router'

import { useAuth } from '@/features/auth/context/AuthContext'
import { ThemeToggle } from '@/shared/theme/ThemeToggle'
import { Button } from '@/shared/ui/button'

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

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="sticky top-0 z-50 border-b border-border bg-background/60 backdrop-blur-[20px] backdrop-saturate-150">
      <div className="mx-auto flex h-16 max-w-[44rem] items-center justify-between gap-3 px-6 sm:px-8">
        <Link
          to="/vehicles"
          className="flex shrink-0 items-center gap-2.5 transition-opacity duration-200 ease-apple hover:opacity-70"
        >
          {/* 파비콘과 같은 계기판 바늘 도형. 로고 옆 작은 마크 하나가 워드마크만 있는 것보다 훨씬 오래 기억된다. */}
          <svg viewBox="0 0 32 32" className="size-[18px]" aria-hidden="true">
            <path
              d="M8 20a8 8 0 1 1 16 0"
              fill="none"
              stroke="currentColor"
              strokeOpacity="0.3"
              strokeWidth="2.5"
              strokeLinecap="round"
            />
            <path
              d="M16 20 21 13"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.5"
              strokeLinecap="round"
            />
          </svg>
          <span className="text-[0.9375rem] font-semibold tracking-[-0.03em] text-strong">
            오도로그
          </span>
        </Link>

        <div className="flex min-w-0 items-center gap-1.5">
          {/* 로그인 전에도 보여준다. 로그인 화면을 눈부신 흰 화면으로 마주해야 할 이유가 없다. */}
          <ThemeToggle className="shrink-0" />

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
