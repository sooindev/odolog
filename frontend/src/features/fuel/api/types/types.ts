/**
 * 백엔드 com.odolog.app.fuel.dto 에 대응하는 타입.
 * 백엔드 DTO를 고치면 이 파일도 같이 고쳐야 한다 (자동 동기화되지 않음).
 */

export interface FuelRecordRegisterRequest {
  fueledAt: string
  odometer: number
  /** 백엔드는 BigDecimal(6,2). JSON 으로는 숫자로 오간다. */
  liters: number
  totalCost: number
  memo?: string
}

/** 부분 수정이라 전부 선택 — 보낸 필드만 바뀐다. */
export interface FuelRecordUpdateRequest {
  fueledAt?: string
  odometer?: number
  liters?: number
  totalCost?: number
  memo?: string
  /** 연비 기준점 표시/해제. 안 보내면 그대로 둔다. */
  resetPoint?: boolean
}

export interface FuelRecordResponse {
  id: number
  fueledAt: string
  odometer: number
  liters: number
  totalCost: number
  memo: string | null
  /**
   * 연비를 여기서부터 다시 세는 기준점인지.
   * 기준점은 직전과의 연결이 끊기므로 이 기록의 구간 연비도 null 이다.
   */
  resetPoint: boolean

  /** 리터당 단가(원). 총액 ÷ 리터를 서버가 반올림한 표시용 값. */
  pricePerLiter: number
  /**
   * 직전 주유 이후 달린 거리(km). 아래 셋은 서버가 계산해서 주는 값이고,
   * **첫 기록이거나 구간이 성립하지 않으면 null** 이다 — 0 이 아니라 null 인 것이 중요하다.
   * 0 으로 두면 "연비 0km/L" 라는 틀린 값이 화면에 찍힌다.
   */
  distance: number | null
  /** 연비(km/L). */
  efficiency: number | null
}

export interface FuelSummaryResponse {
  recordCount: number
  totalCost: number
  totalLiters: number
  /** 기록이 2건 미만이면 null. */
  totalDistance: number | null
  averageEfficiency: number | null
  /** 가장 최근 주유 기록. "연비 초기화"가 이 기록을 기준점으로 찍는다. 기록이 없으면 null. */
  latestRecordId: number | null
  /** 지금 적용 중인 기준점. 없으면 null. */
  resetPointId: number | null
}
