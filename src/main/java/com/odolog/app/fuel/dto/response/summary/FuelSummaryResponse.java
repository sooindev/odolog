package com.odolog.app.fuel.dto.response.summary;

import java.math.BigDecimal;

/**
 * 차량 한 대의 주유 요약
 * 평균 = 총 거리 ÷ 총 주유량. 구간 연비들의 평균이 아님 — 30km 와 600km 가 같은 무게가 됨
 * 첫 주유량은 제외 — 첫 기록 이전 구간의 연료라 거리와 짝이 안 맞음
 */
public record FuelSummaryResponse(
        int recordCount,
        int totalCost,
        BigDecimal totalLiters,
        /** 첫 기록 → 마지막 기록 주행거리(km). 2건 미만이면 null */
        Integer totalDistance,
        /** 총 거리 ÷ (총 주유량 − 첫 주유량). 계산 불가면 null */
        BigDecimal averageEfficiency,

        /**
         * 최근 주유 기록 id. 없으면 null
         * 연비 초기화가 찍을 대상 — 없으면 화면이 목록을 한 번 더 받아야 함
         */
        Long latestRecordId,
        /** 적용 중인 기준점 id. 없으면 null — 해제 버튼 표시 여부 */
        Long resetPointId,

        /** 평소보다 긴 구간 수 = 기록을 빼먹었을 가능성 */
        int longSegmentCount
) {
}
