import { api } from '@/shared/api/client/client'
import type {
  AccountExport,
  AccountRestoreResult,
  ChangePasswordRequest,
  PasswordResetConfirmRequest,
  PasswordResetRequest,
  LoginRequest,
  SignUpRequest,
  UpdateProfileRequest,
  UserResponse,
  WithdrawRequest,
} from '@/features/auth/api/types/types'

/** 회원 관련 엔드포인트 */

/** 앱 기동 시 세션 복구용. 401 이 정상 응답 */
export function fetchMe() {
  return api.get<UserResponse>('/api/users/me')
}

/** 가입은 세션을 만들지 않음. 호출부에서 로그인까지 */
export function signUp(request: SignUpRequest) {
  return api.post<UserResponse>('/api/users', request)
}

export function login(request: LoginRequest) {
  return api.post<UserResponse>('/api/users/login', request)
}

export function logout() {
  return api.post<void>('/api/users/logout')
}

export function updateProfile(request: UpdateProfileRequest) {
  return api.patch<UserResponse>('/api/users/me', request)
}

// 204. 세션 유지
export function changePassword(request: ChangePasswordRequest) {
  return api.patch<void>('/api/users/me/password', request)
}

// 204. 서버가 세션까지 종료
export function withdraw(request: WithdrawRequest) {
  return api.del<void>('/api/users/me', request)
}

/** 기록 내보내기. 파일 생성은 화면 담당 */
export function exportAccount() {
  return api.get<AccountExport>('/api/users/me/export')
}

/** 내보낸 파일 복원. 사용자 정보는 서버가 무시 */
export function restoreAccount(vehicles: AccountExport['vehicles']) {
  return api.post<AccountRestoreResult>('/api/users/me/restore', { vehicles })
}

// 204. 가입 여부와 무관
export function requestPasswordReset(request: PasswordResetRequest) {
  return api.post<void>('/api/users/password-reset', request)
}

// 204. 성공 시 토큰 즉시 만료
export function confirmPasswordReset(request: PasswordResetConfirmRequest) {
  return api.patch<void>('/api/users/password-reset', request)
}
