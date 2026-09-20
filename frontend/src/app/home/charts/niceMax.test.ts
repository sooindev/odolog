import { describe, expect, it } from 'vitest'

import { niceMax } from './niceMax'

describe('niceMax — 축 눈금 올림', () => {
  it('1·2·5 × 10ⁿ 으로 올린다', () => {
    // 문서에 적어 두고 node 로만 확인하던 세 값
    expect(niceMax(873)).toBe(1_000)
    expect(niceMax(45_000)).toBe(50_000)
    expect(niceMax(120_000)).toBe(200_000)
  })

  it('이미 눈금 위의 값은 올리지 않는다', () => {
    // 1,000 을 2,000 으로 올리면 막대가 축의 절반까지밖에 안 자람
    expect(niceMax(1_000)).toBe(1_000)
    expect(niceMax(20_000)).toBe(20_000)
    expect(niceMax(50_000)).toBe(50_000)
  })

  it('각 구간의 경계를 넘으면 다음 눈금으로 간다', () => {
    expect(niceMax(1_001)).toBe(2_000)
    expect(niceMax(2_001)).toBe(5_000)
    expect(niceMax(5_001)).toBe(10_000)
  })

  it('기록이 없어도 0 으로 나누지 않는다', () => {
    // 막대 높이가 값/max 라 max 가 0 이면 NaN% 가 됨
    expect(niceMax(0)).toBe(1)
    expect(niceMax(-100)).toBe(1)
  })
})
