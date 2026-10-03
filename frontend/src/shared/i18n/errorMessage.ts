// 잡은 오류 → 화면 문구. 서버 code 로 언어별 문구를 고르고, 모르면 서버 원문·기본 문구 순

import { ApiError, NETWORK_ERROR_STATUS } from '@/shared/api/client'
import type { Messages } from '@/shared/i18n/messages/ko'

export function errorMessage(caught: unknown, t: Messages, fallback: string): string {
  if (!(caught instanceof ApiError)) {
    return fallback
  }

  if (caught.status === NETWORK_ERROR_STATUS) {
    return t.errors.network
  }

  if (caught.code !== null) {
    // 사전에 없는 코드(새 백엔드 + 옛 화면). 함수로 부르면 TypeError
    const entry = t.errors.codes[caught.code] as (typeof t.errors.codes)[keyof typeof t.errors.codes] | undefined
    if (entry === undefined) {
      return fallback
    }
    if (typeof entry === 'string') {
      return entry
    }
    if (caught.code.startsWith('TOO_MANY_')) {
      return (entry as (minutes: number) => string)(caught.retryAfterMinutes ?? 10)
    }
    // 칸을 모르면 주어 없는 문장 대신 호출부 문구
    if (caught.field === null) {
      return fallback
    }
    // 칸 이름은 화면 이름으로. 가져오기의 vehicles[0].fuelRecords[3].liters 는 마지막 조각으로
    // 끝내 모르는 칸은 서버 이름(영문 경로)을 문장에 넣지 않고 호출부 문구
    const field = t.errors.fields[caught.field] ?? t.errors.fields[lastSegment(caught.field)]
    if (field === undefined) {
      return fallback
    }
    return (entry as (field: string) => string)(field)
  }

  return fallback
}

/** 'a[0].b[3].liters' → 'liters' */
function lastSegment(path: string) {
  return path.split('.').pop()!.replace(/\[\d+\]$/, '')
}
