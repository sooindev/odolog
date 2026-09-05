/**
 * 백엔드(com.odolog.app.*.dto)의 DTO에 대응하는 타입.
 * 스펙은 http://localhost:8080/v3/api-docs 에서 확인할 수 있다.
 * 백엔드 DTO를 고치면 이 파일도 같이 고쳐야 한다 (자동 동기화되지 않음).
 */

export const SERVICE_TYPES = [
  'ENGINE_OIL',
  'TIRE',
  'BRAKE_PAD',
  'BATTERY',
  'OTHER',
] as const

export type ServiceType = (typeof SERVICE_TYPES)[number]

export const SERVICE_TYPE_LABELS: Record<ServiceType, string> = {
  ENGINE_OIL: '엔진오일',
  TIRE: '타이어',
  BRAKE_PAD: '브레이크 패드',
  BATTERY: '배터리',
  OTHER: '기타',
}

/** 백엔드 common.dto.PageResponse */
export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
}

/** 백엔드 common.dto.ErrorResponse */
export interface ErrorResponse {
  message: string
}

// ── user ─────────────────────────────────────────────
export interface SignUpRequest {
  email: string
  password: string
  nickname: string
  phone: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface UpdateProfileRequest {
  nickname?: string
  phone?: string
}

export interface UserResponse {
  id: number
  email: string
  nickname: string
  phone: string | null
}

// ── vehicle ──────────────────────────────────────────
export interface VehicleRegisterRequest {
  plateNumber: string
  manufacturer: string
  modelName: string
  modelYear: number
}

export interface UpdateOdometerRequest {
  odometer: number
}

export interface VehicleResponse {
  id: number
  plateNumber: string
  manufacturer: string
  modelName: string
  modelYear: number
  odometer: number
}

// ── maintenance ──────────────────────────────────────
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
