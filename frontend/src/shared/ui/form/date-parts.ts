// 날짜 문자열 ↔ 년·월·일. 드럼 휠용
// .ts 분리: 핫 리로드 유지
// timeZone 을 주면 그 지역의 오늘 기준(서버 미래 판정과 같은 선). 없으면 브라우저 지역

import { todayString } from '@/shared/lib/format/format'

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

/** 그 해·그 달의 선택 가능한 마지막 날. 이번 달이면 오늘 */
export function lastSelectableDay(year: number, month: number, timeZone?: string) {
  const today = todayParts(timeZone)
  const end = daysInMonth(year, month)

  return year === today.year && month === today.month ? Math.min(end, today.day) : end
}

/** 그 해의 선택 가능한 마지막 달. 올해면 이번 달 */
export function lastSelectableMonth(year: number, timeZone?: string) {
  const today = todayParts(timeZone)

  return year === today.year ? today.month : 12
}

/**
 * 'YYYY-MM-DD' 조립. 선택 불가 값은 년 → 월 → 일 순서로 절단
 * 네이티브 max 와 같은 선. 기기별 차이 방지
 */
export function join(year: number, month: number, day: number, timeZone?: string) {
  const clampedMonth = Math.min(month, lastSelectableMonth(year, timeZone))
  const clampedDay = Math.min(day, lastSelectableDay(year, clampedMonth, timeZone))

  return `${year}-${String(clampedMonth).padStart(2, '0')}-${String(clampedDay).padStart(2, '0')}`
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
