package com.odolog.app.fuel.dto.response.summary;

import java.math.BigDecimal;

/**
 * 차량 한 대의 주유 요약.
 *
 * <p>평균 연비를 "구간 연비들의 평균"으로 내지 않는다. 구간 길이가 제각각이라
 * 30km 구간과 600km 구간이 같은 무게로 들어가면 짧은 구간의 오차가 전체를 흔든다.
 * 대신 <b>총 거리 ÷ 총 주유량</b> 으로 낸다.
 *
 * <p>이때 <b>첫 기록의 주유량은 빼야 한다.</b> 그 연료는 첫 기록 이전 구간을 달리는 데 쓴 것이라
 * 우리가 아는 거리(첫 기록 → 마지막 기록)와 짝이 맞지 않는다. 안 빼면 연비가 실제보다 낮게 나온다.
 */
public record FuelSummaryResponse(
        int recordCount,
        int totalCost,
        BigDecimal totalLiters,
        /** 첫 기록 → 마지막 기록 사이의 주행거리(km). 기록이 2건 미만이면 null. */
        Integer totalDistance,
        /** 총 거리 ÷ (총 주유량 − 첫 주유량). 계산할 수 없으면 null. */
        BigDecimal averageEfficiency
) {
}
