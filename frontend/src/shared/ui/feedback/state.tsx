import { cn } from 'cn'

/**
 * 로딩·에러·안내 표시. shadcn 이 아니라 우리 파일
 * 빈 상태는 제외 — 화면마다 생김새가 달라(카드+버튼 / 한 줄 문장) 한 컴포넌트로 덮으면 옵션만 늘어남
 */

/** 스피너 대신 밝기만 오가는 점. 회전은 시선을 너무 끎 */
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
 * 빨간 글씨 한 줄이 아니라 옅은 면 — 글씨만으로는 "영역"으로 안 읽힘
 * role="alert" 로 스크린리더 즉시 읽기
 * 앱에서 가장 짧은 등장(0.24s/4px). 방금 누른 것에 대한 답이라 늦으면 안 됨
 */
export function ErrorText({ message, className }: { message: string; className?: string }) {
  return (
    <p
      role="alert"
      className={cn(
        'animate-alert border border-destructive/20 bg-destructive/[0.07] px-3.5 py-2.5 text-caption leading-relaxed text-destructive',
        className,
      )}
    >
      {message}
    </p>
  )
}

/**
 * 실패가 아닌 안내. 색 없이 농도만
 * 등장은 에러와 같은 박자 — 같은 자리에 뜨므로 속도가 다르면 헷갈림
 */
export function NoticeText({ message, className }: { message: string; className?: string }) {
  return (
    <p
      role="status"
      className={cn(
        'animate-alert border border-border bg-sunken px-3.5 py-2.5 text-caption leading-relaxed text-muted-foreground',
        className,
      )}
    >
      {message}
    </p>
  )
}

/**
 * 로딩 자리를 미리 차지하는 덩어리. 데이터가 도착해도 레이아웃이 안 튐
 * 좌우로 흐르는 띠로 멈춘 화면이 아님을 표시
 */
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
