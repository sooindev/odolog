import { api } from '@/shared/api/client'
import type {
  PasswordResetConfirmRequest,
  PasswordResetRequest,
  LoginRequest,
  SignUpRequest,
  UserResponse,
  WithdrawRequest,
} from '@/features/auth/api/types'

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

// 204. 서버가 세션까지 종료
export function withdraw(request: WithdrawRequest) {
  return api.del<void>('/api/users/me', request)
}

// 204. 가입 여부와 무관
export function requestPasswordReset(request: PasswordResetRequest) {
  return api.post<void>('/api/users/password-reset', request)
}

// 204. 성공 시 토큰 즉시 만료
export function confirmPasswordReset(request: PasswordResetConfirmRequest) {
  return api.patch<void>('/api/users/password-reset', request)
}
