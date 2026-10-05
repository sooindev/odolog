import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactNode } from 'react'

import { I18nContext } from '@/shared/i18n/I18nContext'
import type { I18nValue } from '@/shared/i18n/I18nContext'
import { ko } from '@/shared/i18n/messages/ko'
import { createFormatter } from '@/shared/lib/format'

/** 화면 테스트 전용. 한국어·km·원화 고정, 테스트마다 새 조회 캐시 */
export function renderWithProviders(ui: ReactNode) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const i18n: I18nValue = {
    language: 'KO',
    locale: 'ko-KR',
    timeZone: 'Asia/Seoul',
    currency: 'KRW',
    unitSystem: 'KM_PER_L',
    t: ko,
    f: createFormatter({ locale: 'ko-KR', language: 'KO', unitSystem: 'KM_PER_L', currency: 'KRW' }),
  }

  const result = render(
    <QueryClientProvider client={queryClient}>
      <I18nContext value={i18n}>{ui}</I18nContext>
    </QueryClientProvider>,
  )
  return { ...result, queryClient }
}
