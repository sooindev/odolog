/**
 * 백엔드 com.odolog.app.user.dto 에 대응하는 타입.
 * 스펙은 http://localhost:8080/v3/api-docs 에서 확인할 수 있다.
 * 백엔드 DTO를 고치면 이 파일도 같이 고쳐야 한다 (자동 동기화되지 않음).
 */

export interface SignUpRequest {
  email: string
  password: string
  nickname: string
  /** 백엔드에 @NotBlank 가 없는 선택 항목이다. */
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

/** 둘 다 필수다. 부분 수정이 아니라 "현재 비밀번호를 대고 새것으로 바꾸는" 한 동작이다. */
export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

/** 되돌릴 수 없는 동작이라 비밀번호로 본인을 다시 확인한다. */
export interface WithdrawRequest {
  password: string
}

export interface UserResponse {
  id: number
  email: string
  nickname: string
  phone: string | null
}
