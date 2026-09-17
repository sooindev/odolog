import { cn } from 'cn'

/**
 * 계기판 바늘 마크. 파비콘과 같은 도형
 * 헤더·로그인·빈 상태 세 곳에 복사돼 있던 SVG — 로고는 언젠가 바뀌는 것이라 세 벌이면 놓침
 * 색은 currentColor. 쓰는 쪽에서 글자색으로 조절되고 테마도 자동으로 따라옴
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
