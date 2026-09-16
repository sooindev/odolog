/**
 * 백엔드 com.odolog.app.maintenance.dto 에 대응하는 타입.
 * 백엔드 DTO를 고치면 이 파일도 같이 고쳐야 한다 (자동 동기화되지 않음).
 */

/**
 * 순서가 백엔드 enum 선언 순서와 같아야 한다 — 선택 목록이 이 순서로 그려지고,
 * `/next-services` 응답도 서버가 같은 순서로 담아 준다.
 * 알파벳순이 아니라 정비 부위별로 묶여 있다(엔진·구동 / 제동 / 타이어·조향 / 소모품).
 */
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

/** 선택 목록을 구역으로 나눈다. 15개를 한 줄로 늘어놓으면 찾는 데 시간이 걸린다. */
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
  id: number
  type: ServiceType
  description: string | null
  cost: number
  serviceOdometer: number
  /** YYYY-MM-DD */
  serviceDate: string
}

export interface NextServiceResponse {
  type: ServiceType
  /** 해당 종류의 이력이 없으면 null */
  lastServiceOdometer: number | null
  /** 이력이 없거나 권장 주기가 없는 종류(OTHER)면 null */
  nextServiceOdometer: number | null
  lastServiceDate: string | null
  nextServiceDate: string | null
}
