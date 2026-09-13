import type { ReactNode } from 'react'

/**
 * 설정 화면용 2단 구성. 왼쪽에 "이 구역이 무엇인지", 오른쪽에 실제 내용.
 *
 * 넓은 화면에서 입력 폼을 가운데 좁게 두면 양옆이 비어 허전하고, 그렇다고 입력창을
 * 화면 폭만큼 늘리면 한 줄이 1000px가 되어 오히려 쓰기 나빠진다.
 * 설명을 왼쪽으로 빼면 **남는 폭이 여백이 아니라 정보로 채워진다.**
 *
 * 좁은 화면(lg 미만)에서는 grid-cols가 풀려 설명이 위, 내용이 아래로 자연스럽게 쌓인다.
 * 모바일용 마크업을 따로 쓰지 않는 이유다.
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
    <section className="grid gap-5 lg:grid-cols-[15rem_minmax(0,1fr)] lg:gap-12">
      <div className="flex flex-col gap-2 lg:pt-1">
        <h2 className="text-[0.9375rem] font-semibold tracking-[-0.01em] text-strong">{title}</h2>
        {description !== undefined && (
          <p className="text-[0.8125rem] leading-relaxed text-muted-foreground">{description}</p>
        )}
      </div>
      {/* min-w-0: grid 자식은 기본적으로 내용보다 작아지지 않아서, 긴 값 하나가 열을 밀어낸다. */}
      <div className="min-w-0">{children}</div>
    </section>
  )
}
