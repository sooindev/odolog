import { api } from '@/shared/api/client/client'
import type { ServiceType } from '@/features/maintenance/api/types/types'

// 홈 화면 데이터. 백엔드 summary DTO 대응, 계산은 서버

/** 월별 유지비 한 칸. 빈 달도 0 으로 채운 12칸 */
export interface MonthlyCost {
  /** 'YYYY-MM' */
  month: string
  /** 정비 + 주유 */
  cost: number
  maintenanceCost: number
  fuelCost: number
  count: number
}

/** 정비 종류별 합계. 기록 있는 종류만, 비용 내림차순 */
export interface TypeCost {
  type: ServiceType
  cost: number
  count: number
}

export interface VehicleLine {
  /** 차량 공개 id */
  id: string
  plateNumber: string
  manufacturer: string
  modelName: string
  odometer: number
  maintenanceCount: number
  /** 정비 이력 없으면 null */
  lastServiceDate: string | null
  /** 주유 2건 미만이면 null */
  averageEfficiency: number | null
  /** 권장 주기가 지난 정비 종류 수. 차량 상세와 같은 계산 */
  overdueServiceCount: number
}

/**
 * 최근 활동 한 줄. 정비·주유 공용, kind 로 구분
 * 정렬은 서버 완료
 */
export interface RecentActivity {
  kind: 'MAINTENANCE' | 'FUEL'
  /** 공개 id */
  recordId: string
  /** 'YYYY-MM-DD' */
  date: string
  vehicleId: string
  vehicleName: string
  /** 주유 금액 없음이면 null */
  cost: number | null
  type: ServiceType | null
  liters: number | null
}

export interface HomeData {
  vehicleCount: number
  totalOdometer: number
  /** 정비 + 주유 건수 */
  recordCount: number
  /** 정비비 + 유류비. 구성도 함께 */
  totalCost: number
  maintenanceCost: number
  fuelCost: number
  monthly: MonthlyCost[]
  byType: TypeCost[]
  vehicles: VehicleLine[]
  recent: RecentActivity[]
}

export function loadHomeData() {
  return api.get<HomeData>('/api/summary')
}
