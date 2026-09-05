import { api } from '@/shared/api/client'
import type {
  MaintenanceRecordRegisterRequest,
  MaintenanceRecordResponse,
  MaintenanceRecordUpdateRequest,
  NextServiceResponse,
  PageResponse,
  ServiceType,
} from '@/shared/api/types'

/** 정비 이력은 항상 특정 차량에 속하므로 모든 경로가 /api/vehicles/{vehicleId} 아래에 있다. */
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

export function fetchNextService(vehicleId: number, type: ServiceType) {
  return api.get<NextServiceResponse>(`${basePath(vehicleId)}/next-service?type=${type}`)
}
