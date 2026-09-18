package com.odolog.app.fuel.dto.response.summary;

import java.math.BigDecimal;

/**
 * 차량 한 대의 주유 요약
 * 평균 = 구간 거리 합 ÷ 구간 주유량 합. 구간 연비들의 평균이 아님 — 30km 와 600km 가 같은 무게가 됨
 * 첫 주유량은 제외 — 첫 기록 이전 구간의 연료라 거리와 짝이 안 맞음
 */
public record FuelSummaryResponse(
        int recordCount,
        int totalCost,
        BigDecimal totalLiters,
        /** 평균에 실제로 쓴 구간 거리의 합(km). 2건 미만이면 null */
        Integer totalDistance,
        /** 구간 거리 합 ÷ 구간 주유량 합. 계산 불가면 null */
        BigDecimal averageEfficiency,

        /**
         * 최근 주유 기록 id. 없으면 null
         * 연비 초기화가 찍을 대상 — 없으면 화면이 목록을 한 번 더 받아야 함
         */
        Long latestRecordId,
        /** 적용 중인 기준점 id. 없으면 null — 해제 버튼 표시 여부 */
        Long resetPointId,

        /** 평소보다 긴 구간 수 = 기록을 빼먹었을 가능성 */
        int longSegmentCount,

        /**
         * 물리적으로 불가능해서 평균에서 뺀 구간 수
         * 뺐으면 화면이 그 사실을 말해야 한다 — 말없이 빼면 그것도 거짓말이다
         */
        int excludedSegmentCount
) {
}
