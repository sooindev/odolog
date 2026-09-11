import { cn } from 'cn'

/**
 * 로딩·에러·안내 표시. shadcn이 만들어 준 파일이 아니라 우리가 직접 쓴 것이다.
 *
 * 같은 마크업이 로딩 5곳, 에러 10곳에 글자까지 똑같이 복사돼 있었다.
 * 스타일을 바꾸려면 그 15곳을 전부 찾아 고쳐야 했다 — 이제 여기 한 곳이다.
 *
 * 빈 상태(빈 목록)는 일부러 뽑지 않았다. 차량 목록은 카드 + "첫 차량 등록하기" 버튼이고
 * 정비 이력은 한 줄짜리 문장이라, 억지로 한 컴포넌트에 담으면 옵션만 늘어난다.
 */

/** 스피너 대신 숨 쉬듯 밝기만 오가는 점 하나. 회전은 시선을 끌지만 여기선 방해가 된다. */
export function LoadingText({ className }: { className?: string }) {
  return (
    <div
      role="status"
      className={cn('flex items-center gap-2.5 text-sm text-muted-foreground', className)}
    >
      <span className="size-1.5 animate-breathe rounded-full bg-current" aria-hidden="true" />
      <span>불러오는 중…</span>
    </div>
  )
}

/**
 * 에러는 빨간 글씨 한 줄이 아니라 옅은 면으로 보여준다. 검정 배경에서 빨간 글씨만
 * 덩그러니 놓이면 폼의 다른 글자와 구분은 되지만 "영역"으로 읽히지 않는다.
 * role="alert" 는 스크린리더가 이 문장을 즉시 읽어 주게 한다.
 */
export function ErrorText({ message, className }: { message: string; className?: string }) {
  return (
    <p
      role="alert"
      className={cn(
        'rounded-xl border border-destructive/20 bg-destructive/[0.07] px-3.5 py-2.5 text-[0.8125rem] leading-relaxed text-destructive',
        className,
      )}
    >
      {message}
    </p>
  )
}

/** 실패가 아닌 안내("저장했습니다", "변경된 내용이 없습니다"). 색 없이 농도만 다르다. */
export function NoticeText({ message, className }: { message: string; className?: string }) {
  return (
    <p
      role="status"
      className={cn(
        'rounded-xl border border-border bg-sunken px-3.5 py-2.5 text-[0.8125rem] leading-relaxed text-muted-foreground',
        className,
      )}
    >
      {message}
    </p>
  )
}

/**
 * 목록이 로딩될 때 들어갈 회색 덩어리. "불러오는 중…" 글자보다 나은 이유는
 * 화면의 크기가 미리 확정되어, 데이터가 도착해도 레이아웃이 튀지 않기 때문이다.
 * 배경색 위에 좌우로 흐르는 밝은 띠(animate-shimmer)를 한 겹 얹어 멈춘 화면이 아님을 알린다.
 */
export function Skeleton({ className }: { className?: string }) {
  return (
    <div
      aria-hidden="true"
      className={cn(
        'animate-shimmer rounded-xl bg-wash bg-[linear-gradient(90deg,transparent,var(--shimmer),transparent)] bg-[length:200%_100%]',
        className,
      )}
    />
  )
}
