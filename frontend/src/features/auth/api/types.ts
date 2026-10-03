/** 백엔드 user DTO 대응(/v3/api-docs). 백엔드 변경 시 함께 수정 */

import type { Language, Preferences, UnitSystem } from '@/shared/lib/preferences'

/** 설정 넷은 선택. 화면이 브라우저 값으로 채움 */
export interface SignUpRequest extends Preferences {
  email: string
  password: string
  nickname: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface UpdateProfileRequest extends Preferences {
  nickname?: string
}

/** 둘 다 필수 */
export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

/** 본인 재확인용 비밀번호 */
export interface WithdrawRequest {
  password: string
}

export interface UserResponse {
  email: string
  nickname: string
  language: Language
  timeZone: string
  currency: string
  unitSystem: UnitSystem
}

/** 재설정 링크 요청. 가입 여부와 무관하게 같은 응답 */
export interface PasswordResetRequest {
  email: string
}

export interface PasswordResetConfirmRequest {
  token: string
  newPassword: string
}
