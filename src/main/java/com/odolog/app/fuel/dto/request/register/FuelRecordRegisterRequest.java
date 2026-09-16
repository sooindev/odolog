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
 * 숫자를 전부 래퍼 타입으로 받는다. int 로 두면 필드를 안 보냈을 때 Jackson 이 조용히 0 을 채우고
 * @PositiveOrZero 가 그 0 을 통과시킨다 — 2026-09-13 점검에서 정비 이력이 실제로 그랬다.
 *
 * liters 는 @Positive 다(@PositiveOrZero 가 아니다). 0L 을 넣으면 연비 계산이 0 으로 나누기가 된다.
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
