import * as React from "react"
import { cn } from "cn"

/*
 * 카드는 평면이다. 전에는 흰색 3% + backdrop-blur(20px) 짜리 '유리'였는데 걷어냈다.
 * 흐림 효과는 2020년 이후 템플릿 UI 의 서명 같은 것이라 어느 서비스에나 있고,
 * 무엇보다 **뒤가 비치는 면은 그 위에 놓인 글자의 배경을 불확실하게 만든다.**
 *
 * 지금 카드가 하는 일은 두 가지뿐이다: 아주 옅은 면(bg-card)과 1px 괘선.
 * 그림자는 쓰지 않는다. 어두운 바탕의 그림자는 보이지도 않으면서 가장자리만 탁하게 만든다.
 * 높이 차이는 오직 괘선과 배경 농도로만 표현한다.
 *
 * **카드를 남발하지 않는 것이 이 디자인의 핵심이다.** 목록처럼 같은 모양이 반복되는 곳은
 * 카드를 하나씩 씌우지 않고 행을 괘선으로 나눈다 — 상자가 줄면 정보가 선으로 정렬된다.
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
        "group/card flex flex-col gap-(--card-spacing) border border-border bg-card py-(--card-spacing) text-sm text-card-foreground [--card-spacing:--spacing(7)] has-data-[slot=card-footer]:pb-0 data-[size=sm]:[--card-spacing:--spacing(6)]",
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
