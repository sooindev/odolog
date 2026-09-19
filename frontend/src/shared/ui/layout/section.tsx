import type { ReactNode } from 'react'

/**
 * 설정 화면용 2단. 왼쪽 설명 / 오른쪽 내용
 * 남는 폭을 여백이 아니라 설명으로 채움 — 입력창을 화면 폭만큼 늘리면 한 줄이 1000px
 * lg 미만에서는 grid 가 풀려 저절로 쌓임
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
      {/* 구역마다 위에 괘선. 스크롤하지 않고도 구역 수가 보임
          제목에 eyebrow 를 안 쓰는 이유 — 한글은 대문자가 없어 자간만 벌어진 2글자가 흩어져 보임 */}
      <div className="flex flex-col gap-3">
        <h2 className="text-[0.875rem] font-semibold tracking-[-0.01em] text-strong">{title}</h2>
        {description !== undefined && (
          <p className="text-caption leading-relaxed text-muted-foreground">{description}</p>
        )}
      </div>
      {/* min-w-0 — grid 자식은 내용보다 작아지지 않아 긴 값 하나가 열을 밀어냄 */}
      <div className="min-w-0">{children}</div>
    </section>
  )
}
