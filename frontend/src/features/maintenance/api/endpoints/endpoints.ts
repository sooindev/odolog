import { api } from '@/shared/api/client/client'
import type { PageResponse } from '@/shared/api/types/types'
import type {
  MaintenanceRecordRegisterRequest,
  MaintenanceRecordResponse,
  MaintenanceRecordUpdateRequest,
  NextServiceResponse,
} from '@/features/maintenance/api/types/types'

/** 정비 이력은 항상 특정 차량 소속이라 모든 경로가 /api/vehicles/{vehicleId} 아래 */
function basePath(vehicleId: number) {
  return `/api/vehicles/${vehicleId}/maintenance-records`
}

export function fetchRecords(vehicleId: number, page: number, size = 10) {
  return api.get<PageResponse<MaintenanceRecordResponse>>(
    `${basePath(vehicleId)}?page=${page}&size=${size}`,
  )
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

