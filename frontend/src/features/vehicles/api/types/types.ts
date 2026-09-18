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
  /**
   * 감소를 허용할지. 안 보내면 서버가 막음
   * 계기판 교체·자리수 오타 정정에만 실어 보냄 — 실수로 낮추는 것은 그대로 걸려야 함
   */
  force?: boolean
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
