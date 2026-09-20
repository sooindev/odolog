import { afterEach, describe, expect, it, vi } from 'vitest'

import {
  formatCompact,
  formatDate,
  formatKm,
  formatMonth,
  formatNumber,
  formatWon,
  todayString,
} from './format'

describe('숫자 표기', () => {
  it('천 단위로 끊는다', () => {
    expect(formatNumber(45000)).toBe('45,000')
    expect(formatNumber(0)).toBe('0')
  })

  it('단위를 붙인다', () => {
    expect(formatKm(45000)).toBe('45,000km')
    expect(formatWon(50000)).toBe('50,000원')
  })
})

describe('formatCompact — 축 눈금 전용', () => {
  it('만·억으로 줄인다', () => {
    expect(formatCompact(320_000)).toBe('32만')
    expect(formatCompact(150_000_000)).toBe('1.5억')
  })

  it('소수점 아래가 0 이면 지운다', () => {
    expect(formatCompact(10_000)).toBe('1만')
    expect(formatCompact(15_000)).toBe('1.5만')
  })

  it('만 미만은 줄이지 않는다', () => {
    // 1,873 을 2천으로 줄이면 축 눈금이 실제 값과 어긋남
    expect(formatCompact(9_999)).toBe('9,999')
    expect(formatCompact(1_873)).toBe('1,873')
  })
})

describe('날짜 표기', () => {
  it('월을 읽을 수 있게 편다', () => {
    expect(formatMonth('2026-07')).toBe('2026년 7월')
    // 앞의 0 을 떼야 "2026년 01월" 이 안 나옴
    expect(formatMonth('2026-01')).toBe('2026년 1월')
  })

  it('날짜를 점으로 끊는다', () => {
    expect(formatDate('2026-07-15')).toBe('2026. 7. 15.')
    expect(formatDate('2026-01-05')).toBe('2026. 1. 5.')
  })
})

describe('todayString — UTC 함정', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  /** 인자가 지역 시각으로 해석되므로 어느 표준시대에서 돌려도 결과가 같음 */
  function freezeLocal(year: number, month: number, day: number, hour: number, minute: number) {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(year, month - 1, day, hour, minute))
  }

  it('자정 직후에도 오늘이다', () => {
    // toISOString() 은 UTC 기준이라 동쪽(UTC+) 에서 하루 전으로 밀림
    freezeLocal(2026, 7, 15, 0, 0)
    expect(todayString()).toBe('2026-07-15')
  })

  it('자정 직전에도 오늘이다', () => {
    // 같은 이유로 서쪽(UTC-) 에서는 하루 뒤로 밀림
    freezeLocal(2026, 7, 15, 23, 59)
    expect(todayString()).toBe('2026-07-15')
  })

  it('한 자리 월·일에 0 을 채운다', () => {
    // 백엔드 LocalDate 와 <input type="date"> 가 둘 다 YYYY-MM-DD 를 요구
    freezeLocal(2026, 1, 5, 12, 0)
    expect(todayString()).toBe('2026-01-05')
  })
})
