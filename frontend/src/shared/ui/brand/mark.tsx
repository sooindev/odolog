import { cn } from 'cn'

/**
 * 계기판 바늘 마크. 파비콘과 같은 도형
 * 색은 currentColor. 글자색·테마 자동 반영
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
