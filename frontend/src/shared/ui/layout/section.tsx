import type { ReactNode } from 'react'

/**
 * 설정 화면용 2단. 왼쪽에 이 구역이 무엇인지, 오른쪽에 내용.
 * 넓은 화면에서 남는 폭을 여백이 아니라 설명으로 채운다. 입력창을 화면 폭만큼 늘리면
 * 한 줄이 1000px 가 되어 오히려 쓰기 나빠진다.
 * lg 미만에서는 grid 가 풀려 저절로 쌓인다.
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
      {/* 구역마다 위에 괘선을 긋는다. 스크롤하지 않고도 몇 개의 구역이 있는지 보인다.
          제목에 eyebrow 를 쓰지 않는 이유: 한글은 대문자가 없어서 자간만 벌어진
          2글자("계정")가 흩어져 보인다. */}
      <div className="flex flex-col gap-3">
        <h2 className="text-[0.875rem] font-semibold tracking-[-0.01em] text-strong">{title}</h2>
        {description !== undefined && (
          <p className="text-[0.8125rem] leading-relaxed text-muted-foreground">{description}</p>
        )}
      </div>
      {/* min-w-0: grid 자식은 기본적으로 내용보다 작아지지 않아서, 긴 값 하나가 열을 밀어낸다. */}
      <div className="min-w-0">{children}</div>
    </section>
  )
}
