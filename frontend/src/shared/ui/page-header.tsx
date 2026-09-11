import type { ReactNode } from 'react'

/**
 * 화면 맨 위의 제목 블록.
 *
 * eyebrow(작은 대문자 라벨)는 제목 위에 놓이는 한 단어짜리 분류다. 제목만 크게 두면
 * 페이지가 어디에 속한 화면인지 알 수 없고, 제목을 길게 늘이면 타이포그래피가 무너진다.
 * 자간을 아주 넓게(0.16em) 벌려 제목의 좁은 자간(-0.03em)과 대비시키는 것이
 * 이 조합의 핵심이다 — 두 줄이 크기가 아니라 '질감'으로 구분된다.
 */
export function PageHeader({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow?: string
  title: string
  description?: string
  action?: ReactNode
}) {
  return (
    <header className="flex items-end justify-between gap-6">
      <div className="flex flex-col gap-2">
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
  )
}
