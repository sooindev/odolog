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
 * 회원 관련 엔드포인트를 한곳에 모은다.
 * vehicles/maintenance 에는 이미 있었는데 auth 만 없어서, URL 문자열이
 * AuthProvider·SignUpPage·ProfilePage 세 곳에 흩어져 있었다.
 */

/** 앱이 뜰 때 "누구로 로그인돼 있는지"를 서버에 되묻는 요청. 401이 정상적인 답이다. */
export function fetchMe() {
  return api.get<UserResponse>('/api/users/me')
}

/** 가입만으로는 세션이 만들어지지 않는다. 호출한 쪽에서 로그인까지 이어서 해야 한다. */
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

// 204 라 돌려받을 것이 없다. 세션은 그대로 유지되므로 다시 로그인시키지 않는다.
export function changePassword(request: ChangePasswordRequest) {
  return api.patch<void>('/api/users/me/password', request)
}

// 204. 서버가 세션까지 끊으므로 따로 로그아웃을 부를 필요가 없다.
export function withdraw(request: WithdrawRequest) {
  return api.del<void>('/api/users/me', request)
}
