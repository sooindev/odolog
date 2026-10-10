import * as React from "react"
import { Input as InputPrimitive } from "@base-ui/react/input"
import { cn } from "@/shared/ui/cn"

import { controlClassName } from "@/shared/ui/form/control"
import { useHintId } from "@/shared/ui/form/field-context"

function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  const hintId = useHintId(props.id)

  return (
    <InputPrimitive
      type={type}
      data-slot="input"
      aria-describedby={hintId}
      className={cn(
        controlClassName,
        "file:inline-flex file:h-7 file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-strong",
        className
      )}
      {...props}
    />
  )
}

export { Input }
