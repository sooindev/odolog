package com.odolog.app.fuel.dto.request.register;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 숫자는 전부 래퍼 타입. int 면 필드 누락 시 Jackson 이 0 을 채우고 PositiveOrZero 가 통과시킴
 * liters 만 Positive — 0L 이면 연비 계산이 0 으로 나누기
 *
 * liters·totalCost 에는 @NotNull 이 없다. 영수증을 잃어버렸거나 계기판만 적어 두고 싶은 경우가
 * 있어 비워서 저장할 수 있다 — 대신 화면이 저장 전에 무엇을 못 하게 되는지 알려 준다.
 * @Positive·@Digits 같은 나머지 제약은 그대로 둔다. 값을 적었다면 여전히 말이 되어야 하고,
 * 이 애노테이션들은 전부 null 을 통과시킨다
 */
public record FuelRecordRegisterRequest(

        @NotNull
        @PastOrPresent
        LocalDate fueledAt,

        @NotNull
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
