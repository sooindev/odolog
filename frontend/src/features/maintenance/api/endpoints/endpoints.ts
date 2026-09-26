import { api } from '@/shared/api/client/client'
import type { PageResponse } from '@/shared/api/types/types'
import type {
  ServiceType,
  MaintenanceRecordRegisterRequest,
  MaintenanceRecordResponse,
  MaintenanceRecordUpdateRequest,
  NextServiceResponse,
  ServiceIntervalRequest,
} from '@/features/maintenance/api/types/types'

/** 모든 경로가 /api/vehicles/{vehicleId} 아래 */
function basePath(vehicleId: string) {
  return `/api/vehicles/${vehicleId}/maintenance-records`
}

/** type 이 없으면 전체 */
export function fetchRecords(
  vehicleId: string,
  page: number,
  type: ServiceType | null = null,
  size = 10,
) {
  const query = `page=${page}&size=${size}${type === null ? '' : `&type=${type}`}`

  return api.get<PageResponse<MaintenanceRecordResponse>>(`${basePath(vehicleId)}?${query}`)
}

export function registerRecord(vehicleId: string, request: MaintenanceRecordRegisterRequest) {
  return api.post<MaintenanceRecordResponse>(basePath(vehicleId), request)
}

export function updateRecord(
  vehicleId: string,
  recordId: string,
  request: MaintenanceRecordUpdateRequest,
) {
  return api.patch<MaintenanceRecordResponse>(`${basePath(vehicleId)}/${recordId}`, request)
}

export function deleteRecord(vehicleId: string, recordId: string) {
  return api.del(`${basePath(vehicleId)}/${recordId}`)
}

/** 이력 있는 종류 전체를 한 번에 */
export function fetchNextServices(vehicleId: string) {
  return api.get<NextServiceResponse[]>(`${basePath(vehicleId)}/next-services`)
}


/**
 * 차량별 권장 주기 설정. 둘 다 null 이면 기본값 복귀
 * 204 후 fetchNextServices 재호출
 */
export function changeServiceInterval(
  vehicleId: string,
  type: ServiceType,
  request: ServiceIntervalRequest,
) {
  return api.patch(`${basePath(vehicleId)}/intervals/${type}`, request)
}
