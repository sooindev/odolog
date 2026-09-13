/*
 * 입력 요소들이 공유하는 클래스 문자열.
 *
 * 컴포넌트(.tsx)가 아니라 별도 .ts 파일에 둔 이유는 프로젝트의 AuthContext 와 같다:
 * 한 파일에서 컴포넌트와 일반 값을 함께 내보내면 Vite 핫 리로드가 부분 갱신을
 * 포기하고 전체 새로고침으로 떨어진다 (oxlint `react(only-export-components)`).
 *
 * <input>, <textarea>, 그리고 shadcn 컴포넌트가 없는 네이티브 <select> 까지
 * 세 곳이 같은 높이·곡률·포커스 반응을 쓰도록 한 곳에 모았다.
 *
 * text-base(16px)를 모바일에서 유지하는 이유: iOS 사파리는 글자가 16px 미만인
 * 입력창에 포커스가 가면 화면을 자동으로 확대해 버린다. md 이상에서만 15px 로 줄인다.
 */
export const controlClassName =
  "h-11 w-full min-w-0 rounded-xl border border-input bg-fill px-3.5 text-base text-strong transition-all duration-200 ease-apple outline-none placeholder:text-faint focus-visible:border-ring/60 focus-visible:bg-fill-hover focus-visible:ring-[3px] focus-visible:ring-ring/15 disabled:cursor-not-allowed disabled:bg-sunken disabled:text-muted-foreground aria-invalid:border-destructive/50 aria-invalid:ring-[3px] aria-invalid:ring-destructive/15 md:text-[0.9375rem]"
