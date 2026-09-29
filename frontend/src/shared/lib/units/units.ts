// 거리·부피·연비 단위 변환. 저장은 언제나 km·L, 변환은 화면에서만
// 서버의 판정(연비 50 초과 등)이 km·L 기준이라 저장 단위를 바꾸지 않음

import type { UnitSystem } from '@/shared/lib/locale/preferences'

export const KM_PER_MILE = 1.609344
export const LITERS_PER_US_GALLON = 3.785411784
export const LITERS_PER_UK_GALLON = 4.54609

export type DistanceUnit = 'km' | 'mi'
export type VolumeUnit = 'L' | 'gal'
export type EfficiencyUnit = 'km/L' | 'L/100km' | 'mpg'

export function distanceUnit(system: UnitSystem): DistanceUnit {
  return system === 'MPG_US' || system === 'MPG_UK' ? 'mi' : 'km'
}

/** 영국은 주유를 리터로, 연비만 영국 갤런 */
export function volumeUnit(system: UnitSystem): VolumeUnit {
  return system === 'MPG_US' ? 'gal' : 'L'
}

export function efficiencyUnit(system: UnitSystem): EfficiencyUnit {
  if (system === 'L_PER_100KM') return 'L/100km'
  if (system === 'MPG_US' || system === 'MPG_UK') return 'mpg'
  return 'km/L'
}

/** 작을수록 좋은 표기. 추이 그래프의 방향 설명용 */
export function lowerIsBetter(system: UnitSystem) {
  return system === 'L_PER_100KM'
}

/** km → 화면 단위. 반올림 안 함 */
export function fromKm(system: UnitSystem, km: number) {
  return distanceUnit(system) === 'mi' ? km / KM_PER_MILE : km
}

/** 화면 단위 → km 정수. km 가 마일보다 촘촘해 왕복해도 원래 마일로 돌아옴 */
export function toKm(system: UnitSystem, value: number) {
  return Math.round(distanceUnit(system) === 'mi' ? value * KM_PER_MILE : value)
}

/** L → 화면 단위. 반올림 안 함 */
export function fromLiters(system: UnitSystem, liters: number) {
  return volumeUnit(system) === 'gal' ? liters / LITERS_PER_US_GALLON : liters
}

/** 화면 단위 → L, 소수 2자리(서버 BigDecimal(6,2)) */
export function toLiters(system: UnitSystem, value: number) {
  const liters = volumeUnit(system) === 'gal' ? value * LITERS_PER_US_GALLON : value
  return Math.round(liters * 100) / 100
}

/** km/L → 화면 표기. L/100km 는 뒤집힘(0 이면 계산 불가) */
export function fromKmPerLiter(system: UnitSystem, kmPerLiter: number) {
  switch (system) {
    case 'L_PER_100KM':
      return kmPerLiter > 0 ? 100 / kmPerLiter : Number.NaN
    case 'MPG_US':
      return (kmPerLiter * LITERS_PER_US_GALLON) / KM_PER_MILE
    case 'MPG_UK':
      return (kmPerLiter * LITERS_PER_UK_GALLON) / KM_PER_MILE
    default:
      return kmPerLiter
  }
}
