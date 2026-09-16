import { fetchRecords } from '@/features/maintenance/api/endpoints/endpoints'
import { fetchFuelRecords } from '@/features/fuel/api/endpoints/endpoints'
import type { FuelRecordResponse } from '@/features/fuel/api/types/types'
import { fetchVehicles } from '@/features/vehicles/api/endpoints/endpoints'
import { SERVICE_TYPES } from '@/features/maintenance/api/types/types'
import type { MaintenanceRecordResponse, ServiceType } from '@/features/maintenance/api/types/types'
import type { VehicleResponse } from '@/features/vehicles/api/types/types'

/*
 * 홈 화면 통계의 조회와 계산. 순수 계산이라 JSX 와 섞지 않고 떼어 놨다.
 *
 * 요청 수는 1 + 차량 수 × 2. 차량 목록 1번, 차량마다 정비 이력과 주유 요약 1번씩 동시에.
 * 백엔드 요약 API 가 생기면 1번으로 줄어든다(CLAUDE.md 백로그).
 *
 * 정비와 주유 모두 **목록**으로 받는다. 주유는 요약 API(/fuel-records/summary)를 쓰면 정확한
 * 합계를 얻지만 달별 내역이 없다. 합계는 요약에서, 달별은 목록에서 가져오면 **같은 데이터가
 * 두 출처에서 오게 되어** 숫자가 어긋났을 때 어느 쪽이 맞는지 알 수 없다. 한 곳으로 통일했다.
 * 그래서 sumsComplete 가 정비·주유 양쪽에 똑같이 적용된다.
 */

// 개인용 앱이라 한 사람이 이보다 많이 가질 일은 없다고 보고 한 번에 받는다.
const VEHICLE_PAGE_SIZE = 100
const RECORD_PAGE_SIZE = 200
const FUEL_PAGE_SIZE = 200

export interface VehicleSummary {
  vehicle: VehicleResponse
  /** totalElements 라서 아래 비용 합계와 달리 항상 정확하다. */
  recordCount: number
  lastServiceDate: string | null
}

