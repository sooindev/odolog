import { describe, expect, it } from 'vitest'

import { fractionDigits, fromMinor, inputStep, maxMajor, toMinor } from './money'

describe('통화의 최소 단위', () => {
  it('원화는 소수 자리가 없고 달러는 둘', () => {
    expect(fractionDigits('KRW')).toBe(0)
    expect(fractionDigits('USD')).toBe(2)
    expect(fractionDigits('JPY')).toBe(0)
  })

  it('입력한 주 단위를 최소 단위 정수로', () => {
    // 45.67 * 100 = 4567.000000000001. 반올림 필수
    expect(toMinor(45.67, 'USD')).toBe(4567)
    expect(toMinor(50_000, 'KRW')).toBe(50_000)
  })

  it('최소 단위를 입력칸 값으로 되돌린다', () => {
    expect(fromMinor(4567, 'USD')).toBe(45.67)
    expect(fromMinor(50_000, 'KRW')).toBe(50_000)
  })

  it('step 과 상한이 통화를 따른다', () => {
    expect(inputStep('USD')).toBe(0.01)
    expect(inputStep('KRW')).toBe(1)
    // 서버 상한은 최소 단위 1억 — 원화 1억, 달러 100만
    expect(maxMajor('KRW')).toBe(100_000_000)
    expect(maxMajor('USD')).toBe(1_000_000)
  })
})
