import { Button as ButtonPrimitive } from "@base-ui/react/button"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "cn"

/*
 * 각진 사각형. rounded-* 를 적지 않는 이유 — 토큰이 전부 0 이라 그냥 두면 각짐
 * 버튼 아래변이 표의 선·카드의 변과 같은 방향으로 정렬됨
 * 호버·누름 모두 크기 대신 투명도만. 눌릴 때 줄어들면 고무처럼 보이고 괘선 정렬이 어긋남
 */
const buttonVariants = cva(
  "group/button relative inline-flex shrink-0 items-center justify-center gap-2 border border-transparent bg-clip-padding text-[0.875rem] font-medium tracking-[-0.01em] whitespace-nowrap transition-all duration-200 ease-apple outline-none select-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background active:opacity-70 disabled:pointer-events-none disabled:opacity-40 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      variant: {
        // 유일한 Accent. 화면에서 가장 밝은 면이라 한 화면에 하나만
        default: "bg-primary text-primary-foreground hover:opacity-90",
        secondary: "bg-secondary text-strong hover:bg-wash",
        outline:
          "border-border bg-sunken text-foreground hover:bg-wash hover:text-strong",
        ghost: "text-muted-foreground hover:bg-wash hover:text-strong",
        destructive: "text-destructive hover:bg-destructive/10",
        link: "px-0 text-strong underline-offset-4 hover:underline",
      },
      /*
       * ⚠️ 여기만 타입 스케일 토큰(text-body / text-caption)을 쓰지 않는다.
       * cva 는 클래스를 이어 붙이기만 하고 충돌을 해결하지 않아서, 위 base 의
       * text-[0.875rem] 과 여기의 크기가 **둘 다** 최종 class 에 남는다.
       * 그러면 승자는 Tailwind 가 CSS 를 배치한 순서로 정해지는데,
       * **토큰 유틸리티가 임의 값보다 먼저 배치된다** — 즉 토큰을 쓰면 base 의 14px 이 이겨
       * size="lg" 버튼이 15px 이 아니라 14px 로 렌더된다(2026-09-19 에 실제로 그렇게 깨졌다).
       * 임의 값끼리는 나중에 오는 쪽이 이기므로 여기서는 임의 값을 유지한다.
       */
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

// buttonVariants 는 내보내지 않는다 — 컴포넌트와 값을 한 파일에서 내보내면 핫 리로드가 깨진다(쓰는 곳도 없다)
export { Button }
