package com.odolog.app.fuel.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 차량 한 대의 주유 요약
 * 평균 = 구간 거리 합 ÷ 구간 주유량 합. 첫 주유량 제외
 */
public record FuelSummaryResponse(
        int recordCount,
        /** 총 유류비(통화의 최소 단위). 홈 요약과 같은 long */
        long totalCost,
        /** totalCost 의 통화(사용자 설정) */
        String currency,
        /** 통화가 달라 totalCost 에서 뺀 기록 수 */
        int otherCurrencyRecordCount,
        BigDecimal totalLiters,
        /** 첫 기록부터 마지막 기록까지 달린 거리(km). 연비 초기화와 무관. 2건 미만이면 null */
        Integer totalDistance,
        /** 평균 연비. 계산 불가면 null */
        BigDecimal averageEfficiency,

        /** 최근 주유 기록 id. 없으면 null. 연비 초기화 대상 */
        /** 공개 id */
        String latestRecordId,
        /** 적용 중인 기준점 id. 없으면 null */
        String resetPointId,

        /** 기록 누락으로 보여 평균에서 뺀 구간 수 */
        int longSegmentCount,

        /** 불가능한 값이라 평균에서 뺀 구간 수. 고치는 방법이 달라 위와 별도 집계 */
        int excludedSegmentCount,

        /** 최근 구간 연비(오래된 것부터). 평균에서 뺀 구간 제외 */
        List<TrendPoint> trend
) {

    public record TrendPoint(LocalDate fueledAt, BigDecimal efficiency) {
    }
}
