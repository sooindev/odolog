import { createContext, useContext } from 'react'

import type { Formatter } from '@/shared/lib/format/format'
import type { Language, UnitSystem } from '@/shared/lib/locale/preferences'
import type { Messages } from '@/shared/i18n/messages/ko'

/** 화면이 쓰는 언어·단위·통화·시간대 한 벌. 로그인하면 계정 설정, 아니면 브라우저 값 */
export interface I18nValue {
  language: Language
  /** Intl 로케일. ko-KR · en-US · en-GB … */
  locale: string
  timeZone: string
  currency: string
  unitSystem: UnitSystem
  /** 문구 */
  t: Messages
  /** 숫자·금액·날짜·단위 표기 */
  f: Formatter
}

// 기본값 null. Provider 누락 시 즉시 오류
export const I18nContext = createContext<I18nValue | null>(null)

export function useI18n() {
  const value = useContext(I18nContext)

  if (value === null) {
    throw new Error('useI18n() must be used inside <I18nProvider>.')
  }

  return value
}
