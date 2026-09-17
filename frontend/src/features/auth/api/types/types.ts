/**
 * 백엔드 com.odolog.app.user.dto 대응. 스펙은 /v3/api-docs
 * 자동 동기화가 아니므로 백엔드 DTO 를 고치면 여기도 함께
 */

export interface SignUpRequest {
  email: string
  password: string
  nickname: string
  /** 백엔드에 NotBlank 가 없는 선택 항목 */
  phone?: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface UpdateProfileRequest {
  nickname?: string
  phone?: string
}

/** 둘 다 필수. 부분 수정이 아니라 "현재 비밀번호를 대고 바꾸는" 한 동작 */
export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

/** 되돌릴 수 없는 동작이라 비밀번호로 본인 재확인 */
export interface WithdrawRequest {
  password: string
}

export interface UserResponse {
  id: number
  email: string
  nickname: string
  phone: string | null
}
