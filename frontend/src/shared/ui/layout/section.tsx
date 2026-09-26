import type { ReactNode } from 'react'

/**
 * 설정 화면용 2단. 왼쪽 설명 / 오른쪽 내용
 * lg 미만은 한 줄로 쌓임
 */
export function Section({
  title,
  description,
  children,
}: {
  title: string
  description?: string
  children: ReactNode
}) {
  return (
    <section className="grid gap-5 border-t border-border pt-8 sm:gap-6 sm:pt-10 lg:grid-cols-[16rem_minmax(0,1fr)] lg:gap-16">
      {/* 구역마다 위 괘선. 한글 제목이라 eyebrow 미사용 */}
      <div className="flex flex-col gap-3">
        <h2 className="text-[0.875rem] font-semibold tracking-[-0.01em] text-strong">{title}</h2>
        {description !== undefined && (
          <p className="text-caption leading-relaxed text-muted-foreground">{description}</p>
        )}
      </div>
      {/* min-w-0: 긴 값의 열 밀림 방지 */}
      <div className="min-w-0">{children}</div>
    </section>
  )
}
