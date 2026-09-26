/** 백엔드 fuel DTO 대응. 백엔드 변경 시 함께 수정 */

export interface FuelRecordRegisterRequest {
  fueledAt: string
  odometer: number
  /** 백엔드 BigDecimal(6,2). 모르면 null, 0 은 서버가 거부 */
  liters: number | null
  totalCost: number | null
  memo?: string
}

/**
 * 부분 수정. 보낸 필드만 변경
 * liters·totalCost 의 비움은 clear 플래그로. JSON 의 키 없음과 null 이 같게 도착
 */
export interface FuelRecordUpdateRequest {
  fueledAt?: string
  odometer?: number
  liters?: number
  totalCost?: number
  /** 주유량 비움 */
  clearLiters?: boolean
  /** 결제 금액 비움 */
  clearTotalCost?: boolean
  memo?: string
  /** 연비 기준점 표시/해제. 안 보내면 유지 */
  resetPoint?: boolean
}

export interface FuelRecordResponse {
  /** 공개 id(12자) */
  id: string
  fueledAt: string
  odometer: number
  /** 안 적었으면 null */
  liters: number | null
  /** 안 적었으면 null. 합계에서만 0 */
  totalCost: number | null
  memo: string | null
  /** 연비 기준점 여부. 이 기록의 구간 연비는 null */
  resetPoint: boolean

  /** 리터당 단가(원). 주유량·금액 중 하나라도 없으면 null */
  pricePerLiter: number | null
  /**
   * 직전 주유 이후 거리(km). 이하 서버 계산값
   * 구간 미성립이면 null
   */
  distance: number | null
  /** 연비(km/L) */
  efficiency: number | null
  /** 물리적으로 불가능한 연비(50 초과·2 미만) = 입력 오류 */
  efficiencySuspicious: boolean
  /** 주유 기록 누락 의심 구간. 평균에서도 제외 */
  missingRecordSuspected: boolean
}

export interface FuelSummaryResponse {
  recordCount: number
  totalCost: number
  totalLiters: number
  /** 2건 미만이면 null */
  totalDistance: number | null
  averageEfficiency: number | null
  /** 최근 주유 기록. 연비 초기화 대상, 없으면 null */
  latestRecordId: string | null
  /** 적용 중인 기준점. 없으면 null */
  resetPointId: string | null
  /** 기록 누락으로 보여 평균에서 뺀 구간 수 */
  longSegmentCount: number
  /** 불가능한 값이라 평균에서 뺀 구간 수 */
  excludedSegmentCount: number
  /** 최근 구간 연비(오래된 것부터, 최대 12개) */
  trend: { fueledAt: string; efficiency: number }[]
}
