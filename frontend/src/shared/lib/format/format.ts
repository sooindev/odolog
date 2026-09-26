/** 45000 → "45,000" */
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

/** 320000 → "32만". 축 눈금 전용, 본문은 전체 자릿수 */
export function formatCompact(value: number) {
  if (value >= 100_000_000) return `${trimZero(value / 100_000_000)}억`
  if (value >= 10_000) return `${trimZero(value / 10_000)}만`

  return formatNumber(value)
}

/** 1.0 → "1", 1.5 → "1.5" */
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

/** 로컬 기준 오늘 날짜. toISOString() 은 UTC 라 오전 9시 전 하루 차이 */
export function todayString() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')

  return `${now.getFullYear()}-${month}-${day}`
}
