/**
 * 백엔드 com.odolog.app.vehicle.dto 에 대응하는 타입.
 * 백엔드 DTO를 고치면 이 파일도 같이 고쳐야 한다 (자동 동기화되지 않음).
 */

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
  /**
   * 등록 API는 @NotNull 이지만 DB 컬럼은 여전히 nullable 이라,
   * 검증이 붙기 전에 만들어진 차량은 null 일 수 있다.
   */
  modelYear: number | null
  odometer: number
}
