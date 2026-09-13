import { ArrowRight } from 'lucide-react'
import { Link, Outlet } from 'react-router'

import { GaugeMark } from '@/shared/ui/brand/mark'

/*
 * 로그인·회원가입이 공유하는 2단 레이아웃. 라우트를 감싸는 껍데기라 ProtectedRoute 와
 * 같은 자리(app/)에 둔다.
 * 왼쪽 패널은 lg 미만에서 렌더되지 않는다. 좁은 화면에서 로그인 전에 읽을 글을 줄 이유가 없다.
 */
export function AuthLayout() {
  return (
    <div className="grid items-center gap-12 lg:min-h-[32rem] lg:grid-cols-2 lg:gap-16">
      {/* 두 단 사이의 세로 괘선. 여백만으로는 별개의 열이라는 게 안 잡힌다. */}
      <div className="hidden flex-col gap-10 lg:flex lg:border-r lg:border-border lg:pr-16">
        <GaugeMark className="size-9 text-strong" />

        {/* 줄바꿈을 직접 넣는다. 브라우저에 맡기면 창 크기마다 끊기는 자리가 달라진다. */}
        <h2 className="text-[clamp(2rem,1.4rem+1.8vw,2.75rem)] leading-[1.06] font-semibold tracking-[-0.04em] text-strong">
          마지막 정비가 언제였는지,
          <br />
          다음은 언제인지.
        </h2>

        {/* 기능 설명은 랜딩이 맡는다. 같은 목록을 두 곳에 두면 한쪽만 고치게 된다. */}
        <Link
          to="/"
          className="inline-flex w-fit items-center gap-1.5 text-[0.8125rem] text-muted-foreground transition-opacity duration-200 ease-apple hover:opacity-70"
        >
          오도로그가 하는 일
          <ArrowRight className="size-3.5" aria-hidden="true" />
        </Link>
      </div>

      {/* 폼은 넓은 화면에서도 23rem 을 넘기지 않는다. 입력창이 길어져서 좋을 일은 없다. */}
      <div className="mx-auto w-full max-w-[23rem] lg:mx-0">
        <Outlet />
      </div>
    </div>
  )
}
