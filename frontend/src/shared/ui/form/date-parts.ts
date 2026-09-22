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

/**
 * 그 해·그 달에 고를 수 있는 마지막 날
 * 올해 이번 달이면 오늘까지다 — 미래 날짜는 서버가 @PastOrPresent 로 거절한다
 */
export function lastSelectableDay(year: number, month: number) {
  const today = todayParts()
  const end = daysInMonth(year, month)

  return year === today.year && month === today.month ? Math.min(end, today.day) : end
}

/** 그 해에 고를 수 있는 마지막 달. 올해면 이번 달까지 */
export function lastSelectableMonth(year: number) {
  const today = todayParts()

  return year === today.year ? today.month : 12
}

/**
 * 'YYYY-MM-DD' 조립. 고를 수 없는 값은 자른다
 *
 * 자르는 순서가 년 → 월 → 일인 이유: 굴린 칸의 뜻을 최대한 살리기 위해서다.
 * 2020-12-25 에서 년을 올해로 굴리면 12월이 미래라 9월로 잘리고, 25일도 미래면 오늘로 잘린다.
 * 1/31 에서 2월로 옮기면 28일이 되는 것(윤년은 Date 가 계산)과 같은 성격의 보정이다
 *
 * 화면(네이티브 date 입력)은 max 로 막지만 휠은 칸을 직접 만들므로,
 * 여기서 자르지 않으면 **기기에 따라 되는 날짜가 달라진다**
 */
export function join(year: number, month: number, day: number) {
  const clampedMonth = Math.min(month, lastSelectableMonth(year))
  const clampedDay = Math.min(day, lastSelectableDay(year, clampedMonth))

  return `${year}-${String(clampedMonth).padStart(2, '0')}-${String(clampedDay).padStart(2, '0')}`
}
