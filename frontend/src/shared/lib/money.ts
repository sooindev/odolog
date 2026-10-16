// 금액은 통화의 최소 단위 정수(원·센트). 입력칸은 주 단위(달러)
// 백엔드 InputLimits.MAX_AMOUNT 와 같은 숫자(최소 단위 기준)

import { MAX_AMOUNT } from '@/shared/lib/limits'

/**
 * ISO 4217 의 소수 자리 중 2 가 아닌 것. 브라우저 ICU 에 묻지 않음
 * 브라우저마다 값이 달라(크롬은 IDR·HUF 를 0, ISO 는 2) 같은 저장값이 브라우저에 따라 100배로 읽힘
 */
const MINOR_UNITS: Record<string, number> = {
  BIF: 0, CLP: 0, DJF: 0, GNF: 0, ISK: 0, JPY: 0, KMF: 0, KRW: 0, PYG: 0, RWF: 0,
  UGX: 0, UYI: 0, VND: 0, VUV: 0, XAF: 0, XOF: 0, XPF: 0,
  BHD: 3, IQD: 3, JOD: 3, KWD: 3, LYD: 3, OMR: 3, TND: 3,
  CLF: 4, UYW: 4,
}

/** 소수 자리. KRW 0, USD 2. 표에 없는 코드는 2(ISO 4217 의 대부분) */
export function fractionDigits(currency: string) {
  return MINOR_UNITS[currency] ?? 2
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
