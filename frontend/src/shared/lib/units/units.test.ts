import { describe, expect, it } from 'vitest'

import {
  distanceUnit,
  efficiencyUnit,
  fromKm,
  fromKmPerLiter,
  fromLiters,
  toKm,
  toLiters,
  volumeUnit,
} from './units'

describe('단위 체계', () => {
  it('나라별 조합', () => {
    expect([distanceUnit('KM_PER_L'), volumeUnit('KM_PER_L'), efficiencyUnit('KM_PER_L')])
      .toEqual(['km', 'L', 'km/L'])
    expect([distanceUnit('MPG_US'), volumeUnit('MPG_US'), efficiencyUnit('MPG_US')])
      .toEqual(['mi', 'gal', 'mpg'])
    // 영국은 주유만 리터
    expect([distanceUnit('MPG_UK'), volumeUnit('MPG_UK'), efficiencyUnit('MPG_UK')])
      .toEqual(['mi', 'L', 'mpg'])
    expect(efficiencyUnit('L_PER_100KM')).toBe('L/100km')
  })
})

describe('거리 왕복', () => {
  it('마일로 적은 값은 km 로 저장했다가 같은 마일로 돌아온다', () => {
    // 저장이 km 정수라 반올림 오차가 마일 한 자리를 넘으면 안 됨
    for (const miles of [0, 1, 12_345, 99_999, 1_234_567]) {
      expect(Math.round(fromKm('MPG_US', toKm('MPG_US', miles)))).toBe(miles)
    }
  })

  it('km 체계는 그대로', () => {
    expect(toKm('KM_PER_L', 45_000)).toBe(45_000)
    expect(fromKm('L_PER_100KM', 45_000)).toBe(45_000)
  })
})

describe('부피 왕복', () => {
  it('US 갤런은 소수 2자리까지 되돌아온다', () => {
    for (const gallons of [0.01, 9.87, 12.5, 25]) {
      expect(Math.round(fromLiters('MPG_US', toLiters('MPG_US', gallons)) * 100) / 100).toBe(gallons)
    }
  })

  it('영국은 리터 그대로', () => {
    expect(toLiters('MPG_UK', 32.45)).toBe(32.45)
  })
})

describe('연비 표기', () => {
  it('km/L 20 은 L/100km 5, US 약 47mpg, 영국 약 56mpg', () => {
    expect(fromKmPerLiter('L_PER_100KM', 20)).toBeCloseTo(5)
    expect(fromKmPerLiter('MPG_US', 20)).toBeCloseTo(47.04, 1)
    expect(fromKmPerLiter('MPG_UK', 20)).toBeCloseTo(56.5, 1)
  })

  it('0 km/L 은 L/100km 로 뒤집을 수 없다', () => {
    expect(fromKmPerLiter('L_PER_100KM', 0)).toBeNaN()
  })
})
