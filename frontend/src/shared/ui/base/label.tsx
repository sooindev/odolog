import * as React from "react"
import { cn } from "cn"

// 라벨은 입력값보다 한 단계 옅게
function Label({ className, ...props }: React.ComponentProps<"label">) {
  return (
    <label
      data-slot="label"
      className={cn(
        // 크기는 임의 값. cn 의 토큰 삭제 문제(card.tsx 참고)
        "flex items-center gap-2 text-[0.8125rem] leading-none font-medium tracking-[-0.005em] text-muted-foreground select-none peer-disabled:opacity-50",
        className
      )}
      {...props}
    />
  )
}

export { Label }
