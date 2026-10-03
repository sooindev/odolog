// 금액은 통화의 최소 단위 정수(원·센트). 입력칸은 주 단위(달러)
// 백엔드 InputLimits.MAX_AMOUNT 와 같은 숫자(최소 단위 기준)

import { MAX_AMOUNT } from '@/shared/lib/limits'

/** 소수 자리. KRW 0, USD 2. 모르는 코드는 2 */
export function fractionDigits(currency: string) {
  try {
    return new Intl.NumberFormat('en', { style: 'currency', currency }).resolvedOptions()
      .maximumFractionDigits ?? 2
  } catch {
    return 2
  }
}

/** 45.67 USD → 4567 */
export function toMinor(major: number, currency: string) {
  return Math.round(major * 10 ** fractionDigits(currency))
}

/** 4567 USD → 45.67 */
export function fromMinor(minor: number, currency: string) {
  return minor / 10 ** fractionDigits(currency)
}

/** 입력칸 step. KRW 1, USD 0.01 */
export function inputStep(currency: string) {
  return 1 / 10 ** fractionDigits(currency)
}

/** 입력칸 상한(주 단위) */
export function maxMajor(currency: string) {
  return fromMinor(MAX_AMOUNT, currency)
}