/** 월별 유지비 한 칸. cost 가 0이면 그 달에 아무 기록도 없었다는 뜻이다. */
export interface MonthlyCost {
  /** 'YYYY-MM' */
  month: string
  /** 정비 + 주유. 구성을 따로 들고 다녀 표에서 나눠 보여준다. */
  cost: number
  maintenanceCost: number
  fuelCost: number
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
  /** 정비 + 주유 기록 건수. 양쪽 다 서버가 센 값이라 정확하다. */
  recordCount: number
  /** 정비비 + 유류비. 차량 유지비에서 유류비가 빠지면 이 숫자는 사실상 틀린 값이 된다. */
  totalCost: number
  /** 구성을 따로 들고 다닌다 — 합계만 보여주면 어느 쪽이 큰지 알 수 없다. */
  maintenanceCost: number
  fuelCost: number
  /**
   * 합계를 낼 때 모든 행을 다 가져왔는지.
   * 건수는 서버의 totalElements 라 항상 맞지만, 합계는 받아 온 행을 직접 더한 값이라
   * 위 상한을 넘으면 일부만 반영된다. 화면에 그 사실을 표시하려고 들고 다닌다.
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
      maintenanceCost: 0,
      fuelCost: 0,
      sumsComplete: true,
      vehicles: [],
      recent: [],
      monthly: lastMonths(MONTHS_SHOWN).map((month) => ({
        month,
        cost: 0,
        maintenanceCost: 0,
        fuelCost: 0,
        count: 0,
      })),
      byType: [],
    }
  }

  // 순차로 기다리면 차량이 늘어난 만큼 그대로 느려진다.
  // 정비와 주유를 한 덩어리로 묶어 동시에 보낸다.
  const [recordPages, fuelPages] = await Promise.all([
    Promise.all(vehicles.map((vehicle) => fetchRecords(vehicle.id, 0, RECORD_PAGE_SIZE))),
    Promise.all(vehicles.map((vehicle) => fetchFuelRecords(vehicle.id, 0, FUEL_PAGE_SIZE))),
  ])

  const summaries: VehicleSummary[] = vehicles.map((vehicle, index) => ({
    vehicle,
    recordCount: recordPages[index].totalElements,
    // 목록 기본 정렬이 serviceDate DESC 라 첫 줄이 가장 최근이다.
    // 컨트롤러의 @PageableDefault 가 바뀌면 여기도 같이 틀어진다.
    lastServiceDate: recordPages[index].items[0]?.serviceDate ?? null,
  }))

  const recent = recordPages
    .flatMap((page, index) =>
      page.items.map((record) => ({ record, vehicle: vehicles[index] })),
    )
    // 'YYYY-MM-DD' 라 문자열 비교로 날짜 순서가 맞는다.
    // 같은 날짜면 id 내림차순. 백엔드가 쓰는 동점 기준과 같게 맞췄다.
    .sort((a, b) =>
      a.record.serviceDate === b.record.serviceDate
        ? b.record.id - a.record.id
        : b.record.serviceDate.localeCompare(a.record.serviceDate),
    )
    .slice(0, RECENT_LIMIT)

  const allRecords = recordPages.flatMap((page) => page.items)

  const maintenanceCost = sum(
    recordPages.flatMap((page) => page.items.map((record) => record.cost)),
  )
  const allFuel = fuelPages.flatMap((page) => page.items)
  const fuelCost = sum(allFuel.map((record) => record.totalCost))

  return {
    vehicleCount: vehiclePage.totalElements,
    totalOdometer: sum(vehicles.map((vehicle) => vehicle.odometer)),
    recordCount:
      sum(recordPages.map((page) => page.totalElements)) +
      sum(fuelPages.map((page) => page.totalElements)),
    totalCost: maintenanceCost + fuelCost,
    maintenanceCost,
    fuelCost,
    sumsComplete:
      vehicles.length === vehiclePage.totalElements &&
      recordPages.every((page) => page.items.length === page.totalElements) &&
      fuelPages.every((page) => page.items.length === page.totalElements),
    vehicles: summaries,
    recent,
    monthly: monthlyCost(allRecords, allFuel),
    byType: costByType(allRecords),
  }
}

/**
 * 최근 N개월의 'YYYY-MM' 목록.
 * new Date(년, 월 - i, 1) 은 월이 음수면 연도를 알아서 넘겨 준다. 직접 계산하면
 * 연말 경계에서 틀리기 쉽다. 날짜를 1일로 고정해서 31일에도 달이 안 밀린다.
 */
function lastMonths(count: number): string[] {
  const now = new Date()

  return Array.from({ length: count }, (_, index) => {
    const date = new Date(now.getFullYear(), now.getMonth() - (count - 1 - index), 1)

    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
  })
}

function monthlyCost(
  records: MaintenanceRecordResponse[],
  fuelRecords: FuelRecordResponse[],
): MonthlyCost[] {
  const buckets = new Map<string, { maintenanceCost: number; fuelCost: number; count: number }>()
  const bucketOf = (month: string) =>
    buckets.get(month) ?? { maintenanceCost: 0, fuelCost: 0, count: 0 }

  for (const record of records) {
    // 앞 7글자가 곧 'YYYY-MM'. Date 로 변환하면 시간대 문제만 생긴다.
    const month = record.serviceDate.slice(0, 7)
    const bucket = bucketOf(month)
    buckets.set(month, { ...bucket, maintenanceCost: bucket.maintenanceCost + record.cost, count: bucket.count + 1 })
  }

  for (const record of fuelRecords) {
    const month = record.fueledAt.slice(0, 7)
    const bucket = bucketOf(month)
    buckets.set(month, { ...bucket, fuelCost: bucket.fuelCost + record.totalCost, count: bucket.count + 1 })
  }

  // 빈 달도 채워 12칸을 유지한다. 기록이 있는 달만 모으면 가로축이 등간격이 아니게 되어
  // 띄엄띄엄 정비한 것이 꾸준히 정비한 것처럼 보인다.
  return lastMonths(MONTHS_SHOWN).map((month) => {
    const bucket = bucketOf(month)
    return {
      month,
      cost: bucket.maintenanceCost + bucket.fuelCost,
      maintenanceCost: bucket.maintenanceCost,
      fuelCost: bucket.fuelCost,
      count: bucket.count,
    }
  })
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
