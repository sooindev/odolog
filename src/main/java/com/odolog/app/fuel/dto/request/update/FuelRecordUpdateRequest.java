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
        String memo,

        /**
         * 연비 기준점 표시/해제. null 이면 그대로 둔다.
         *
         * <p>전용 엔드포인트를 따로 두지 않은 이유: "여기서부터 다시 센다"는 건 그 기록의
         * 속성이지 차량의 동작이 아니다. 부분 수정에 얹으면 아무 기록이나 기준점으로
         * 삼거나 풀 수 있고, 엔드포인트도 안 늘어난다.
         */
        Boolean resetPoint
) {
}
