import { ChevronLeft } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router'

/**
 * 앱 화면 한 장의 껍데기. 뒤로가기 · 머리말 · 본문 사이의 간격을 여기서만 정한다.
 *
 * 원래는 `PageHeader`(머리말만)였고 바깥 래퍼는 화면마다 직접 썼는데, 그 사이에
 * `gap-8`·`gap-10`·`gap-12` 세 값이 생겼고 차량 상세는 아예 머리말을 손으로 다시 그리다가
 * **`sm:text-[2rem]` 을 빠뜨려 넓은 화면에서만 제목이 혼자 작았다.**
 * 간격과 머리말을 한 컴포넌트가 같이 쥐고 있어야 이런 드리프트가 안 생긴다.
 *
 * 규칙:
 * - **eyebrow** = "지금 보는 것이 어디에 속하는가". `Garage`(차량), `Account`(계정),
 *   차량 상세는 번호판. 로그인·회원가입은 아직 아무 데도 속하지 않으므로 **비운다.**
 * - **back** = 목록에서 파고 들어간 화면에만. 차량 등록·차량 상세 두 곳이다.
 * - **action** = 이 화면에서 새로 만드는 동작 하나. 목록의 "차량 등록"이 유일하다.
 *
 * children 은 이 컴포넌트의 flex 자식이 되어 자동으로 같은 간격(gap-10)으로 벌어진다.
 * 더 촘촘히 붙여야 하는 덩어리는 화면 쪽에서 한 번 더 묶는다(목록의 격자+페이지 이동처럼).
 */
export function Page({
  back,
  eyebrow,
  title,
  description,
  action,
  children,
}: {
  back?: { to: string; label: string }
  eyebrow?: string
  title: string
  description?: string
  action?: ReactNode
  children?: ReactNode
}) {
  return (
    <div className="flex flex-col gap-10">
      <div className="flex flex-col gap-5">
        {back !== undefined && (
          <Link
            to={back.to}
            className="-ml-1 inline-flex w-fit items-center gap-1 text-[0.8125rem] text-muted-foreground transition-opacity duration-200 ease-apple hover:opacity-70"
          >
            <ChevronLeft className="size-3.5" aria-hidden="true" />
            {back.label}
          </Link>
        )}

        <header className="flex items-end justify-between gap-6">
          <div className="flex flex-col gap-2">
            {/*
              eyebrow 는 제목 위의 작은 분류 한 줄이다. 자간을 아주 넓게(0.16em) 벌려
              제목의 좁은 자간(-0.03em)과 대비시키는 게 이 조합의 핵심 — 두 줄이 크기가 아니라
              '질감'으로 구분된다.
            */}
            {eyebrow !== undefined && (
              <p className="text-[0.6875rem] font-medium tracking-[0.16em] text-muted-foreground uppercase">
                {eyebrow}
              </p>
            )}

            <h1 className="text-[1.75rem] leading-[1.15] font-semibold tracking-[-0.03em] text-strong sm:text-[2rem]">
              {title}
            </h1>

            {description !== undefined && (
              <p className="text-sm text-muted-foreground">{description}</p>
            )}
          </div>

          {action !== undefined && <div className="shrink-0 pb-1.5">{action}</div>}
        </header>
      </div>

      {children}
    </div>
  )
}

/**
 * 폼 맨 아래 버튼 줄. 주 동작이 먼저, 취소는 ghost 로 오른쪽.
 *
 * 이 줄도 화면마다 `mt-1 self-start` / `mt-1 flex gap-2` / `flex gap-2` 로 제각각이었다.
 * 취소 버튼을 채워진 버튼으로 두지 않는 이유는 둘 중 무엇이 기본 동작인지 흐려지기 때문.
 */
export function FormActions({ children }: { children: ReactNode }) {
  return <div className="mt-2 flex gap-2">{children}</div>
}
