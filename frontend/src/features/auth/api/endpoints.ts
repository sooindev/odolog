import { api } from '@/shared/api/client'
import type {
  LoginRequest,
  SignUpRequest,
  UpdateProfileRequest,
  UserResponse,
} from '@/features/auth/api/types'

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
