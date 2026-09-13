import { Button as ButtonPrimitive } from "@base-ui/react/button"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "cn"

/*
 * 각진 사각형을 기본으로 삼는다. 반경을 따로 적지 않는 이유는 --radius-* 가 전부 0 이라
 * 아무것도 안 쓰면 그대로 각지기 때문이다.
 *
 * 전에는 알약(pill) 형태였다. 완전한 원호는 어느 화면에나 무난하게 어울리는 대신
 * 화면의 성격을 지운다 — 글자·수치·표가 전부 직선인데 버튼만 원호면 버튼이 따로 논다.
 * 각지게 두면 버튼의 아래변이 표의 선, 카드의 변과 같은 방향으로 정렬된다.
 *
 * 호버에서 색을 바꾸지 않고 투명도·배경 농도만 미세하게 움직인다.
 * 누를 때도 **크기를 바꾸지 않는다**(전에는 scale(0.97) 이었다). 각진 사각형이 눌릴 때
 * 줄어들면 고무처럼 보이고, 무엇보다 버튼의 변이 주변 괘선과 맞춰 둔 정렬이 그 순간
 * 어긋난다. 지금은 투명도만 떨어진다 — 선은 제자리에 있고 반응만 전달된다.
 */
const buttonVariants = cva(
  "group/button relative inline-flex shrink-0 items-center justify-center gap-2 border border-transparent bg-clip-padding text-[0.875rem] font-medium tracking-[-0.01em] whitespace-nowrap transition-all duration-200 ease-apple outline-none select-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background active:opacity-70 disabled:pointer-events-none disabled:opacity-40 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      variant: {
        // 유일한 Accent. 화면에서 가장 밝은 면이라 한 화면에 하나만 두는 것이 원칙이다.
        default: "bg-primary text-primary-foreground hover:opacity-90",
        secondary: "bg-secondary text-strong hover:bg-wash",
        outline:
          "border-border bg-sunken text-foreground hover:bg-wash hover:text-strong",
        ghost: "text-muted-foreground hover:bg-wash hover:text-strong",
        destructive: "text-destructive hover:bg-destructive/10",
        link: "px-0 text-strong underline-offset-4 hover:underline",
      },
      size: {
        default: "h-11 px-6",
        xs: "h-7 px-3 text-xs",
        sm: "h-8 px-3.5 text-[0.8125rem]",
        lg: "h-13 px-8 text-[0.9375rem]",
        icon: "size-10",
        "icon-xs": "size-7",
        "icon-sm": "size-8",
        "icon-lg": "size-12",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

function Button({
  className,
  variant = "default",
  size = "default",
  ...props
}: ButtonPrimitive.Props & VariantProps<typeof buttonVariants>) {
  return (
    <ButtonPrimitive
      data-slot="button"
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  )
}

export { Button, buttonVariants }
