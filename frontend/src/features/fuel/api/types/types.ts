/**
 * 백엔드 com.odolog.app.fuel.dto 대응
 * 자동 동기화가 아니므로 백엔드 DTO 를 고치면 여기도 함께
 */

export interface FuelRecordRegisterRequest {
  fueledAt: string
  odometer: number
  /**
   * 백엔드는 BigDecimal(6,2). JSON 에서는 숫자
   * 모르면 null — 영수증을 잃었거나 계기판만 적어 두는 경우가 있다.
   * 0 을 보내면 안 된다. 0L 을 넣었다는 말이 되고 서버의 @Positive 에 막힌다
   */
  liters: number | null
  totalCost: number | null
  memo?: string
}

/**
 * 부분 수정이라 전부 선택 — 보낸 필드만 변경
 *
 * liters·totalCost 만 상태가 셋이라(유지 / 변경 / 비움) clear 플래그가 따로 있다.
 * null 하나로는 안 되는 이유: JSON 에서 "키가 없음" 과 "값이 null" 은 서버에 똑같이
 * 도착해서, null 을 비움으로 읽으면 메모만 고치는 요청이 주유량을 지운다
 */
export interface FuelRecordUpdateRequest {
  fueledAt?: string
  odometer?: number
  liters?: number
  totalCost?: number
  /** 주유량을 비운다 */
  clearLiters?: boolean
  /** 결제 금액을 비운다 */
  clearTotalCost?: boolean
  memo?: string
  /** 연비 기준점 표시/해제. 안 보내면 유지 */
  resetPoint?: boolean
}

export interface FuelRecordResponse {
  /** 공개 id(12자). 숫자 PK 는 서버 밖으로 안 나온다 */
  id: string
  fueledAt: string
  odometer: number
  /** 안 적었으면 null. 0 이 아니다 — 0L 을 넣었다는 말이 되고 연비가 0 으로 나누기가 된다 */
  liters: number | null
  /** 안 적었으면 null. 합계에서는 0 으로 치지만 단가 계산에서는 빠진다 */
  totalCost: number | null
  memo: string | null
  /** 연비 재계산 기준점인지. 직전과의 연결이 끊겨 이 기록의 구간 연비도 null */
  resetPoint: boolean

  /** 리터당 단가(원). 서버가 반올림한 표시용 값. 주유량·금액 중 하나라도 없으면 null */
  pricePerLiter: number | null
  /**
   * 직전 주유 이후 거리(km). 아래 셋은 서버 계산값
   * 첫 기록·구간 미성립이면 0 이 아니라 null — 0 이면 "연비 0km/L" 라는 틀린 값이 찍힘
   */
  distance: number | null
  /** 연비(km/L) */
  efficiency: number | null
  /**
   * 물리적으로 불가능한 연비인지(50 초과 · 2 미만) = 입력 오류
   * 25 는 불가능한 값이 아니라 빠진 기록은 여기서 안 잡힘 — 아래가 담당
   */
  efficiencySuspicious: boolean
  /**
   * 이 구간에 주유 기록이 빠진 것으로 보이는지 = 안 적었거나 지운 자리
   * 이 구간은 평균에서도 빠져 있음 — 표시가 없으면 목록 숫자와 평균이 안 맞아 보임
   */
  missingRecordSuspected: boolean
}

export interface FuelSummaryResponse {
  recordCount: number
  totalCost: number
  totalLiters: number
  /** 2건 미만이면 null */
  totalDistance: number | null
  averageEfficiency: number | null
  /** 최근 주유 기록. 연비 초기화가 찍을 대상. 없으면 null */
  latestRecordId: string | null
  /** 적용 중인 기준점. 없으면 null */
  resetPointId: string | null
  /** 평소보다 긴 구간 수 = 기록을 빼먹었을 가능성 */
  longSegmentCount: number
  /** 물리적으로 불가능해서 평균에서 뺀 구간 수. 뺐으면 화면이 말해야 함 */
  excludedSegmentCount: number
  /**
   * 최근 구간 연비 (오래된 것부터, 최대 12개)
   * 평균 하나로는 추세를 알 수 없다 — 연비가 꾸준히 떨어지는 것은 그 자체로 정비 신호다
   */
  trend: { fueledAt: string; efficiency: number }[]
}
