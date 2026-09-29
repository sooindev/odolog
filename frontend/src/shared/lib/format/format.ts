// 숫자·금액·날짜·단위 표기. 사용자 설정(언어·단위·통화)을 받아 만든 한 벌
// 화면은 useI18n().f 로 받음. 여기서는 React 를 모름

import type { Language, UnitSystem } from '@/shared/lib/locale/preferences'
import { fractionDigits, fromMinor } from '@/shared/lib/money/money'
import {
  distanceUnit,
  efficiencyUnit,
  fromKm,
  fromKmPerLiter,
  fromLiters,
  volumeUnit,
} from '@/shared/lib/units/units'

export interface FormatSettings {
  /** Intl 로케일 태그. ko-KR, en-US, en-GB … */
  locale: string
  language: Language
  unitSystem: UnitSystem
  /** 사용자 통화. 금액마다 따로 넘기지 않을 때의 기본 */
  currency: string
}

/** 이 아래로는 줄이지 않음. 1,873 을 1.9천으로 적으면 눈금과 값이 어긋남 */
const COMPACT_FROM = 10_000

export function createFormatter(settings: FormatSettings) {
  const { locale, language, unitSystem } = settings

  function number(value: number, digits?: number) {
    return new Intl.NumberFormat(
      locale,
      digits === undefined ? undefined : { minimumFractionDigits: digits, maximumFractionDigits: digits },
    ).format(value)
  }

  // 한국어는 붙여 씀(45,000km). 영어는 띄움(45,000 mi)
  function withUnit(value: string, unit: string) {
    return language === 'KO' ? `${value}${unit}` : `${value} ${unit}`
  }

  // 원화를 한국어로 볼 때만 "50,000원". 그 밖은 Intl 기호(₩50,000·$45.67)
  function wonStyle(currency: string) {
    return language === 'KO' && currency === 'KRW'
  }

  function compact(value: number) {
    if (Math.abs(value) < COMPACT_FROM) {
      return number(value)
    }
    return new Intl.NumberFormat(locale, { notation: 'compact', maximumFractionDigits: 1 }).format(value)
  }

  /** 'YYYY-MM-DD' 는 달력 날짜. UTC 로 고정해야 시간대 따라 하루 밀리지 않음 */
  function calendar(value: string) {
    const [year, month, day = 1] = value.split('-').map(Number)
    return new Date(Date.UTC(year, month - 1, day))
  }

  return {
    number,
    compact,

    distanceUnit: distanceUnit(unitSystem),
    volumeUnit: volumeUnit(unitSystem),
    efficiencyUnit: efficiencyUnit(unitSystem),

    /** km → 화면 단위 정수 표기(단위 없음). 히어로 숫자용 */
    distanceNumber: (km: number) => number(Math.round(fromKm(unitSystem, km))),
    distance: (km: number) => withUnit(number(Math.round(fromKm(unitSystem, km))), distanceUnit(unitSystem)),

    volumeNumber: (liters: number) => number(fromLiters(unitSystem, liters), 2),
    volume: (liters: number) => `${number(fromLiters(unitSystem, liters), 2)} ${volumeUnit(unitSystem)}`,

    /** km/L → 화면 표기 숫자. 계산 불가(L/100km 에서 0)는 — */
    efficiencyNumber: (kmPerLiter: number, digits = 2) => {
      const value = fromKmPerLiter(unitSystem, kmPerLiter)
      return Number.isFinite(value) ? number(value, digits) : '—'
    },
    efficiency: (kmPerLiter: number, digits = 2) => {
      const value = fromKmPerLiter(unitSystem, kmPerLiter)
      return `${Number.isFinite(value) ? number(value, digits) : '—'} ${efficiencyUnit(unitSystem)}`
    },

    /** 최소 단위 금액. 통화 생략 시 사용자 통화 */
    money: (minor: number, currency = settings.currency) => {
      if (wonStyle(currency)) {
        return `${number(minor)}원`
      }
      return new Intl.NumberFormat(locale, { style: 'currency', currency }).format(fromMinor(minor, currency))
    },

    /** 히어로 숫자와 작은 단위를 따로 그릴 때 */
    moneyParts: (minor: number, currency = settings.currency) => ({
      value: number(fromMinor(minor, currency), fractionDigits(currency)),
      unit: wonStyle(currency) ? '원' : currency,
    }),

    /** 차트 축 눈금. 최소 단위를 주 단위로 바꿔 줄임 */
    compactMoney: (minor: number, currency = settings.currency) => compact(fromMinor(minor, currency)),

    /** 'YYYY-MM-DD'. 한국어 2026. 7. 15. · 미국 7/15/2026 */
    date: (value: string) =>
      new Intl.DateTimeFormat(locale, { year: 'numeric', month: 'numeric', day: 'numeric', timeZone: 'UTC' })
        .format(calendar(value)),

    /** 'YYYY-MM'. 한국어 2026년 7월 · 영어 July 2026 */
    month: (value: string) =>
      new Intl.DateTimeFormat(locale, { year: 'numeric', month: 'long', timeZone: 'UTC' })
        .format(calendar(value)),
  }
}

export type Formatter = ReturnType<typeof createFormatter>

/**
 * 오늘 날짜 YYYY-MM-DD. 시간대를 주면 그 지역의 오늘(서버 판정과 같은 기준)
 * 없으면 브라우저 지역. toISOString() 은 UTC 라 한국에서 오전 9시 전 하루 차이
 */
export function todayString(timeZone?: string) {
  const now = new Date()

  if (timeZone !== undefined) {
    try {
      // en-CA 는 YYYY-MM-DD 순서
      return new Intl.DateTimeFormat('en-CA', {
        timeZone,
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
      }).format(now)
    } catch {
      // 모르는 시간대면 브라우저 지역으로
    }
  }

  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')

  return `${now.getFullYear()}-${month}-${day}`
}
