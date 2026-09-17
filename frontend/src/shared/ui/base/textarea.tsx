import * as React from "react"
import { cn } from "cn"

import { controlClassName } from "@/shared/ui/form/control"

function Textarea({ className, ...props }: React.ComponentProps<"textarea">) {
  return (
    <textarea
      data-slot="textarea"
      className={cn(
        controlClassName,
        // field-sizing-content — 입력 줄 수만큼 높이 자동 확장
        "field-sizing-content min-h-20 resize-none py-3 leading-relaxed",
        className
      )}
      {...props}
    />
  )
}

export { Textarea }
