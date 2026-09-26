import { api } from '@/shared/api/client/client'
import type { PageResponse } from '@/shared/api/types/types'
import type {
  FuelRecordRegisterRequest,
  FuelRecordResponse,
  FuelRecordUpdateRequest,
  FuelSummaryResponse,
} from '@/features/fuel/api/types/types'

/** 모든 경로가 /api/vehicles/{vehicleId} 아래 */
function base(vehicleId: string) {
  return `/api/vehicles/${vehicleId}/fuel-records`
}

/** sort 미전송. 서버 고정 */
export function fetchFuelRecords(vehicleId: string, page: number, size = 10) {
  return api.get<PageResponse<FuelRecordResponse>>(`${base(vehicleId)}?page=${page}&size=${size}`)
}

export function fetchFuelSummary(vehicleId: string) {
  return api.get<FuelSummaryResponse>(`${base(vehicleId)}/summary`)
}

export function registerFuelRecord(vehicleId: string, request: FuelRecordRegisterRequest) {
  return api.post<FuelRecordResponse>(base(vehicleId), request)
}

export function updateFuelRecord(
  vehicleId: string,
  recordId: string,
  request: FuelRecordUpdateRequest,
) {
  return api.patch<FuelRecordResponse>(`${base(vehicleId)}/${recordId}`, request)
}

export function deleteFuelRecord(vehicleId: string, recordId: string) {
  return api.del(`${base(vehicleId)}/${recordId}`)
}
