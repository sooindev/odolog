/** 45000 → "45,000km" */
export function formatKm(value: number) {
  return `${value.toLocaleString('ko-KR')}km`
}

/** 50000 → "50,000원" */
export function formatWon(value: number) {
  return `${value.toLocaleString('ko-KR')}원`
}

/**
 * 오늘 날짜를 YYYY-MM-DD 로. `new Date().toISOString()`을 쓰면 UTC 기준이라
 * 한국 시간 오전 9시 이전에는 하루 전 날짜가 나온다.
 */
export function todayString() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')

  return `${now.getFullYear()}-${month}-${day}`
}
