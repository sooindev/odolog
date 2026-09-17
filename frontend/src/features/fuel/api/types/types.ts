/**
 * 백엔드 com.odolog.app.fuel.dto 대응
 * 자동 동기화가 아니므로 백엔드 DTO 를 고치면 여기도 함께
 */

export interface FuelRecordRegisterRequest {
  fueledAt: string
  odometer: number
  /** 백엔드는 BigDecimal(6,2). JSON 에서는 숫자 */
  liters: number
  totalCost: number
  memo?: string
}

/** 부분 수정이라 전부 선택 — 보낸 필드만 변경 */
export interface FuelRecordUpdateRequest {
  fueledAt?: string
  odometer?: number
  liters?: number
  totalCost?: number
  memo?: string
  /** 연비 기준점 표시/해제. 안 보내면 유지 */
  resetPoint?: boolean
}

export interface FuelRecordResponse {
  id: number
  fueledAt: string
  odometer: number
  liters: number
  totalCost: number
  memo: string | null
  /** 연비 재계산 기준점인지. 직전과의 연결이 끊겨 이 기록의 구간 연비도 null */
  resetPoint: boolean

  /** 리터당 단가(원). 서버가 반올림한 표시용 값 */
  pricePerLiter: number
  /**
   * 직전 주유 이후 거리(km). 아래 셋은 서버 계산값
   * 첫 기록·구간 미성립이면 0 이 아니라 null — 0 이면 "연비 0km/L" 라는 틀린 값이 찍힘
   */
  distance: number | null
  /** 연비(km/L) */
  efficiency: number | null
  /**
   * 물리적으로 불가능한 연비인지(50 초과 · 2 미만) = 입력 오류
   * 빠진 기록은 여기서 안 잡힘 — 25 는 불가능한 값이 아니라서. longSegmentCount 담당
   */
  efficiencySuspicious: boolean
}

export interface FuelSummaryResponse {
  recordCount: number
  totalCost: number
  totalLiters: number
  /** 2건 미만이면 null */
  totalDistance: number | null
  averageEfficiency: number | null
  /** 최근 주유 기록. 연비 초기화가 찍을 대상. 없으면 null */
  latestRecordId: number | null
  /** 적용 중인 기준점. 없으면 null */
  resetPointId: number | null
  /** 평소보다 긴 구간 수 = 기록을 빼먹었을 가능성 */
  longSegmentCount: number
}
