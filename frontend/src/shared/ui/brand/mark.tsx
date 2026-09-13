import { cn } from 'cn'

/**
 * 계기판 바늘 마크. 파비콘과 같은 도형이다.
 *
 * 헤더·로그인 화면·빈 상태 세 곳에 같은 SVG가 복사돼 있었다. 로고는 언젠가 반드시
 * 바뀌는 것이라, 세 벌로 두면 그때 한두 곳을 놓친다.
 *
 * 색을 지정하지 않고 currentColor 로 둔다. 그래야 쓰는 쪽에서 text-strong,
 * text-muted-foreground 처럼 글자색으로 조절할 수 있고 테마도 자동으로 따라온다.
 */
export function GaugeMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 32 32" className={cn('size-[18px]', className)} aria-hidden="true">
      <path
        d="M8 20a8 8 0 1 1 16 0"
        fill="none"
        stroke="currentColor"
        strokeOpacity="0.3"
        strokeWidth="2.5"
        strokeLinecap="round"
      />
      <path
        d="M16 20 21 13"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
      />
    </svg>
  )
}
