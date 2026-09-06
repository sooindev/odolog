import { cn } from 'cn'

/**
 * 로딩·에러 표시. shadcn이 만들어 준 파일이 아니라 우리가 직접 쓴 것이다.
 *
 * 같은 마크업이 로딩 5곳, 에러 10곳에 글자까지 똑같이 복사돼 있었다.
 * 목록을 스켈레톤으로 바꾸려면 그 5곳을 전부 찾아 고쳐야 했다 — 이제 여기 한 곳이다.
 *
 * 빈 상태(빈 목록)는 일부러 뽑지 않았다. 차량 목록은 카드 + "첫 차량 등록하기" 버튼이고
 * 정비 이력은 한 줄짜리 문장이라, 억지로 한 컴포넌트에 담으면 옵션만 늘어난다.
 */
export function LoadingText({ className }: { className?: string }) {
  return <p className={cn('text-muted-foreground text-sm', className)}>불러오는 중…</p>
}

export function ErrorText({ message, className }: { message: string; className?: string }) {
  return <p className={cn('text-destructive text-sm', className)}>{message}</p>
}
