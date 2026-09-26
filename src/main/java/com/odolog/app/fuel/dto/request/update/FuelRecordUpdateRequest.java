package com.odolog.app.fuel.dto.request.update;

import com.odolog.app.common.validation.limit.InputLimits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 부분 수정. 보낸 필드만 변경
 * liters·totalCost 는 유지·변경·비움 세 상태라 clear 플래그 별도
 * JSON 의 키 없음과 null 이 같게 도착해서 null 로는 비움 표현 불가. Optional 도 같은 문제
 */
public record FuelRecordUpdateRequest(

        @PastOrPresent
        LocalDate fueledAt,

        @PositiveOrZero
        @Max(InputLimits.MAX_ODOMETER)
        Integer odometer,

        @Positive
        @Digits(integer = 4, fraction = 2)
        @DecimalMax("9999.99")
        BigDecimal liters,

        @PositiveOrZero
        @Max(InputLimits.MAX_AMOUNT)
        Integer totalCost,

        /** 주유량 비움. 값과 함께 오면 비움 우선 */
        Boolean clearLiters,

        /** 결제 금액 비움 */
        Boolean clearTotalCost,

        @Size(max = 255)
        String memo,

        /** 연비 기준점 표시/해제. null 이면 유지 */
        Boolean resetPoint
) {
}
