import { ArrowRight } from 'lucide-react'
import { Link, Outlet } from 'react-router'

import { GaugeMark } from '@/shared/ui/brand/mark'

/*
 * 로그인·회원가입이 공유하는 2단 레이아웃.
 *
 * `app/` 에 두는 이유는 ProtectedRoute 와 같다 — 라우트를 감싸는 울타리이고,
 * 여러 화면이 함께 쓰는 껍데기다. features/auth 안에 넣으면 파일 하나짜리 폴더가 생긴다.
 *
 * 왼쪽 패널은 lg 미만에서 아예 렌더되지 않는다(hidden lg:flex). 좁은 화면에서
 * 로그인 전에 읽어야 할 글을 먼저 보여줄 이유가 없다.
 */
export function AuthLayout() {
  return (
    <div className="grid items-center gap-12 lg:min-h-[30rem] lg:grid-cols-2 lg:gap-20">
      <div className="hidden flex-col gap-9 lg:flex">
        <GaugeMark className="size-9 text-strong" />

        {/* 문장을 직접 끊는다(<br/>). 줄바꿈을 브라우저에 맡기면 창 크기에 따라
            "다음은"에서 끊기는 등 리듬이 매번 달라진다. 큰 글씨일수록 티가 난다. */}
        <h2 className="text-[2.25rem] leading-[1.12] font-semibold tracking-[-0.035em] text-strong">
          마지막 정비가 언제였는지,
          <br />
          다음은 언제인지.
        </h2>

        {/* 기능 설명은 랜딩(/)이 맡는다. 같은 목록을 두 곳에 두면 한쪽만 고치게 된다.
            여기서는 링크 한 줄로 넘긴다 — 로그인하러 온 사람에게 읽을거리를 더 주지 않는다. */}
        <Link
          to="/"
          className="inline-flex w-fit items-center gap-1.5 text-[0.8125rem] text-muted-foreground transition-opacity duration-200 ease-apple hover:opacity-70"
        >
          오도로그가 하는 일
          <ArrowRight className="size-3.5" aria-hidden="true" />
        </Link>
      </div>

      {/* 폼 자체는 넓은 화면에서도 22rem 을 넘기지 않는다. 입력창이 길어져서 좋을 일은 없다. */}
      <div className="mx-auto w-full max-w-[22rem] lg:mx-0">
        <Outlet />
      </div>
    </div>
  )
}
