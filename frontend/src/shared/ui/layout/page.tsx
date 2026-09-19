import { ChevronLeft } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router'

/**
 * 앱 화면 한 장의 껍데기. 머리말과 간격을 여기서만 결정
 * 화면마다 직접 그리면 어긋남 — 차량 상세가 제목 크기를 빠뜨린 전례
 *
 * eyebrow  지금 보는 것의 소속. 로그인·회원가입은 비움
 * back     목록에서 파고든 화면에만
 * action   이 화면에서 새로 만드는 동작 하나
 *
 * children 은 flex 자식이라 같은 간격. 더 촘촘히 붙일 덩어리는 화면 쪽에서 한 번 더 묶음
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
    <div className="flex flex-col gap-10 sm:gap-14">
      <div className="flex flex-col gap-6">
        {back !== undefined && (
          <Link
            to={back.to}
            className="-ml-1 inline-flex w-fit items-center gap-1 text-caption text-muted-foreground transition-opacity duration-200 ease-apple hover:opacity-70"
          >
            <ChevronLeft className="size-3.5" aria-hidden="true" />
            {back.label}
          </Link>
        )}

        <header className="flex flex-col gap-5 sm:gap-7">
          <div className="flex items-end justify-between gap-4 sm:gap-8">
            <div className="flex min-w-0 flex-col gap-3 sm:gap-4">
              {eyebrow !== undefined && (
                <p className="text-eyebrow text-muted-foreground uppercase">{eyebrow}</p>
              )}

              {/* text-balance — 둘째 줄에 한 단어만 남는 것 방지 */}
              <h1 className="text-title text-balance text-strong">{title}</h1>
            </div>

            {action !== undefined && <div className="shrink-0 pb-2">{action}</div>}
          </div>

          {/* 설명은 제목 폭을 따라가지 않고 46ch 에서 */}
          {description !== undefined && (
            <p className="max-w-[46ch] text-lede text-muted-foreground">{description}</p>
          )}
        </header>

        {/* 표제와 내용을 가르는 기준선. 이 화면의 유일한 등장 연출 */}
        <hr className="rule-draw border-t border-border" />
      </div>

      {children}
    </div>
  )
}

/**
 * 폼 맨 아래 버튼 줄. 주 동작 먼저, 취소는 ghost 로 오른쪽
 * 취소를 채워진 버튼으로 두면 무엇이 기본 동작인지 흐려짐
 */
export function FormActions({ children }: { children: ReactNode }) {
  return <div className="mt-4 flex gap-2">{children}</div>
}
