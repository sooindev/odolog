/*
 * 입력값 상한. 백엔드 common/validation/limit/InputLimits 와 짝이다
 *
 * 양쪽에 같은 숫자가 있는 것은 중복이지만 의도한 중복이다 — 브라우저가 먼저 막아 주면
 * 사용자가 저장을 누르기 전에 알고, 서버는 화면을 거치지 않는 요청까지 막는다.
 * 한쪽만 고치면 "화면은 되는데 저장이 안 되는" 상태가 되므로 둘을 같이 고쳐야 한다
 */

/** 주행거리 상한(km). 자리수를 크게 잘못 넣으면 차량 값이 거기 묶인다 */
export const MAX_ODOMETER = 2_000_000

/** 금액 상한(원) */
export const MAX_AMOUNT = 100_000_000

/** 비밀번호 상한(UTF-8 바이트). BCrypt 자체의 한계라 늘릴 수 없다 */
export const MAX_PASSWORD_BYTES = 72

/**
 * UTF-8 바이트 길이. 한글은 글자당 3바이트
 * `maxLength` 는 글자 수만 셀 수 있어 한글에서 72바이트를 못 막는다 —
 * 브라우저가 216바이트까지 통과시키고 서버가 400 을 준다.
 * 저장을 누르기 전에 알려 주려면 여기서 직접 세야 한다
 */
export function utf8Length(value: string) {
  return new TextEncoder().encode(value).length
}

/** 비밀번호 칸의 도움말. 넘치면 얼마나 넘쳤는지까지 */
export function passwordHint(value: string) {
  const bytes = utf8Length(value)

  return bytes > MAX_PASSWORD_BYTES
    ? `${MAX_PASSWORD_BYTES}바이트를 넘었습니다 (현재 ${bytes}바이트 · 한글은 글자당 3바이트)`
    : '8자 이상 · 한글은 24자까지'
}
