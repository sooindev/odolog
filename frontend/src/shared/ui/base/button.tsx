import { Button as ButtonPrimitive } from "@base-ui/react/button"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "cn"

// 각진 사각형(반경 토큰 0). rounded-* 미사용
// 호버·누름은 투명도만. 크기 변화 없음
const buttonVariants = cva(
  "group/button relative inline-flex shrink-0 items-center justify-center gap-2 border border-transparent bg-clip-padding text-[0.875rem] font-medium tracking-[-0.01em] whitespace-nowrap transition-all duration-200 ease-apple outline-none select-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background active:opacity-70 disabled:pointer-events-none disabled:opacity-40 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      variant: {
        // 유일한 Accent. 한 화면에 하나만
        default: "bg-primary text-primary-foreground hover:opacity-90",
        secondary: "bg-secondary text-strong hover:bg-wash",
        outline:
          "border-border bg-sunken text-foreground hover:bg-wash hover:text-strong",
        ghost: "text-muted-foreground hover:bg-wash hover:text-strong",
        destructive: "text-destructive hover:bg-destructive/10",
        link: "px-0 text-strong underline-offset-4 hover:underline",
      },
      // 크기는 임의 값. cva 는 클래스를 이어 붙이기만 해서 토큰을 쓰면 base 의 14px 이 우선 적용
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

// buttonVariants 미공개. 핫 리로드 유지
export { Button }
