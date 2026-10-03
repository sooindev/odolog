// 입력값 상한. 백엔드 InputLimits 와 같은 숫자(의도한 중복)
// 한쪽만 고치면 화면은 되는데 저장은 안 되는 상태

import type { Messages } from '@/shared/i18n/messages/ko'

/** 주행거리 상한(km) */
export const MAX_ODOMETER = 2_000_000

/** 금액 상한(통화의 최소 단위). 원화 1억, 달러 100만 */
export const MAX_AMOUNT = 100_000_000

/** 비밀번호 상한(UTF-8 바이트). BCrypt 한계 */
export const MAX_PASSWORD_BYTES = 72

/** 요청 본문 상한(바이트). 백엔드 InputLimits.MAX_BODY_BYTES 와 같은 값. 넘으면 보내기 전에 안내 */
export const MAX_BODY_BYTES = 10 * 1024 * 1024

/** UTF-8 바이트 길이. maxLength 는 글자 수라 한글 72바이트 판정 불가 */
export function utf8Length(value: string) {
  return new TextEncoder().encode(value).length
}

/** 비밀번호 도움말. 초과 시 바이트 수까지 */
export function passwordHint(value: string, t: Messages) {
  const bytes = utf8Length(value)

  return bytes > MAX_PASSWORD_BYTES ? t.password.tooLong(MAX_PASSWORD_BYTES, bytes) : t.password.hint
}
