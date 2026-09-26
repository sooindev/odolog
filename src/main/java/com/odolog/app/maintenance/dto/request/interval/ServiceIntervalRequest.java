package com.odolog.app.maintenance.dto.request.interval;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

/**
 * 차량별 권장 주기. 부분 수정이 아닌 전체 교체
 * 둘 다 null 이면 기본값 복귀
 */
public record ServiceIntervalRequest(

        /** 주행거리 주기(km). null 이면 기본값 */
        @Positive
        @Max(500_000)
        Integer intervalKm,

        /** 기간 주기(개월). null 이면 기본값, 상한 10년 */
        @Positive
        @Max(120)
        Integer intervalMonths
) {
}
