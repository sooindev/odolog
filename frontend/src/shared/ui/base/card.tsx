import * as React from "react"
import { cn } from "cn"

/*
 * 카드가 하는 일은 둘뿐 — 아주 옅은 면과 1px 괘선
 * 그림자 없음. 어두운 바탕에서는 보이지도 않으면서 가장자리만 탁해짐
 * 반복되는 목록에는 카드를 씌우지 않고 행을 괘선으로 나눔
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

/*
 * shadcn 원본은 div 지만 h2 로 바꿨다. 이 앱에서 카드 제목은 곧 구역 제목이고,
 * 차량 상세 한 화면에만 카드가 여섯이다 — div 로 두면 스크린리더의 heading 목차에
 * 제목이 h1 하나만 잡혀 화면 전체가 한 덩어리로 보인다.
 * 레벨을 prop 으로 열지 않은 이유: 지금 CardTitle 열 곳이 전부 Page(h1) 직속이라
 * h2 가 맞고, Section(h2) 안에 카드를 넣는 화면이 아직 없다. 생기면 그때 연다.
 */
function CardTitle({ className, ...props }: React.ComponentProps<"h2">) {
  return (
    <h2
      data-slot="card-title"
      className={cn(
        "font-heading text-section text-strong",
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
