import * as React from "react"
import { cn } from "@/shared/ui/cn"

import { controlClassName } from "@/shared/ui/form/control"
import { useHintId } from "@/shared/ui/form/field-context"

function Textarea({ className, ...props }: React.ComponentProps<"textarea">) {
  const hintId = useHintId(props.id)

  return (
    <textarea
      data-slot="textarea"
      aria-describedby={hintId}
      className={cn(
        controlClassName,
        // field-sizing-content: 줄 수만큼 높이 확장
        "field-sizing-content min-h-20 resize-none py-3 leading-relaxed",
        className
      )}
      {...props}
    />
  )
}

export { Textarea }
