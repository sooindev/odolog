import { api } from '@/shared/api/client'
import type { AccountExport, AccountRestoreResult } from '@/features/account/api/types'

/** 계정 기록 전체에 걸친 엔드포인트. 백엔드 account 대응 */

/** 기록 내보내기. 파일 생성은 화면 담당 */
export function exportAccount() {
  return api.get<AccountExport>('/api/users/me/export')
}

/** 내보낸 파일 복원. 사용자 정보는 서버가 무시 */
export function restoreAccount(vehicles: AccountExport['vehicles']) {
  return api.post<AccountRestoreResult>('/api/users/me/restore', { vehicles })
}
