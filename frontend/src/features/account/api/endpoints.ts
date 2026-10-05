import { api } from '@/shared/api/client'
import type {
  AccountExport,
  AccountRestoreResult,
  ChangePasswordRequest,
  UpdateProfileRequest,
} from '@/features/account/api/types'
import type { UserResponse } from '@/features/auth/api/types'

/** 계정 화면의 엔드포인트. 세션을 바꾸는 것(로그인·로그아웃·탈퇴)은 auth */

export function updateProfile(request: UpdateProfileRequest) {
  return api.patch<UserResponse>('/api/users/me', request)
}

// 204. 세션 유지
export function changePassword(request: ChangePasswordRequest) {
  return api.patch<void>('/api/users/me/password', request)
}

/** 기록 내보내기. 파일 생성은 화면 담당 */
export function exportAccount() {
  return api.get<AccountExport>('/api/users/me/export')
}

/** 내보낸 파일 복원. 사용자 정보는 서버가 무시 */
export function restoreAccount(vehicles: AccountExport['vehicles']) {
  return api.post<AccountRestoreResult>('/api/users/me/restore', { vehicles })
}
