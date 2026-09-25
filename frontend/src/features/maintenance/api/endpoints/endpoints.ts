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

/** 정비 이력은 항상 특정 차량 소속이라 모든 경로가 /api/vehicles/{vehicleId} 아래 */
function basePath(vehicleId: number) {
  return `/api/vehicles/${vehicleId}/maintenance-records`
}

/** type 이 없으면 전체 */
export function fetchRecords(
  vehicleId: number,
  page: number,
  type: ServiceType | null = null,
  size = 10,
) {
  const query = `page=${page}&size=${size}${type === null ? '' : `&type=${type}`}`

  return api.get<PageResponse<MaintenanceRecordResponse>>(`${basePath(vehicleId)}?${query}`)
}

export function registerRecord(vehicleId: number, request: MaintenanceRecordRegisterRequest) {
  return api.post<MaintenanceRecordResponse>(basePath(vehicleId), request)
}

export function updateRecord(
  vehicleId: number,
  recordId: number,
  request: MaintenanceRecordUpdateRequest,
) {
  return api.patch<MaintenanceRecordResponse>(`${basePath(vehicleId)}/${recordId}`, request)
}

export function deleteRecord(vehicleId: number, recordId: number) {
  return api.del(`${basePath(vehicleId)}/${recordId}`)
}

/** 이력 있는 종류를 한 번에. 종류마다 요청하면 15왕복 */
export function fetchNextServices(vehicleId: number) {
  return api.get<NextServiceResponse[]>(`${basePath(vehicleId)}/next-services`)
}


/**
 * 이 차량에서 쓸 권장 주기를 정한다. 둘 다 null 이면 기본값으로 되돌아간다
 * 응답이 없어(204) 화면은 곧바로 fetchNextServices 를 다시 부른다
 */
export function changeServiceInterval(
  vehicleId: number,
  type: ServiceType,
  request: ServiceIntervalRequest,
) {
  return api.patch(`${basePath(vehicleId)}/intervals/${type}`, request)
}
