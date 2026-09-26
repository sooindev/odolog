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
         * 감소 허용 여부. null 이면 false
         * 계기판 교체·자리수 오타 정정용
         */
        Boolean force
) {

    public boolean forced() {
        return Boolean.TRUE.equals(force);
    }
}
