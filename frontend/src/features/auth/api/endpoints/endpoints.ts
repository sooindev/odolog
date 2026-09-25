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

/** 탈퇴 전에 챙겨 갈 수 있어야 한다. 파일로 만드는 일은 화면이 한다 */
export function exportAccount() {
  return api.get<AccountExport>('/api/users/me/export')
}

/**
 * 내보낸 파일을 되돌려 넣는다. 내보내기의 짝 — 복원할 수 없으면 백업이 아니다
 * 사용자 정보는 서버가 무시한다. 남의 파일을 넣어도 내 계정에 기록만 붙는다
 */
export function restoreAccount(vehicles: AccountExport['vehicles']) {
  return api.post<AccountRestoreResult>('/api/users/me/restore', { vehicles })
}

// 204. 가입되지 않은 주소여도 똑같이 204 다 — 응답이 갈리면 가입 여부 조회가 된다
export function requestPasswordReset(request: PasswordResetRequest) {
  return api.post<void>('/api/users/password-reset', request)
}

// 204. 성공하면 그 토큰은 즉시 죽는다
export function confirmPasswordReset(request: PasswordResetConfirmRequest) {
  return api.patch<void>('/api/users/password-reset', request)
}
