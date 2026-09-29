import { useEffect, useMemo } from 'react'
import type { ReactNode } from 'react'

import { useAuth } from '@/features/auth/context/definition/AuthContext'
import { I18nContext } from '@/shared/i18n/context/I18nContext'
import { en } from '@/shared/i18n/messages/en'
import { ko } from '@/shared/i18n/messages/ko'
import { createFormatter } from '@/shared/lib/format/format'
import { detectPreferences } from '@/shared/lib/locale/preferences'
import type { Language } from '@/shared/lib/locale/preferences'

/** 브라우저 태그가 같은 언어면 그 지역 표기(en-GB 의 날짜 순서)를 살림 */
function localeFor(language: Language, browserTag: string) {
  const wanted = language === 'KO' ? 'ko' : 'en'
  return browserTag.toLowerCase().startsWith(wanted) ? browserTag : language === 'KO' ? 'ko-KR' : 'en-US'
}

/**
 * 언어·단위·통화·시간대 공급. 로그인하면 계정 설정, 아니면 브라우저 추정값
 * 사용자 정보를 알아야 해서 shared 가 아니라 app 층
 */
export function I18nProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const detected = useMemo(() => detectPreferences(), [])

  const language = user?.language ?? detected.language ?? 'EN'
  const timeZone = user?.timeZone ?? detected.timeZone ?? 'UTC'
  // 추정 못 한 지역은 언어의 기본 조합
  const currency = user?.currency ?? detected.currency ?? (language === 'KO' ? 'KRW' : 'USD')
  const unitSystem = user?.unitSystem ?? detected.unitSystem ?? (language === 'KO' ? 'KM_PER_L' : 'L_PER_100KM')

  const value = useMemo(() => {
    const locale = localeFor(language, navigator.language)
    return {
      language,
      locale,
      timeZone,
      currency,
      unitSystem,
      t: language === 'KO' ? ko : en,
      f: createFormatter({ locale, language, unitSystem, currency }),
    }
  }, [language, timeZone, currency, unitSystem])

  // 문서 언어·제목. 스크린리더 발음과 번역 제안의 기준
  useEffect(() => {
    document.documentElement.lang = language === 'KO' ? 'ko' : 'en'
    document.title = value.t.app.name
    document.querySelector('meta[name="description"]')?.setAttribute('content', value.t.app.description)
  }, [language, value.t])

  return <I18nContext value={value}>{children}</I18nContext>
}
