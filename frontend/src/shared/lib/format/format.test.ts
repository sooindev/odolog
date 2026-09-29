import { afterEach, describe, expect, it, vi } from 'vitest'

import { createFormatter, todayString } from './format'

const korean = createFormatter({ locale: 'ko-KR', language: 'KO', unitSystem: 'KM_PER_L', currency: 'KRW' })
const american = createFormatter({ locale: 'en-US', language: 'EN', unitSystem: 'MPG_US', currency: 'USD' })
const british = createFormatter({ locale: 'en-GB', language: 'EN', unitSystem: 'MPG_UK', currency: 'GBP' })

describe('숫자·단위 표기', () => {
  it('한국어는 지금까지와 같다', () => {
    expect(korean.number(45000)).toBe('45,000')
    expect(korean.distance(45000)).toBe('45,000km')
    expect(korean.money(50000)).toBe('50,000원')
  })

  it('미국은 마일·달러, 금액은 센트에서 달러로', () => {
    // 저장은 km. 16,093km = 10,000mi
    expect(american.distance(16093)).toBe('10,000 mi')
    expect(american.money(4567)).toBe('$45.67')
    expect(american.volume(37.85)).toBe('10.00 gal')
  })

  it('영국은 마일이지만 주유는 리터', () => {
    expect(british.volume(32.45)).toBe('32.45 L')
    expect(british.efficiencyUnit).toBe('mpg')
  })

  it('기록의 통화가 사용자 통화와 달라도 그 기록의 통화로', () => {
    // 한국어 화면에서 달러 기록
    expect(korean.money(4567, 'USD')).toContain('45.67')
    expect(korean.moneyParts(4567, 'USD')).toEqual({ value: '45.67', unit: 'USD' })
    expect(korean.moneyParts(50000)).toEqual({ value: '50,000', unit: '원' })
  })
})

describe('축 눈금 줄이기', () => {
  it('한국어는 만·억, 영어는 K·M', () => {
    expect(korean.compact(320_000)).toBe('32만')
    expect(korean.compact(150_000_000)).toBe('1.5억')
    expect(american.compact(320_000)).toBe('320K')
  })

  it('1만 미만은 줄이지 않는다', () => {
    // 1,873 을 1.9천으로 줄이면 눈금과 실제 값 불일치
    expect(korean.compact(1_873)).toBe('1,873')
    expect(american.compact(9_999)).toBe('9,999')
  })

  it('금액 눈금은 주 단위로', () => {
    // 3,200,000 센트 = $32,000
    expect(american.compactMoney(3_200_000)).toBe('32K')
  })
})

describe('날짜 표기', () => {
  it('한국어는 점, 미국은 월/일/년', () => {
    expect(korean.date('2026-07-15')).toBe('2026. 7. 15.')
    expect(american.date('2026-07-15')).toBe('7/15/2026')
  })

  it('월 이름', () => {
    expect(korean.month('2026-07')).toBe('2026년 7월')
    expect(american.month('2026-01')).toBe('January 2026')
  })
})

describe('todayString — UTC 함정', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  /** 인자는 지역 시각. 표준시대와 무관한 결과 */
  function freezeLocal(year: number, month: number, day: number, hour: number, minute: number) {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(year, month - 1, day, hour, minute))
  }

  it('자정 직후에도 오늘이다', () => {
    // toISOString() 은 UTC 기준. 동쪽(UTC+)에서 하루 전
    freezeLocal(2026, 7, 15, 0, 0)
    expect(todayString()).toBe('2026-07-15')
  })

  it('자정 직전에도 오늘이다', () => {
    // 서쪽(UTC-)에서는 하루 뒤
    freezeLocal(2026, 7, 15, 23, 59)
    expect(todayString()).toBe('2026-07-15')
  })

  it('한 자리 월·일에 0 을 채운다', () => {
    // 백엔드 LocalDate·<input type=date> 공통 형식 YYYY-MM-DD
    freezeLocal(2026, 1, 5, 12, 0)
    expect(todayString()).toBe('2026-01-05')
  })

  it('시간대를 주면 그 지역의 오늘', () => {
    // UTC 2026-09-29 12:00 = 서울 29일 21시 = 오클랜드 30일 01시. 서버 UserToday 와 같은 기준
    vi.useFakeTimers()
    vi.setSystemTime(new Date(Date.UTC(2026, 8, 29, 12, 0)))
    expect(todayString('Asia/Seoul')).toBe('2026-09-29')
    expect(todayString('Pacific/Auckland')).toBe('2026-09-30')
  })
})
