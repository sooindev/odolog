/** 기능에 속하지 않는 공용 타입. 기능별 DTO 는 각 feature 에 */

/** 백엔드 PageResponse */
export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
}

/** 백엔드 ErrorCode. 이름이 API 계약이라 백엔드 enum 과 함께 수정 */
export const ERROR_CODES = [
  'VALIDATION_FAILED',
  'MALFORMED_BODY',
  'INVALID_PARAMETER',
  'INVALID_SORT',
  'FUTURE_DATE',
  'UNSUPPORTED_TIME_ZONE',
  'UNSUPPORTED_CURRENCY',
  'SAME_PASSWORD',
  'BAD_REQUEST',
  'LOGIN_REQUIRED',
  'LOGIN_FAILED',
  'WRONG_PASSWORD',
  'RESET_LINK_INVALID',
  'FORBIDDEN',
  'CSRF_REJECTED',
  'VEHICLE_NOT_FOUND',
  'MAINTENANCE_RECORD_NOT_FOUND',
  'FUEL_RECORD_NOT_FOUND',
  'NOT_FOUND',
  'METHOD_NOT_ALLOWED',
  'UNSUPPORTED_MEDIA_TYPE',
  'PAYLOAD_TOO_LARGE',
  'EMAIL_DUPLICATE',
  'PLATE_DUPLICATE',
  'ODOMETER_DECREASE',
  'DUPLICATE_VALUE',
  'CONCURRENT_UPDATE',
  'TOO_MANY_LOGIN_ATTEMPTS',
  'TOO_MANY_SIGNUP_ATTEMPTS',
  'TOO_MANY_RESET_REQUESTS',
  'TOO_MANY_PASSWORD_ATTEMPTS',
  'SAVE_FAILED',
  'SERVER_ERROR',
] as const

export type ErrorCode = (typeof ERROR_CODES)[number]

/** 백엔드 ErrorResponse. 화면은 code 로 문구를 고르고 message 는 대비책 */
export interface ErrorResponse {
  code?: ErrorCode
  message: string
  /** 문제가 된 입력 칸 */
  field?: string
  /** 429 일 때 남은 분 */
  retryAfterMinutes?: number
}
