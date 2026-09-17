/**
 * 백엔드 com.odolog.app.vehicle.dto 대응
 * 자동 동기화가 아니므로 백엔드 DTO 를 고치면 여기도 함께
 */

export interface VehicleRegisterRequest {
  plateNumber: string
  manufacturer: string
  modelName: string
  modelYear: number
}

/**
 * 부분 수정이라 전부 선택 — 보낸 필드만 변경
 * odometer 제외 — 감소 금지 규칙이 붙어 전용 엔드포인트 사용
 */
export interface VehicleUpdateRequest {
  plateNumber?: string
  manufacturer?: string
  modelName?: string
  modelYear?: number
}

export interface UpdateOdometerRequest {
  odometer: number
}

export interface VehicleResponse {
  id: number
  plateNumber: string
  manufacturer: string
  modelName: string
  /** 등록 API 는 NotNull 이지만 컬럼은 nullable — 검증 이전 데이터는 null 가능 */
  modelYear: number | null
  odometer: number
}
