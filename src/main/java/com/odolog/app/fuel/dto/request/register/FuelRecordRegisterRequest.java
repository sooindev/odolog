package com.odolog.app.fuel.dto.request.register;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 숫자는 전부 래퍼 타입. int 면 필드 누락 시 Jackson 이 0 을 채우고 PositiveOrZero 가 통과시킴
 * liters 만 Positive — 0L 이면 연비 계산이 0 으로 나누기
 */
public record FuelRecordRegisterRequest(

        @NotNull
        LocalDate fueledAt,

        @NotNull
        @PositiveOrZero
        Integer odometer,

        @NotNull
        @Positive
        @Digits(integer = 4, fraction = 2)
        @DecimalMax("9999.99")
        BigDecimal liters,

        @NotNull
        @PositiveOrZero
        Integer totalCost,

        @Size(max = 255)
        String memo
) {
}
