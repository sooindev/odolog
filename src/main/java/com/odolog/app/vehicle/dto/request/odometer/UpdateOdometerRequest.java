package com.odolog.app.vehicle.dto.request.odometer;

import com.odolog.app.common.validation.limit.InputLimits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateOdometerRequest(

        @NotNull
        @PositiveOrZero
        @Max(InputLimits.MAX_ODOMETER)
        Integer odometer,

        /**
         * 감소를 허용할지. null 이면 false
         * 기본이 막는 쪽이라 실수로 낮추는 것은 그대로 걸리고, 낮추려면 의도를 밝혀야 함
         * 계기판 교체·자리수 오타 정정이 이 플래그를 쓰는 자리
         */
        Boolean force
) {

    public boolean forced() {
        return Boolean.TRUE.equals(force);
    }
}
