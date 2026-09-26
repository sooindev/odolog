import { ChevronLeft } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router'

/**
 * 앱 화면 한 장의 껍데기. 머리말과 간격 담당
 *
 * eyebrow  소속. 로그인·회원가입은 없음
 * back     목록에서 들어온 화면만
 * action   이 화면에서 새로 만드는 동작 하나
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

              {/* text-balance: 둘째 줄 외톨이 단어 방지 */}
              <h1 className="text-title text-balance text-strong">{title}</h1>
            </div>

            {action !== undefined && <div className="shrink-0 pb-2">{action}</div>}
          </div>

          {/* 설명 폭 46ch */}
          {description !== undefined && (
            <p className="max-w-[46ch] text-lede text-muted-foreground">{description}</p>
          )}
        </header>

        {/* 머리말 아래 기준선. 화면당 하나뿐인 등장 연출 */}
        <hr className="rule-draw border-t border-border" />
      </div>

      {children}
    </div>
  )
}

/** 폼 맨 아래 버튼 줄. 주 동작 먼저, 취소는 ghost */
export function FormActions({ children }: { children: ReactNode }) {
  return <div className="mt-4 flex gap-2">{children}</div>
}
