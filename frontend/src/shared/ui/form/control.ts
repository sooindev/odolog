// input·textarea·네이티브 select 공용 클래스
// .ts 분리: 핫 리로드 유지
// 모바일 16px 유지. iOS 사파리 확대 방지
export const controlClassName =
  "h-11 w-full min-w-0 border border-input bg-fill px-3.5 text-base text-strong transition-all duration-200 ease-apple outline-none placeholder:text-faint focus-visible:border-ring/60 focus-visible:bg-fill-hover focus-visible:ring-[3px] focus-visible:ring-ring/15 disabled:cursor-not-allowed disabled:bg-sunken disabled:text-muted-foreground aria-invalid:border-destructive/50 aria-invalid:ring-[3px] aria-invalid:ring-destructive/15 md:text-[0.9375rem]"
