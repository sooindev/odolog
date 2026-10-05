import * as React from "react"
import { cn } from "@/shared/ui/cn"

// 라벨은 입력값보다 한 단계 옅게
function Label({ className, ...props }: React.ComponentProps<"label">) {
  return (
    <label
      data-slot="label"
      className={cn(
        "flex items-center gap-2 text-caption leading-none font-medium tracking-[-0.005em] text-muted-foreground select-none peer-disabled:opacity-50",
        className
      )}
      {...props}
    />
  )
}

export { Label }
