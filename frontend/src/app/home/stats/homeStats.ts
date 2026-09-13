import { fetchRecords } from '@/features/maintenance/api/endpoints/endpoints'
import { fetchVehicles } from '@/features/vehicles/api/endpoints/endpoints'
import { SERVICE_TYPES } from '@/features/maintenance/api/types/types'
import type { MaintenanceRecordResponse, ServiceType } from '@/features/maintenance/api/types/types'
import type { VehicleResponse } from '@/features/vehicles/api/types/types'

/*
 * 홈 화면 통계의 조회와 계산.
 *
 * 화면(.tsx)에서 떼어 낸 이유: 여기 있는 건 전부 순수한 계산이라 JSX와 섞이면 둘 다 읽기 나빠진다.
 * 컴포넌트가 아닌 값만 내보내므로 핫 리로드에도 영향이 없다.
 *
 * **요청 수는 1 + 차량 수다.** 차량 목록 1번, 그다음 차량마다 정비 이력 1번씩(동시에).
 * 차량 3대면 4번. 백엔드에 요약 API가 하나 생기면 1번으로 줄어든다(CLAUDE.md 백로그).
 */

// 개인용 차량 관리 앱에서 한 사람이 이보다 많이 가질 일은 없다고 보고 한 번에 받는다.
const VEHICLE_PAGE_SIZE = 100
const RECORD_PAGE_SIZE = 200

export interface VehicleSummary {
  vehicle: VehicleResponse
  /** totalElements 라서 아래 비용 합계와 달리 항상 정확하다. */
  recordCount: number
  lastServiceDate: string | null
}

/** 월별 정비 비용 한 칸. cost 가 0이면 그 달에 정비가 없었다는 뜻이다. */
export interface MonthlyCost {
  /** 'YYYY-MM' */
  month: string
  cost: number
  count: number
}

/** 정비 종류별 합계. 비용 내림차순. */
export interface TypeCost {
  type: ServiceType
  cost: number
  count: number
}

export interface RecentRecord {
  record: MaintenanceRecordResponse
  vehicle: VehicleResponse
}

export interface HomeData {
  vehicleCount: number
  totalOdometer: number
  recordCount: number
  totalCost: number
  /**
   * 합계를 낼 때 모든 행을 다 가져왔는지.
   *
   * 건수(recordCount)는 서버가 준 totalElements 라 항상 맞지만, **주행거리·비용 합계는
   * 받아 온 행을 직접 더한 값**이라 위 상한을 넘으면 일부만 더해진다.
   * 그 사실을 숨기지 않고 화면에 표시하려고 들고 다닌다.
   */
  sumsComplete: boolean
  vehicles: VehicleSummary[]
  recent: RecentRecord[]
  monthly: MonthlyCost[]
  byType: TypeCost[]
}

const RECENT_LIMIT = 5
const MONTHS_SHOWN = 12

