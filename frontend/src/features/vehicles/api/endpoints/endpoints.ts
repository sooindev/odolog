import { api } from '@/shared/api/client/client'
import type { PageResponse } from '@/shared/api/types/types'
import type {
  UpdateOdometerRequest,
  VehicleRegisterRequest,
  VehicleResponse,
  VehicleUpdateRequest,
} from '@/features/vehicles/api/types/types'

/**
 * 차량 관련 엔드포인트
 * 화면이 URL 문자열을 직접 들고 있으면 경로가 바뀔 때 여러 파일을 뒤져야 함
 */

export function fetchVehicles(page: number, size = 10) {
  return api.get<PageResponse<VehicleResponse>>(`/api/vehicles?page=${page}&size=${size}`)
}

export function fetchVehicle(vehicleId: number) {
  return api.get<VehicleResponse>(`/api/vehicles/${vehicleId}`)
}

export function registerVehicle(request: VehicleRegisterRequest) {
  return api.post<VehicleResponse>('/api/vehicles', request)
}

export function updateVehicle(vehicleId: number, request: VehicleUpdateRequest) {
  return api.patch<VehicleResponse>(`/api/vehicles/${vehicleId}`, request)
}

export function updateOdometer(vehicleId: number, request: UpdateOdometerRequest) {
  return api.patch<VehicleResponse>(`/api/vehicles/${vehicleId}/odometer`, request)
}

export function deleteVehicle(vehicleId: number) {
  return api.del(`/api/vehicles/${vehicleId}`)
}
