import type { ServiceType } from '@/features/maintenance/api/types'

// 컴포넌트 파일에서 값을 내보내면 핫 리로드가 깨져 따로 둠

/** 목록이 보는 자리. 목록 밖(빠른 정비)에서 기록이 생기면 부모가 첫 장·전체로 되돌림 */
export interface MaintenanceView {
  page: number
  /** null 이면 전체 */
  filter: ServiceType | null
}

export const ALL_MAINTENANCE: MaintenanceView = { page: 0, filter: null }
