// 날짜 문자열 ↔ 년·월·일. 드럼 휠용
// .ts 분리: 핫 리로드 유지
// 절단 기준은 max(고를 수 있는 마지막 날). 없으면 브라우저 지역의 오늘

import { todayString } from '@/shared/lib/format'

export function daysInMonth(year: number, month: number) {
  // 다음 달 0일 = 이번 달 마지막 날. 윤년 자동
  return new Date(year, month, 0).getDate()
}

export function todayParts(timeZone?: string) {
  const [year, month, day] = todayString(timeZone).split('-').map(Number)
  return { year, month, day }
}

/** 'YYYY-MM-DD' 분해. 빈 값·깨진 값이면 오늘 */
export function parse(value: string, timeZone?: string) {
  const [year, month, day] = value.split('-').map(Number)
  if (!Number.isInteger(year) || !Number.isInteger(month) || !Number.isInteger(day)) {
    return todayParts(timeZone)
  }
  return { year, month, day }
}

/** 고를 수 있는 마지막 날('YYYY-MM-DD'). 없으면 브라우저 지역의 오늘 */
function lastParts(max?: string) {
  return max === undefined ? todayParts() : parse(max)
}

/** 그 해·그 달의 선택 가능한 마지막 날. 마지막 날의 달이면 그날 */
export function lastSelectableDay(year: number, month: number, max?: string) {
  const last = lastParts(max)
  const end = daysInMonth(year, month)

  return year === last.year && month === last.month ? Math.min(end, last.day) : end
}

/** 그 해의 선택 가능한 마지막 달. 마지막 날의 해면 그달 */
export function lastSelectableMonth(year: number, max?: string) {
  const last = lastParts(max)

  return year === last.year ? last.month : 12
}

/**
 * 'YYYY-MM-DD' 조립. 선택 불가 값은 년 → 월 → 일 순서로 절단
 * 네이티브 max 와 같은 선. 기기별 차이 방지
 */
export function join(year: number, month: number, day: number, max?: string) {
  const clampedMonth = Math.min(month, lastSelectableMonth(year, max))
  const clampedDay = Math.min(day, lastSelectableDay(year, clampedMonth, max))

  return `${year}-${String(clampedMonth).padStart(2, '0')}-${String(clampedDay).padStart(2, '0')}`
}

/**
 * 'YYYY-MM-DD' 에서 몇 달 전. 없는 날(3/31 의 한 달 전)은 그 달 말일
 * "석 달 전쯤 갈았다" 를 날짜로 바꾸는 자리. Date 의 월 넘김(2/31 → 3/3) 방지
 */
export function monthsAgo(value: string, months: number) {
  const [year, month, day] = value.split('-').map(Number)
  const total = year * 12 + (month - 1) - months
  const targetYear = Math.floor(total / 12)
  const targetMonth = (total % 12) + 1
  const targetDay = Math.min(day, daysInMonth(targetYear, targetMonth))

  return `${targetYear}-${String(targetMonth).padStart(2, '0')}-${String(targetDay).padStart(2, '0')}`
}

export type DatePart = 'year' | 'month' | 'day'

/** 로케일의 칸 순서. 한국 년·월·일, 미국 월·일·년, 영국 일·월·년 */
export function partOrder(locale: string): DatePart[] {
  try {
    const order = new Intl.DateTimeFormat(locale, { year: 'numeric', month: 'numeric', day: 'numeric' })
      .formatToParts(new Date(Date.UTC(2026, 0, 15)))
      .map((part) => part.type)
      .filter((type): type is DatePart => type === 'year' || type === 'month' || type === 'day')

    return order.length === 3 ? order : ['year', 'month', 'day']
  } catch {
    return ['year', 'month', 'day']
  }
}
