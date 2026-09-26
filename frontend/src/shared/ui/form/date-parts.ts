// 날짜 문자열 ↔ 년·월·일. 드럼 휠용
// .ts 분리: 핫 리로드 유지

export function daysInMonth(year: number, month: number) {
  // 다음 달 0일 = 이번 달 마지막 날. 윤년 자동
  return new Date(year, month, 0).getDate()
}

export function todayParts() {
  const now = new Date()
  return { year: now.getFullYear(), month: now.getMonth() + 1, day: now.getDate() }
}

/** 'YYYY-MM-DD' 분해. 빈 값·깨진 값이면 오늘 */
export function parse(value: string) {
  const [year, month, day] = value.split('-').map(Number)
  if (!Number.isInteger(year) || !Number.isInteger(month) || !Number.isInteger(day)) {
    return todayParts()
  }
  return { year, month, day }
}

/** 그 해·그 달의 선택 가능한 마지막 날. 이번 달이면 오늘 */
export function lastSelectableDay(year: number, month: number) {
  const today = todayParts()
  const end = daysInMonth(year, month)

  return year === today.year && month === today.month ? Math.min(end, today.day) : end
}

/** 그 해의 선택 가능한 마지막 달. 올해면 이번 달 */
export function lastSelectableMonth(year: number) {
  const today = todayParts()

  return year === today.year ? today.month : 12
}

/**
 * 'YYYY-MM-DD' 조립. 선택 불가 값은 년 → 월 → 일 순서로 절단
 * 네이티브 max 와 같은 선. 기기별 차이 방지
 */
export function join(year: number, month: number, day: number) {
  const clampedMonth = Math.min(month, lastSelectableMonth(year))
  const clampedDay = Math.min(day, lastSelectableDay(year, clampedMonth))

  return `${year}-${String(clampedMonth).padStart(2, '0')}-${String(clampedDay).padStart(2, '0')}`
}
