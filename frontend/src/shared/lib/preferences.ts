// 가입 시 브라우저에서 읽는 초기 설정. 백엔드 User 설정 넷과 짝
// 모르는 값은 undefined. 보내지 않으면 서버 기본값

export type Language = 'KO' | 'EN'
export type UnitSystem = 'KM_PER_L' | 'L_PER_100KM' | 'MPG_US' | 'MPG_UK'

export interface Preferences {
  language?: Language
  timeZone?: string
  currency?: string
  unitSystem?: UnitSystem
}

/** 지역별 통화·단위. 영어권 우선, 목록 밖은 추정하지 않음 */
const REGIONS: Record<string, { currency: string; unitSystem: UnitSystem }> = {
  KR: { currency: 'KRW', unitSystem: 'KM_PER_L' },
  US: { currency: 'USD', unitSystem: 'MPG_US' },
  GB: { currency: 'GBP', unitSystem: 'MPG_UK' },
  CA: { currency: 'CAD', unitSystem: 'L_PER_100KM' },
  AU: { currency: 'AUD', unitSystem: 'L_PER_100KM' },
  NZ: { currency: 'NZD', unitSystem: 'L_PER_100KM' },
  IE: { currency: 'EUR', unitSystem: 'L_PER_100KM' },
}

/** 언어 태그(en-US)와 시간대에서 설정 추정. 순수 함수라 테스트 가능 */
export function preferencesFrom(languageTag: string, timeZone: string | undefined): Preferences {
  let language: string | undefined
  let region: string | undefined
  try {
    // region 은 태그에 적힌 것만. maximize() 는 en → US 로 추측해 영국 사용자도 달러
    const locale = new Intl.Locale(languageTag)
    language = locale.language
    region = locale.region
  } catch {
    // 깨진 태그는 언어·지역 모름
  }

  const regional = region ? REGIONS[region] : undefined

  return {
    // 한국어 외에는 영어. 번역이 두 벌뿐
    language: language === undefined ? undefined : language === 'ko' ? 'KO' : 'EN',
    timeZone: timeZone || undefined,
    currency: regional?.currency,
    unitSystem: regional?.unitSystem,
  }
}

/** 지금 브라우저의 설정 */
export function detectPreferences(): Preferences {
  return preferencesFrom(navigator.language, Intl.DateTimeFormat().resolvedOptions().timeZone)
}
