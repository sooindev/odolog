import { Button as ButtonPrimitive } from "@base-ui/react/button"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "cn"

/*
 * 알약(pill) 형태를 기본으로 삼는다. 애플의 다크 UI에서 버튼은 대개 완전한 원호이고,
 * 사각에 가까운 버튼은 "웹 부트스트랩" 인상을 강하게 남긴다.
 *
 * 호버에서 색을 바꾸지 않고 투명도·배경 농도만 미세하게 움직인다. 누를 때만
 * scale(0.97) 로 살짝 들어간다 — 기기에서 손가락에 눌리는 느낌을 흉내 낸 것.
 */
const buttonVariants = cva(
  "group/button relative inline-flex shrink-0 items-center justify-center gap-2 rounded-full border border-transparent bg-clip-padding text-[0.9375rem] font-medium tracking-[-0.01em] whitespace-nowrap transition-all duration-200 ease-apple outline-none select-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background active:scale-[0.97] disabled:pointer-events-none disabled:opacity-40 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
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
        link: "rounded-none px-0 text-strong underline-offset-4 hover:underline active:scale-100",
      },
      size: {
        default: "h-10 px-5",
        xs: "h-7 px-3 text-xs",
        sm: "h-8 px-3.5 text-[0.8125rem]",
        lg: "h-12 px-7 text-base",
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
