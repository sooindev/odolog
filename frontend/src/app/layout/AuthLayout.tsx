import { ArrowRight } from 'lucide-react'
import { Link, Outlet } from 'react-router'

import { GaugeMark } from '@/shared/ui/brand/mark'

// 로그인·회원가입 공용 2단 레이아웃
// 왼쪽 패널은 lg 미만에서 숨김
export function AuthLayout() {
  return (
    <div className="grid items-center gap-12 lg:min-h-[32rem] lg:grid-cols-2 lg:gap-16">
      {/* 두 단 사이 세로 괘선 */}
      <div className="hidden flex-col gap-10 lg:flex lg:border-r lg:border-border lg:pr-16">
        <GaugeMark className="size-9 text-strong" />

        {/*
          태그라인이라 p. h2 면 목차 순서가 h2 → h1, 폭에 따라 목차 변동
          줄바꿈 직접 지정
        */}
        <p className="text-[clamp(2rem,1.4rem+1.8vw,2.75rem)] leading-[1.06] font-semibold tracking-[-0.04em] text-strong">
          마지막 정비가 언제였는지,
          <br />
          다음은 언제인지.
        </p>

        {/* 기능 설명은 랜딩 담당 */}
        <Link
          to="/"
          className="inline-flex w-fit items-center gap-1.5 text-caption text-muted-foreground transition-opacity duration-200 ease-apple hover:opacity-70"
        >
          오도로그가 하는 일
          <ArrowRight className="size-3.5" aria-hidden="true" />
        </Link>
      </div>

      {/* 폼 폭 23rem 이하 */}
      <div className="mx-auto w-full max-w-[23rem] lg:mx-0">
        <Outlet />
      </div>
    </div>
  )
}
