import { afterEach, describe, expect, it, vi } from 'vitest'

import { daysInMonth, join, parse } from './date-parts'

describe('daysInMonth — 윤년을 직접 계산하지 않는다', () => {
  it('달마다 마지막 날이 다르다', () => {
    expect(daysInMonth(2026, 1)).toBe(31)
    expect(daysInMonth(2026, 4)).toBe(30)
  })

  it('윤년 규칙을 Date 에 맡긴다', () => {
    expect(daysInMonth(2026, 2)).toBe(28)
    expect(daysInMonth(2024, 2)).toBe(29)
    // 100 으로 나뉘면 윤년이 아니고, 400 으로 나뉘면 윤년이다
    expect(daysInMonth(2100, 2)).toBe(28)
    expect(daysInMonth(2000, 2)).toBe(29)
  })
})

describe('join — 일수가 줄어드는 달로 옮길 때', () => {
  it('넘치는 일을 그 달의 마지막 날로 자른다', () => {
    // 휠에서 1/31 을 두고 월만 굴리면 2/31 이라는 날짜가 만들어짐
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

describe('parse — 빈 값·깨진 값', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('정상 값을 그대로 쪼갠다', () => {
    expect(parse('2026-07-15')).toEqual({ year: 2026, month: 7, day: 15 })
  })

  it('빈 값이면 오늘로 되돌린다', () => {
    // 네이티브 date 입력은 지우면 빈 문자열을 준다. 그대로 두면 휠에 NaN 이 찍힘
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 6, 15, 12, 0))

    expect(parse('')).toEqual({ year: 2026, month: 7, day: 15 })
    expect(parse('2026-07')).toEqual({ year: 2026, month: 7, day: 15 })
    expect(parse('오늘')).toEqual({ year: 2026, month: 7, day: 15 })
  })
})
