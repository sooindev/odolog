import * as React from "react"
import { cn } from "cn"

/*
 * 라벨은 입력값보다 한 단계 어둡게(45%)
 * 같은 밝기면 무엇을 먼저 읽어야 할지 모르고 폼 전체가 웅성거림
 */
function Label({ className, ...props }: React.ComponentProps<"label">) {
  return (
    <label
      data-slot="label"
      className={cn(
        // 크기는 임의 값으로. cn 이 커스텀 토큰을 색으로 오해해 지운다 (card.tsx 주석 참고)
        "flex items-center gap-2 text-[0.8125rem] leading-none font-medium tracking-[-0.005em] text-muted-foreground select-none peer-disabled:opacity-50",
        className
      )}
      {...props}
    />
  )
}

export { Label }
