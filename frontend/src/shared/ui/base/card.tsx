import * as React from "react"
import { cn } from "cn"

// 옅은 면 + 1px 괘선. 그림자 없음
// 반복 목록은 카드 대신 괘선 행
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
        "group/card flex flex-col gap-(--card-spacing) border border-border bg-card py-(--card-spacing) text-sm text-card-foreground [--card-spacing:--spacing(5)] sm:[--card-spacing:--spacing(7)] has-data-[slot=card-footer]:pb-0 data-[size=sm]:[--card-spacing:--spacing(5)] sm:data-[size=sm]:[--card-spacing:--spacing(6)]",
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

// 제목 태그는 h2. 카드 제목 = 구역 제목, 스크린리더 목차용
// 레벨 prop 은 Section 안 카드가 생길 때
function CardTitle({ className, ...props }: React.ComponentProps<"h2">) {
  return (
    <h2
      data-slot="card-title"
      className={cn(
        // 크기는 임의 값(--text-section 과 동일). cn 이 커스텀 토큰을 색으로 인식해 삭제
        "font-heading text-[1.0625rem] leading-[1.35] font-semibold tracking-[-0.022em] text-strong",
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
