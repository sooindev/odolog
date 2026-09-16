import { api } from '@/shared/api/client/client'
import type { PageResponse } from '@/shared/api/types/types'
import type {
  FuelRecordRegisterRequest,
  FuelRecordResponse,
  FuelRecordUpdateRequest,
  FuelSummaryResponse,
} from '@/features/fuel/api/types/types'

/** 주유 기록은 항상 특정 차량에 속하므로 모든 경로가 /api/vehicles/{vehicleId} 아래에 있다. */
function base(vehicleId: number) {
  return `/api/vehicles/${vehicleId}/fuel-records`
}

/**
 * sort 를 보내지 않는다. 서버가 주행거리 내림차순으로 고정한다 —
 * 정렬이 곧 연비 계산의 전제라서, 다른 순서로 정렬하면 연비가 조용히 틀린 값이 된다.
 */
export function fetchFuelRecords(vehicleId: number, page: number, size = 10) {
  return api.get<PageResponse<FuelRecordResponse>>(`${base(vehicleId)}?page=${page}&size=${size}`)
}

export function fetchFuelSummary(vehicleId: number) {
  return api.get<FuelSummaryResponse>(`${base(vehicleId)}/summary`)
}

export function registerFuelRecord(vehicleId: number, request: FuelRecordRegisterRequest) {
  return api.post<FuelRecordResponse>(base(vehicleId), request)
}

export function updateFuelRecord(
  vehicleId: number,
  recordId: number,
  request: FuelRecordUpdateRequest,
) {
  return api.patch<FuelRecordResponse>(`${base(vehicleId)}/${recordId}`, request)
}

export function deleteFuelRecord(vehicleId: number, recordId: number) {
  return api.del(`${base(vehicleId)}/${recordId}`)
}
