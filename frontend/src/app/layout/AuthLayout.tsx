import { ArrowRight } from 'lucide-react'
import { Link, Outlet } from 'react-router'

import { GaugeMark } from '@/shared/ui/brand/mark'

/*
 * 로그인·회원가입 공용 2단 레이아웃. 라우트를 감싸는 껍데기라 ProtectedRoute 와 같은 자리
 * 왼쪽 패널은 lg 미만에서 미렌더 — 좁은 화면에서 로그인 전에 읽을 글을 줄 이유가 없음
 */
export function AuthLayout() {
  return (
    <div className="grid items-center gap-12 lg:min-h-[32rem] lg:grid-cols-2 lg:gap-16">
      {/* 두 단 사이 세로 괘선. 여백만으로는 별개의 열로 안 잡힘 */}
      <div className="hidden flex-col gap-10 lg:flex lg:border-r lg:border-border lg:pr-16">
        <GaugeMark className="size-9 text-strong" />

        {/* 줄바꿈 직접 지정. 브라우저에 맡기면 창 크기마다 끊기는 자리가 달라짐 */}
        <h2 className="text-[clamp(2rem,1.4rem+1.8vw,2.75rem)] leading-[1.06] font-semibold tracking-[-0.04em] text-strong">
          마지막 정비가 언제였는지,
          <br />
          다음은 언제인지.
        </h2>

        {/* 기능 설명은 랜딩 담당. 같은 목록이 두 곳이면 한쪽만 고치게 됨 */}
        <Link
          to="/"
          className="inline-flex w-fit items-center gap-1.5 text-caption text-muted-foreground transition-opacity duration-200 ease-apple hover:opacity-70"
        >
          오도로그가 하는 일
          <ArrowRight className="size-3.5" aria-hidden="true" />
        </Link>
      </div>

      {/* 폼은 넓은 화면에서도 23rem 이하. 입력창이 길어져서 좋을 일이 없음 */}
      <div className="mx-auto w-full max-w-[23rem] lg:mx-0">
        <Outlet />
      </div>
    </div>
  )
}
