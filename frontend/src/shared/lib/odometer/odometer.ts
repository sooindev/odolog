/*
 * 주행거리 입력을 읽는 규칙. 폼 셋(주유·정비·주행거리 갱신)이 공유한다
 * 전에는 looksPast 가 두 폼에 복사돼 있었다 — 규칙이 두 벌이면 한쪽만 고치게 된다
 */

/** 이번 입력이 기존보다 작다 = 과거 기록이거나 계기판을 교체했다 */
export function looksPast(value: number, base: number) {
  return Number.isFinite(value) && value < base
}

/**
 * 자리수를 잘못 넣은 것으로 보이는 급증
 *
 * 왜 필요한가: 줄어드는 것은 확인 창으로 막으면서 **비정상적으로 커지는 것은 무방비**였다.
 * 방향이 거꾸로다 — 줄이는 것은 force 로 되돌릴 수 있지만, 늘어난 값은 liftOdometerTo 가
 * "지금까지 기록된 최댓값" 으로 붙잡아서 force 정정 말고는 길이 없다.
 *
 * 기준이 배수와 절대량 둘인 이유:
 *   · 배수만 보면 1,000 → 5,000(5배) 같은 새 차의 정상 입력을 잡는다
 *   · 절대량만 보면 4,500 → 45,000(자리수 오타)을 놓친다
 * 3배는 "오래 기록을 안 하다가 지금 계기판을 적는" 경우(보통 2배 안쪽)를 통과시키는 선이다
 *
 * 기존 값이 0 이면 묻지 않는다 — 차량을 막 등록하고 처음 계기판을 적는 자리다
 */
export function looksBigJump(value: number, base: number) {
  if (!Number.isFinite(value) || base <= 0) {
    return false
  }

  return value >= base * 3 && value - base >= 10_000
}
