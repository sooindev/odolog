/** 백엔드 vehicle DTO 대응. 백엔드 변경 시 함께 수정 */

export interface VehicleRegisterRequest {
  plateNumber: string
  manufacturer: string
  modelName: string
  modelYear: number
}

/** 부분 수정. 주행거리는 전용 엔드포인트 */
export interface VehicleUpdateRequest {
  plateNumber?: string
  manufacturer?: string
  modelName?: string
  modelYear?: number
}

export interface UpdateOdometerRequest {
  odometer: number
  /** 감소 허용 여부. 계기판 교체·자리수 오타 정정 때만 */
  force?: boolean
}

export interface VehicleResponse {
  /** 공개 id(12자) */
  id: string
  plateNumber: string
  manufacturer: string
  modelName: string
  /** 예전 데이터는 null 가능 */
  modelYear: number | null
  odometer: number
  /** 권장 주기가 지난 정비 종류 수. 목록 조회에서만 채움, 그 밖에는 null */
  overdueServiceCount: number | null
}
