/*
 * 날짜 문자열 ↔ 년·월·일 변환. date-input 의 드럼 휠이 쓴다
 * 컴포넌트 파일에서 내보내면 핫 리로드가 깨져 control.ts 와 같은 이유로 .ts 로 갈라 둔다
 */

export function daysInMonth(year: number, month: number) {
  // 다음 달 0일 = 이번 달 마지막 날. 윤년 자동 처리
  return new Date(year, month, 0).getDate()
}

export function todayParts() {
  const now = new Date()
  return { year: now.getFullYear(), month: now.getMonth() + 1, day: now.getDate() }
}

/**
 * 'YYYY-MM-DD' 분해. 빈 값·깨진 값이면 오늘로
 * 네이티브 date 는 지우면 빈 문자열을 주는데, 그 상태로 휠이 받으면 NaN 이 찍힘
 */
export function parse(value: string) {
  const [year, month, day] = value.split('-').map(Number)
  if (!Number.isInteger(year) || !Number.isInteger(month) || !Number.isInteger(day)) {
    return todayParts()
  }
  return { year, month, day }
}

export function join(year: number, month: number, day: number) {
  // 일수가 줄어드는 달로 옮기면 자르기 (1/31 → 2/28)
  const clamped = Math.min(day, daysInMonth(year, month))
  return `${year}-${String(month).padStart(2, '0')}-${String(clamped).padStart(2, '0')}`
}
