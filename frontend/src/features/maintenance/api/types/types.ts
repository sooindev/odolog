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

export const SERVICE_TYPE_LABELS: Record<ServiceType, string> = {
  ENGINE_OIL: '엔진오일',
  TRANSMISSION_FLUID: '미션오일',
  SPARK_PLUG: '점화 플러그',
  TIMING_BELT: '타이밍 벨트',
  COOLANT: '냉각수',
  BRAKE_PAD: '브레이크 패드',
  BRAKE_FLUID: '브레이크액',
  TIRE: '타이어',
  TIRE_ROTATION: '타이어 위치 교환',
  WHEEL_ALIGNMENT: '휠 얼라인먼트',
  AIR_FILTER: '에어 필터',
  CABIN_FILTER: '에어컨 필터',
  BATTERY: '배터리',
  WIPER: '와이퍼',
  OTHER: '기타',
}

/** 선택 목록 구역 */
export const SERVICE_TYPE_GROUPS: { label: string; types: readonly ServiceType[] }[] = [
  { label: '엔진·구동', types: ['ENGINE_OIL', 'TRANSMISSION_FLUID', 'SPARK_PLUG', 'TIMING_BELT', 'COOLANT'] },
  { label: '제동', types: ['BRAKE_PAD', 'BRAKE_FLUID'] },
  { label: '타이어·조향', types: ['TIRE', 'TIRE_ROTATION', 'WHEEL_ALIGNMENT'] },
  { label: '소모품', types: ['AIR_FILTER', 'CABIN_FILTER', 'BATTERY', 'WIPER'] },
  { label: '그 밖', types: ['OTHER'] },
]

export interface MaintenanceRecordRegisterRequest {
  type: ServiceType
  description?: string
  cost: number
  serviceOdometer: number
  /** YYYY-MM-DD */
  serviceDate: string
}

export interface MaintenanceRecordUpdateRequest {
  type?: ServiceType
  description?: string
  cost?: number
  serviceOdometer?: number
  serviceDate?: string
}

export interface MaintenanceRecordResponse {
  /** 공개 id(12자) */
  id: string
  type: ServiceType
  description: string | null
  cost: number
  serviceOdometer: number
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
  /** 실제 적용 주기. 차량별 설정 우선 */
  intervalKm: number | null
  intervalMonths: number | null
  /** 기본값 덮어씀 여부 */
  customized: boolean
}

/** 차량별 권장 주기. 전체 교체, 둘 다 null 이면 기본값 */
export interface ServiceIntervalRequest {
  intervalKm: number | null
  intervalMonths: number | null
}
