import { cn } from 'cn'

/**
 * 로딩·에러·안내 표시
 * 빈 상태는 화면마다 달라 제외
 */

/** 스피너 대신 밝기만 오가는 점 */
export function LoadingText({ className }: { className?: string }) {
  return (
    <div
      role="status"
      className={cn('flex items-center gap-2.5 text-sm text-muted-foreground', className)}
    >
      <span className="size-1.5 animate-breathe bg-current" aria-hidden="true" />
      <span>불러오는 중…</span>
    </div>
  )
}

/**
 * 옅은 면의 오류 표시. role="alert"
 * 가장 짧은 등장(0.24s/4px)
 */
export function ErrorText({ message, className }: { message: string; className?: string }) {
  return (
    <p
      role="alert"
      className={cn(
        // 크기는 임의 값. cn 의 토큰 삭제 문제(card.tsx 참고)
        'animate-alert border border-destructive/20 bg-destructive/[0.07] px-3.5 py-2.5 text-[0.8125rem] leading-relaxed text-destructive',
        className,
      )}
    >
      {message}
    </p>
  )
}

/** 실패가 아닌 안내. 색 없이 농도만, 등장은 에러와 동일 */
export function NoticeText({ message, className }: { message: string; className?: string }) {
  return (
    <p
      role="status"
      className={cn(
        'animate-alert border border-border bg-sunken px-3.5 py-2.5 text-[0.8125rem] leading-relaxed text-muted-foreground',
        className,
      )}
    >
      {message}
    </p>
  )
}

/** 로딩 자리 확보용 덩어리. 좌우로 흐르는 띠 */
export function Skeleton({ className }: { className?: string }) {
  return (
    <div
      aria-hidden="true"
      className={cn(
        'animate-shimmer bg-wash bg-[linear-gradient(90deg,transparent,var(--shimmer),transparent)] bg-[length:200%_100%]',
        className,
      )}
    />
  )
}
