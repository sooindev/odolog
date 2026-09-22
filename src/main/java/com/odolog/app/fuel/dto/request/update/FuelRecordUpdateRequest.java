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
 * 부분 수정이라 전부 nullable — 보낸 필드만 변경
 *
 * liters·totalCost 만 상태가 셋이라(유지 / 변경 / 비움) 값 옆에 clear 플래그를 따로 둔다.
 *
 * 왜 null 하나로 안 되나: JSON 에서 "키가 없음" 과 "키는 있고 값이 null" 은 서버에
 * 똑같이 null 로 도착한다. 그래서 null 을 "비움" 으로 읽으면 **메모만 고치는 요청이
 * 주유량을 함께 지운다.**
 *
 * Optional 로 감싸는 방법을 먼저 해 봤고 실패했다 — Jackson 은 키가 아예 없을 때도
 * Optional.empty() 를 채워 넣어서, 결국 둘이 또 같아진다(테스트로 확인).
 * 불리언 하나가 덜 우아해 보여도 이쪽은 헷갈릴 여지가 없다
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

        /** 주유량을 비운다. 값을 함께 보내면 비우기가 이긴다 */
        Boolean clearLiters,

        /** 결제 금액을 비운다 */
        Boolean clearTotalCost,

        @Size(max = 255)
        String memo,

        /**
         * 연비 기준점 표시/해제. null 이면 유지
         * 전용 엔드포인트 대신 부분 수정에 얹음 — 기준점은 그 기록의 속성이지 차량의 동작이 아님
         */
        Boolean resetPoint
) {
}
