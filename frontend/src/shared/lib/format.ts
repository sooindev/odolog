/** 45000 → "45,000" — 단위 없이 숫자만. 큰 글씨로 값과 단위를 따로 배치할 때 쓴다. */
export function formatNumber(value: number) {
  return value.toLocaleString('ko-KR')
}

/** 45000 → "45,000km" */
export function formatKm(value: number) {
  return `${formatNumber(value)}km`
}

/** 50000 → "50,000원" */
export function formatWon(value: number) {
  return `${formatNumber(value)}원`
}

/** "2026-07-15" → "2026. 7. 15." — 목록에서 날짜를 읽기 쉽게. */
export function formatDate(value: string) {
  const [year, month, day] = value.split('-')

  return `${year}. ${Number(month)}. ${Number(day)}.`
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
