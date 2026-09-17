import { api } from '@/shared/api/client/client'
import type { ServiceType } from '@/features/maintenance/api/types/types'

/*
 * 홈 화면 데이터. 백엔드 com.odolog.app.summary.dto 에 대응한다.
 *
 * 전에는 이 파일이 차량 목록 1번 + 차량마다 정비·주유 목록을 받아(요청 1 + 차량수 × 2)
 * 월별·종류별·최근 활동을 **직접 계산했다.** 그래서 두 가지가 따라왔다:
 * 페이지 상한(200건)을 넘는 기록이 합계에서 빠졌고("일부 정비 기록만 합산됨"이라는 단서를
 * 화면에 달고 있었다), 같은 계산이 서버·클라이언트 두 곳에 존재했다.
 *
 * 이제 서버가 SQL 로 전부 읽어 더한 것을 한 번에 준다. 이 파일에 계산은 남아 있지 않다.
 */

/** 월별 유지비 한 칸. 기록이 없는 달도 0 으로 채워 12칸이 유지된다. */
export interface MonthlyCost {
  /** 'YYYY-MM' */
  month: string
  /** 정비 + 주유. */
  cost: number
  maintenanceCost: number
  fuelCost: number
  count: number
}

/** 정비 종류별 합계. 기록이 있는 종류만 오고, 비용 내림차순이다. */
export interface TypeCost {
  type: ServiceType
  cost: number
  count: number
}

export interface VehicleLine {
  id: number
  plateNumber: string
  manufacturer: string
  modelName: string
  odometer: number
  maintenanceCount: number
  /** 정비 이력이 없으면 null. */
  lastServiceDate: string | null
  /** 주유 기록이 2건 미만이면 null. */
  averageEfficiency: number | null
}

/**
 * 최근 활동 한 줄. 정비와 주유가 한 목록에 섞인다.
 *
 * kind 로 갈라 읽는다 — 정비면 type, 주유면 liters 가 채워진다.
 * 서버가 이미 날짜 내림차순(같은 날은 정비 먼저)으로 정렬해 준다.
 */
export interface RecentActivity {
  kind: 'MAINTENANCE' | 'FUEL'
  recordId: number
  /** 'YYYY-MM-DD' */
  date: string
  vehicleId: number
  vehicleName: string
  cost: number
  type: ServiceType | null
  liters: number | null
}

export interface HomeData {
  vehicleCount: number
  totalOdometer: number
  /** 정비 + 주유 건수. */
  recordCount: number
  /** 정비비 + 유류비. 구성을 알아야 어느 쪽이 큰지 보이므로 아래 둘도 함께 온다. */
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
