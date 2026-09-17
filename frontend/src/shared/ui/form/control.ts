/*
 * input · textarea · 네이티브 select 공용 클래스 문자열
 * .tsx 가 아닌 이유는 AuthContext 와 같음 — 컴포넌트와 값을 한 파일에서 내보내면 핫 리로드가 깨짐
 * 모바일 16px 유지 — iOS 사파리는 그보다 작은 입력창에 포커스가 가면 화면을 확대
 */
export const controlClassName =
  "h-11 w-full min-w-0 border border-input bg-fill px-3.5 text-base text-strong transition-all duration-200 ease-apple outline-none placeholder:text-faint focus-visible:border-ring/60 focus-visible:bg-fill-hover focus-visible:ring-[3px] focus-visible:ring-ring/15 disabled:cursor-not-allowed disabled:bg-sunken disabled:text-muted-foreground aria-invalid:border-destructive/50 aria-invalid:ring-[3px] aria-invalid:ring-destructive/15 md:text-[0.9375rem]"
