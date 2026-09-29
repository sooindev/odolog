// 잡은 오류 → 화면 문구. 서버 code 로 언어별 문구를 고르고, 모르면 서버 원문·기본 문구 순

import { ApiError, NETWORK_ERROR_STATUS } from '@/shared/api/client/client'
import type { Messages } from '@/shared/i18n/messages/ko'

export function errorMessage(caught: unknown, t: Messages, fallback: string): string {
  if (!(caught instanceof ApiError)) {
    return fallback
  }

  if (caught.status === NETWORK_ERROR_STATUS) {
    return t.errors.network
  }

  if (caught.code !== null) {
    const entry = t.errors.codes[caught.code]
    if (typeof entry === 'string') {
      return entry
    }
    if (caught.code.startsWith('TOO_MANY_')) {
      return (entry as (minutes: number) => string)(caught.retryAfterMinutes ?? 10)
    }
    // 칸 이름은 화면 이름으로. 모르는 칸은 서버가 준 이름 그대로
    const field = caught.field === null ? '' : (t.errors.fields[caught.field] ?? caught.field)
    return (entry as (field: string) => string)(field)
  }

  return fallback
}
