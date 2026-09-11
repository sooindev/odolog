import * as React from "react"
import { cn } from "cn"

/*
 * 카드는 "검은 판 위에 올린 유리"다. 불투명한 회색 배경(#1C1C1E)을 칠하는 대신
 * 흰색 3% + backdrop-blur(20px) 로 만든다. 뒤에 깔린 상단 그라데이션이 유리를
 * 통과해 비치기 때문에, 화면 위쪽 카드가 아래쪽 카드보다 아주 미세하게 밝아진다.
 * 불투명하게 칠하면 이 깊이감이 사라진다.
 *
 * 그림자는 쓰지 않는다. 검정 위의 그림자는 보이지도 않으면서 가장자리만 탁하게 만든다.
 * 높이 차이는 오직 hairline 경계선(흰색 8%)으로만 표현한다.
 */
function Card({
  className,
  size = "default",
  ...props
}: React.ComponentProps<"div"> & { size?: "default" | "sm" }) {
  return (
    <div
      data-slot="card"
      data-size={size}
      className={cn(
        "group/card flex flex-col gap-(--card-spacing) rounded-2xl border border-border bg-card py-(--card-spacing) text-sm text-card-foreground backdrop-blur-[20px] [--card-spacing:--spacing(6)] has-data-[slot=card-footer]:pb-0 data-[size=sm]:[--card-spacing:--spacing(5)]",
        className
      )}
      {...props}
    />
  )
}

function CardHeader({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-header"
      className={cn(
        "grid auto-rows-min items-start gap-1.5 px-(--card-spacing) has-data-[slot=card-action]:grid-cols-[1fr_auto]",
        className
      )}
      {...props}
    />
  )
}

function CardTitle({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-title"
      className={cn(
        "font-heading text-[1.0625rem] leading-snug font-semibold tracking-[-0.02em] text-strong",
        className
      )}
      {...props}
    />
  )
}

function CardDescription({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-description"
      className={cn("text-sm text-muted-foreground", className)}
      {...props}
    />
  )
}

function CardAction({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-action"
      className={cn("col-start-2 row-span-2 row-start-1 self-start justify-self-end", className)}
      {...props}
    />
  )
}

function CardContent({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div data-slot="card-content" className={cn("px-(--card-spacing)", className)} {...props} />
  )
}

function CardFooter({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-footer"
      className={cn(
        "flex items-center border-t border-border p-(--card-spacing)",
        className
      )}
      {...props}
    />
  )
}

export { Card, CardHeader, CardFooter, CardTitle, CardAction, CardDescription, CardContent }
