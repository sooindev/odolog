package com.odolog.app.fuel.dto.request.update;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 부분 수정이라 전부 nullable — 보낸 필드만 바뀐다. */
public record FuelRecordUpdateRequest(

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
        String memo
) {
}
