import { cn } from 'cn'

/**
 * 로딩·에러·안내 표시. shadcn 파일이 아니라 우리가 쓴 것이다.
 *
 * 빈 상태는 일부러 넣지 않았다. 화면마다 생김새가 달라서(카드+버튼 / 한 줄 문장)
 * 한 컴포넌트로 덮으면 옵션만 늘어난다.
 */

/** 회전하는 스피너 대신 밝기만 오가는 점. 회전은 시선을 너무 끈다. */
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
 * 빨간 글씨 한 줄이 아니라 옅은 면으로 보여준다. 글씨만으로는 "영역"으로 읽히지 않는다.
 * role="alert" 는 스크린리더가 즉시 읽어 주게 한다.
 * 등장이 앱에서 가장 짧다(0.24s/4px). 방금 누른 것에 대한 답이라 늦게 오면 안 된다.
 */
export function ErrorText({ message, className }: { message: string; className?: string }) {
  return (
    <p
      role="alert"
      className={cn(
        'animate-alert border border-destructive/20 bg-destructive/[0.07] px-3.5 py-2.5 text-[0.8125rem] leading-relaxed text-destructive',
        className,
      )}
    >
      {message}
    </p>
  )
}

/**
 * 실패가 아닌 안내("저장했습니다"). 색 없이 농도만 다르다.
 * 등장은 에러와 같은 박자. 둘 다 같은 자리에 뜨므로 속도까지 같아야 헷갈리지 않는다.
 */
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

/**
 * 로딩 자리를 미리 차지하는 덩어리. 글자 한 줄과 달리 데이터가 도착해도 레이아웃이 안 튄다.
 * 좌우로 흐르는 띠를 얹어 멈춘 화면이 아님을 알린다.
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
