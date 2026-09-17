import { api } from '@/shared/api/client/client'
import type {
  ChangePasswordRequest,
  LoginRequest,
  SignUpRequest,
  UpdateProfileRequest,
  UserResponse,
  WithdrawRequest,
} from '@/features/auth/api/types/types'

/**
 * 회원 관련 엔드포인트
 * auth 만 이 파일이 없어 URL 문자열이 AuthProvider·SignUpPage·ProfilePage 에 흩어져 있었음
 */

/** 앱이 뜰 때 세션 복구용. 401 이 정상 응답 */
export function fetchMe() {
  return api.get<UserResponse>('/api/users/me')
}

/** 가입만으로는 세션이 안 생김. 호출부에서 로그인까지 이어서 */
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

// 204. 세션은 유지되므로 다시 로그인시키지 않음
export function changePassword(request: ChangePasswordRequest) {
  return api.patch<void>('/api/users/me/password', request)
}

// 204. 서버가 세션까지 끊으므로 로그아웃 호출 불필요
export function withdraw(request: WithdrawRequest) {
  return api.del<void>('/api/users/me', request)
}
