package com.odolog.app.fuel.dto.response.summary;

import java.math.BigDecimal;

/**
 * 차량 한 대의 주유 요약
 * 평균 = 구간 거리 합 ÷ 구간 주유량 합. 구간 연비들의 평균이 아님 — 30km 와 600km 가 같은 무게가 됨
 * 첫 주유량은 제외 — 첫 기록 이전 구간의 연료라 거리와 짝이 안 맞음
 */
public record FuelSummaryResponse(
        int recordCount,
        /**
         * 총 유류비(원). int 가 아닌 이유 — 홈 요약(GarageSummaryResponse.fuelCost)이 long 이라
         * 둘이 다르면 같은 값을 두 화면이 다르게 말한다. 실제로 40억 원에서 음수가 나왔다
         */
        long totalCost,
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

        /**
         * 기록이 빠진 것으로 보여 평균에서 뺀 구간 수
         * 거리와 연비가 둘 다 그 차의 평소를 크게 넘는 구간이다 — 기록을 빼먹었거나 지운 자리
         */
        int longSegmentCount,

        /**
         * 값이 물리적으로 불가능해서 평균에서 뺀 구간 수
         * 위와 나눠 세는 이유는 고치는 방법이 달라서다 — 이쪽은 적힌 값을 고치는 일이고
         * 위는 없는 기록을 채우는 일이다. 뺐으면 화면이 그 사실을 말해야 한다
         */
        int excludedSegmentCount
) {
}