export async function loadHomeData(): Promise<HomeData> {
  const vehiclePage = await fetchVehicles(0, VEHICLE_PAGE_SIZE)
  const vehicles = vehiclePage.items

  if (vehicles.length === 0) {
    return {
      vehicleCount: vehiclePage.totalElements,
      totalOdometer: 0,
      recordCount: 0,
      totalCost: 0,
      sumsComplete: true,
      vehicles: [],
      recent: [],
      monthly: lastMonths(MONTHS_SHOWN).map((month) => ({ month, cost: 0, count: 0 })),
      byType: [],
    }
  }

  // Promise.all: 차량 수만큼의 요청을 순서대로 기다리지 않고 동시에 보낸다.
  // 순차로 하면 차량이 늘어난 만큼 그대로 느려진다.
  const recordPages = await Promise.all(
    vehicles.map((vehicle) => fetchRecords(vehicle.id, 0, RECORD_PAGE_SIZE)),
  )

  const summaries: VehicleSummary[] = vehicles.map((vehicle, index) => ({
    vehicle,
    recordCount: recordPages[index].totalElements,
    // 목록의 기본 정렬이 serviceDate DESC 라 첫 줄이 가장 최근이다.
    // 이 전제가 깨지면(컨트롤러의 @PageableDefault 가 바뀌면) 여기도 같이 틀어진다.
    lastServiceDate: recordPages[index].items[0]?.serviceDate ?? null,
  }))

  const recent = recordPages
    .flatMap((page, index) =>
      page.items.map((record) => ({ record, vehicle: vehicles[index] })),
    )
    // serviceDate 는 'YYYY-MM-DD' 라서 문자열 비교만으로 날짜 순서가 맞는다.
    // 같은 날짜면 id 가 큰 쪽이 나중에 등록된 것 — 백엔드가 쓰는 동점 기준과 같다.
    .sort((a, b) =>
      a.record.serviceDate === b.record.serviceDate
        ? b.record.id - a.record.id
        : b.record.serviceDate.localeCompare(a.record.serviceDate),
    )
    .slice(0, RECENT_LIMIT)

  const allRecords = recordPages.flatMap((page) => page.items)

  return {
    vehicleCount: vehiclePage.totalElements,
    totalOdometer: sum(vehicles.map((vehicle) => vehicle.odometer)),
    recordCount: sum(recordPages.map((page) => page.totalElements)),
    totalCost: sum(recordPages.flatMap((page) => page.items.map((record) => record.cost))),
    sumsComplete:
      vehicles.length === vehiclePage.totalElements &&
      recordPages.every((page) => page.items.length === page.totalElements),
    vehicles: summaries,
    recent,
    monthly: monthlyCost(allRecords),
    byType: costByType(allRecords),
  }
}

/**
 * 오늘이 속한 달까지 거슬러 올라가 최근 N개월의 'YYYY-MM' 목록.
 *
 * `new Date(년, 월 - i, 1)` 은 월이 음수가 되면 연도를 알아서 넘겨 준다(-1월 → 작년 12월).
 * 직접 나눗셈으로 계산하면 연말 경계에서 틀리기 쉬운데 여기에 맡기면 된다.
 *
 * UTC 가 아니라 로컬 기준으로 만든다 — todayString() 이 피해 간 함정과 같은 이유로,
 * UTC 를 쓰면 한국 시간 새벽에 달이 하나 밀린다.
 */
function lastMonths(count: number): string[] {
  const now = new Date()

  return Array.from({ length: count }, (_, index) => {
    const date = new Date(now.getFullYear(), now.getMonth() - (count - 1 - index), 1)

    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
  })
}

function monthlyCost(records: MaintenanceRecordResponse[]): MonthlyCost[] {
  const buckets = new Map<string, { cost: number; count: number }>()

  for (const record of records) {
    // 'YYYY-MM-DD' 의 앞 7글자가 곧 'YYYY-MM' 이다. Date 로 변환하면 시간대 문제만 생긴다.
    const month = record.serviceDate.slice(0, 7)
    const bucket = buckets.get(month) ?? { cost: 0, count: 0 }
    buckets.set(month, { cost: bucket.cost + record.cost, count: bucket.count + 1 })
  }

  // 기록이 있는 달만 모으지 않고 12칸을 항상 채운다. 빈 달을 빼 버리면 가로축이
  // 등간격이 아니게 되어, 띄엄띄엄 정비한 것이 꾸준히 정비한 것처럼 보인다.
  return lastMonths(MONTHS_SHOWN).map((month) => ({
    month,
    cost: buckets.get(month)?.cost ?? 0,
    count: buckets.get(month)?.count ?? 0,
  }))
}

function costByType(records: MaintenanceRecordResponse[]): TypeCost[] {
  return SERVICE_TYPES.map((type) => {
    const matched = records.filter((record) => record.type === type)

    return {
      type,
      cost: sum(matched.map((record) => record.cost)),
      count: matched.length,
    }
  })
    .filter((entry) => entry.count > 0)
    .sort((a, b) => b.cost - a.cost)
}

function sum(values: number[]) {
  return values.reduce((total, value) => total + value, 0)
}
