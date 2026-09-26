package com.odolog.app.fuel.dto.request.register;

import com.odolog.app.common.validation.limit.InputLimits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 숫자는 래퍼 타입. int 면 누락 시 0 이 채워져 검증 통과
 * liters 는 @Positive. 0L 이면 0 으로 나누기
 * liters·totalCost 는 선택 입력(@NotNull 없음). 나머지 제약은 null 통과
 */
public record FuelRecordRegisterRequest(

        @NotNull
        @PastOrPresent
        LocalDate fueledAt,

        @NotNull
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

        @Size(max = 255)
        String memo
) {
}
