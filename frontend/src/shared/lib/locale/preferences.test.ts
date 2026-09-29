import { describe, expect, it } from 'vitest'

import { preferencesFrom } from './preferences'

describe('브라우저 설정 추정', () => {
  it('미국·영국·캐나다는 각자의 통화와 단위', () => {
    expect(preferencesFrom('en-US', 'America/Chicago')).toEqual({
      language: 'EN',
      timeZone: 'America/Chicago',
      currency: 'USD',
      unitSystem: 'MPG_US',
    })
    expect(preferencesFrom('en-GB', 'Europe/London').unitSystem).toBe('MPG_UK')
    expect(preferencesFrom('en-CA', 'America/Toronto').unitSystem).toBe('L_PER_100KM')
  })

  it('한국은 지금과 같은 기본값', () => {
    expect(preferencesFrom('ko-KR', 'Asia/Seoul')).toEqual({
      language: 'KO',
      timeZone: 'Asia/Seoul',
      currency: 'KRW',
      unitSystem: 'KM_PER_L',
    })
  })

  it('지역이 없으면 통화·단위를 추측하지 않는다', () => {
    // en 만으로 US 를 고르면 영국 사용자가 달러·US 갤런으로 시작
    const result = preferencesFrom('en', 'Europe/London')

    expect(result.language).toBe('EN')
    expect(result.currency).toBeUndefined()
    expect(result.unitSystem).toBeUndefined()
  })

  it('번역이 없는 언어는 영어, 목록 밖 지역은 추측하지 않는다', () => {
    const result = preferencesFrom('fr-FR', 'Europe/Paris')

    expect(result.language).toBe('EN')
    expect(result.currency).toBeUndefined()
  })

  it('깨진 태그와 빈 시간대는 보내지 않는다', () => {
    expect(preferencesFrom('!!', '')).toEqual({
      language: undefined,
      timeZone: undefined,
      currency: undefined,
      unitSystem: undefined,
    })
  })
})
