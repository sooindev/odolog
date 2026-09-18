package com.odolog.app.fuel.dto.request.update;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 부분 수정이라 전부 nullable — 보낸 필드만 변경 */
public record FuelRecordUpdateRequest(

        @PastOrPresent
        LocalDate fueledAt,

        @PositiveOrZero
        Integer odometer,

        @Positive
        @Digits(integer = 4, fraction = 2)
        @DecimalMax("9999.99")
        BigDecimal liters,

        @PositiveOrZero
        Integer totalCost,

        @Size(max = 255)
        String memo,

        /**
         * 연비 기준점 표시/해제. null 이면 유지
         * 전용 엔드포인트 대신 부분 수정에 얹음 — 기준점은 그 기록의 속성이지 차량의 동작이 아님
         */
        Boolean resetPoint
) {
}
