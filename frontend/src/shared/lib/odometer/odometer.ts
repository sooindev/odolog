// 주행거리 입력 판정 규칙. 주유·정비·주행거리 갱신 폼 공용

/** 기존보다 작은 값 = 과거 기록 또는 계기판 교체 */
export function looksPast(value: number, base: number) {
  return Number.isFinite(value) && value < base
}

/**
 * 자리수 오타로 보이는 급증
 * 올라간 값은 force 정정으로만 복구되어 감소보다 위험
 * 3배 이상이면서 1만km 이상 증가. 배수만이면 새 차 오탐, 절대량만이면 자리수 오타 누락
 * 기존 값 0 은 제외(첫 입력)
 */
export function looksBigJump(value: number, base: number) {
  if (!Number.isFinite(value) || base <= 0) {
    return false
  }

  return value >= base * 3 && value - base >= 10_000
}
