/** 45000 → "45,000". 값과 단위를 따로 배치할 때 */
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

/**
 * 320000 → "32만". 축 눈금처럼 자리가 좁은 곳 전용
 * 본문은 전체 자릿수 — 요약한 숫자를 근거로 계산하게 두면 안 됨
 */
export function formatCompact(value: number) {
  if (value >= 100_000_000) return `${trimZero(value / 100_000_000)}억`
  if (value >= 10_000) return `${trimZero(value / 10_000)}만`

  return formatNumber(value)
}

/** 1.0 → "1", 1.5 → "1.5". 소수점 아래가 0 이면 제거 */
function trimZero(value: number) {
  return String(Math.round(value * 10) / 10)
}

/** "2026-07" → "2026년 7월" */
export function formatMonth(value: string) {
  const [year, month] = value.split('-')

  return `${year}년 ${Number(month)}월`
}

/** "2026-07-15" → "2026. 7. 15." */
export function formatDate(value: string) {
  const [year, month, day] = value.split('-')

  return `${year}. ${Number(month)}. ${Number(day)}.`
}

/** 오늘 날짜. toISOString() 은 UTC 기준이라 오전 9시 이전에 하루 전 날짜가 나옴 */
export function todayString() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')

  return `${now.getFullYear()}-${month}-${day}`
}
