import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { daysInMonth, join, lastSelectableDay, lastSelectableMonth, parse } from './date-parts'

describe('daysInMonth — 윤년을 직접 계산하지 않는다', () => {
  it('달마다 마지막 날이 다르다', () => {
    expect(daysInMonth(2026, 1)).toBe(31)
    expect(daysInMonth(2026, 4)).toBe(30)
  })

  it('윤년 규칙을 Date 에 맡긴다', () => {
    expect(daysInMonth(2026, 2)).toBe(28)
    expect(daysInMonth(2024, 2)).toBe(29)
    // 100 의 배수는 평년, 400 의 배수는 윤년
    expect(daysInMonth(2100, 2)).toBe(28)
    expect(daysInMonth(2000, 2)).toBe(29)
  })
})

describe('join — 일수가 줄어드는 달로 옮길 때', () => {
  // join 은 오늘 이후를 자르므로 시각 고정
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 11, 31, 12, 0))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('넘치는 일을 그 달의 마지막 날로 자른다', () => {
    // 1/31 에서 월만 2월로 굴린 경우
    expect(join(2026, 2, 31)).toBe('2026-02-28')
    expect(join(2024, 2, 31)).toBe('2024-02-29')
    expect(join(2026, 4, 31)).toBe('2026-04-30')
  })

  it('들어가는 일은 그대로 둔다', () => {
    expect(join(2026, 1, 31)).toBe('2026-01-31')
  })

  it('한 자리 월·일에 0 을 채운다', () => {
    expect(join(2026, 1, 5)).toBe('2026-01-05')
  })
})

describe('오늘 이후는 고를 수 없다 — 휠이 칸을 직접 만들기 때문', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 8, 23, 12, 0)) // 2026-09-23
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('올해는 이번 달까지만, 지난 해는 12월까지', () => {
    expect(lastSelectableMonth(2026)).toBe(9)
    expect(lastSelectableMonth(2025)).toBe(12)
  })

  it('올해 이번 달은 오늘까지, 그 밖은 그 달의 마지막 날까지', () => {
    expect(lastSelectableDay(2026, 9)).toBe(23)
    expect(lastSelectableDay(2026, 8)).toBe(31)
    expect(lastSelectableDay(2025, 9)).toBe(30)
  })

  it('미래로 굴린 년·월·일을 순서대로 자른다', () => {
    // 2020-12-25 에서 년만 올해로 → 9월 23일로 절단
    expect(join(2026, 12, 25)).toBe('2026-09-23')
    // 이번 달 안의 미래 날짜
    expect(join(2026, 9, 30)).toBe('2026-09-23')
    // 지난 달·지난 해는 그대로
    expect(join(2026, 8, 31)).toBe('2026-08-31')
    expect(join(2025, 12, 31)).toBe('2025-12-31')
  })
})

describe('parse — 빈 값·깨진 값', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('정상 값을 그대로 쪼갠다', () => {
    expect(parse('2026-07-15')).toEqual({ year: 2026, month: 7, day: 15 })
  })

  it('빈 값이면 오늘로 되돌린다', () => {
    // 지운 네이티브 입력의 빈 문자열
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 6, 15, 12, 0))

    expect(parse('')).toEqual({ year: 2026, month: 7, day: 15 })
    expect(parse('2026-07')).toEqual({ year: 2026, month: 7, day: 15 })
    expect(parse('오늘')).toEqual({ year: 2026, month: 7, day: 15 })
  })
})
