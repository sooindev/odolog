import type { QueryClient } from '@tanstack/react-query'

/**
 * 조회 캐시 키 한 벌. 차량 하나의 모든 것이 ['vehicles', id] 아래
 * 기능 사이 import 없이 무효화하려고 shared 에 둠
 */
export const queryKeys = {
  /** 모든 차량의 캐시. 화면 밖에서 기록·설정이 바뀌었을 때 통째로 버림 */
  allVehicles: () => ['vehicles'] as const,
  vehicle: (vehicleId: string) => ['vehicles', vehicleId] as const,
  vehicleDetail: (vehicleId: string) => ['vehicles', vehicleId, 'detail'] as const,
  maintenance: (vehicleId: string) => ['vehicles', vehicleId, 'maintenance'] as const,
  maintenanceList: (vehicleId: string, page: number, filter: string | null) =>
    ['vehicles', vehicleId, 'maintenance', 'list', page, filter] as const,
  nextServices: (vehicleId: string) => ['vehicles', vehicleId, 'maintenance', 'next-services'] as const,
  fuel: (vehicleId: string) => ['vehicles', vehicleId, 'fuel'] as const,
  fuelList: (vehicleId: string, page: number) => ['vehicles', vehicleId, 'fuel', 'list', page] as const,
  fuelSummary: (vehicleId: string) => ['vehicles', vehicleId, 'fuel', 'summary'] as const,
  gettingStarted: (vehicleId: string) => ['vehicles', vehicleId, 'getting-started'] as const,
}

/**
 * 정비 기록 변경 뒤 다시 읽을 것. 서버가 차량 주행거리를 올렸을 수 있어 차량까지
 * 완료까지 기다림. 저장 버튼의 잠금이 새 값과 함께 풀리게
 */
export function invalidateAfterMaintenance(queryClient: QueryClient, vehicleId: string) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: queryKeys.maintenance(vehicleId) }),
    queryClient.invalidateQueries({ queryKey: queryKeys.vehicleDetail(vehicleId) }),
    queryClient.invalidateQueries({ queryKey: queryKeys.gettingStarted(vehicleId) }),
  ])
}

/** 주유 기록 변경 뒤 다시 읽을 것. 주행거리가 오르면 지남 판정도 바뀌어 다음 정비까지 */
export function invalidateAfterFuel(queryClient: QueryClient, vehicleId: string) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: queryKeys.fuel(vehicleId) }),
    queryClient.invalidateQueries({ queryKey: queryKeys.vehicleDetail(vehicleId) }),
    queryClient.invalidateQueries({ queryKey: queryKeys.nextServices(vehicleId) }),
    queryClient.invalidateQueries({ queryKey: queryKeys.gettingStarted(vehicleId) }),
  ])
}
