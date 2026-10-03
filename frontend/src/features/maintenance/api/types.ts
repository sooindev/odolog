/** 백엔드 maintenance DTO 대응. 백엔드 변경 시 함께 수정 */

/** 백엔드 enum 과 같은 순서. 부위별(엔진·구동 / 제동 / 타이어·조향 / 소모품) */
export const SERVICE_TYPES = [
  'ENGINE_OIL',
  'TRANSMISSION_FLUID',
  'SPARK_PLUG',
  'TIMING_BELT',
  'COOLANT',
  'BRAKE_PAD',
  'BRAKE_FLUID',
  'TIRE',
  'TIRE_ROTATION',
  'WHEEL_ALIGNMENT',
  'AIR_FILTER',
  'CABIN_FILTER',
  'BATTERY',
  'WIPER',
  'OTHER',
] as const

export type ServiceType = (typeof SERVICE_TYPES)[number]

/** 선택 목록 구역. 이름은 문구 사전(t.serviceTypeGroups), 종류 이름은 t.serviceTypes */
export const SERVICE_TYPE_GROUPS = [
  { key: 'engine', types: ['ENGINE_OIL', 'TRANSMISSION_FLUID', 'SPARK_PLUG', 'TIMING_BELT', 'COOLANT'] },
  { key: 'brakes', types: ['BRAKE_PAD', 'BRAKE_FLUID'] },
  { key: 'tires', types: ['TIRE', 'TIRE_ROTATION', 'WHEEL_ALIGNMENT'] },
  { key: 'consumables', types: ['AIR_FILTER', 'CABIN_FILTER', 'BATTERY', 'WIPER'] },
  { key: 'other', types: ['OTHER'] },
] as const satisfies readonly { key: string; types: readonly ServiceType[] }[]

export interface MaintenanceRecordRegisterRequest {
  type: ServiceType
  description?: string
  /** 모르면 null. 0 은 "0원" 이라는 다른 뜻 */
  cost: number | null
  /** 모르면 null. 다음 정비는 날짜 기준만 */
  serviceOdometer: number | null
  /** YYYY-MM-DD */
  serviceDate: string
}

export interface MaintenanceRecordUpdateRequest {
  type?: ServiceType
  description?: string
  cost?: number
  serviceOdometer?: number
  serviceDate?: string
  /** 비용 비움. JSON 의 키 없음과 null 이 같게 도착해 플래그 별도 */
  clearCost?: boolean
  /** 주행거리 비움 */
  clearServiceOdometer?: boolean
}

export interface MaintenanceRecordResponse {
  /** 공개 id(12자) */
  id: string
  type: ServiceType
  description: string | null
  /** 통화의 최소 단위. 안 적었으면 null */
  cost: number | null
  /** ISO 4217. 기록할 때의 사용자 통화 */
  currency: string
  /** 안 적었으면 null */
  serviceOdometer: number | null
  /** YYYY-MM-DD */
  serviceDate: string
}

export interface NextServiceResponse {
  type: ServiceType
  /** 이력 없으면 null */
  lastServiceOdometer: number | null
  /** 이력이 없거나 주기 없는 종류(OTHER)면 null */
  nextServiceOdometer: number | null
  lastServiceDate: string | null
  nextServiceDate: string | null
  /** 주행거리·날짜 중 하나라도 지남. 서버 판정 */
  overdue: boolean
  /** 아직 안 지났지만 곧(1,000km 또는 1개월 안). 서버 판정 */
  dueSoon: boolean
  /** 실제 적용 주기. 차량별 설정 우선 */
  intervalKm: number | null
  intervalMonths: number | null
  /** 기본값 덮어씀 여부 */
  customized: boolean
  /** 직접 정한 km 주기. 기본값을 쓰면 null */
  customIntervalKm: number | null
  /** 직접 정한 개월 주기. 기본값을 쓰면 null */
  customIntervalMonths: number | null
  /** 종류의 기본 주기. 칸을 비우면 쓰이는 값 */
  defaultIntervalKm: number | null
  defaultIntervalMonths: number | null
}

/** 차량별 권장 주기. 전체 교체, 둘 다 null 이면 기본값 */
export interface ServiceIntervalRequest {
  intervalKm: number | null
  intervalMonths: number | null
}
