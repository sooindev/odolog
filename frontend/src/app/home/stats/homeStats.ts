import { api } from '@/shared/api/client/client'
import type { ServiceType } from '@/features/maintenance/api/types/types'

/*
 * 홈 화면 데이터. 백엔드 com.odolog.app.summary.dto 대응
 * 서버가 전부 읽어 더한 것을 한 번에 받으므로 이 파일에 계산은 없음
 * 직접 집계하던 시절의 문제 — 페이지 상한을 넘는 기록이 합계에서 빠지고, 계산이 두 곳에 존재
 */

/** 월별 유지비 한 칸. 빈 달도 0 으로 채워 12칸 유지 */
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
  /** 권장 주기가 지난 정비 종류 수. 차량 상세와 같은 계산이다 */
  overdueServiceCount: number
}

/**
 * 최근 활동 한 줄. 정비·주유 공용이라 kind 로 갈라 읽음
 * 정렬(날짜 내림차순, 같은 날은 정비 먼저)은 서버가 끝냄
 */
export interface RecentActivity {
  kind: 'MAINTENANCE' | 'FUEL'
  recordId: number
  /** 'YYYY-MM-DD' */
  date: string
  vehicleId: string
  vehicleName: string
  cost: number
  type: ServiceType | null
  liters: number | null
}

export interface HomeData {
  vehicleCount: number
  totalOdometer: number
  /** 정비 + 주유 건수 */
  recordCount: number
  /** 정비비 + 유류비. 구성(아래 둘)도 함께 옴 */
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
