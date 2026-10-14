package com.odolog.app.vehicle.dto.request;

import com.odolog.app.common.validation.InputLimits;
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
         * 자리수 오타 정정용. 계기판 교체는 기록이 주행거리순이라 연비·다음 정비가 맞지 않음
         */
        Boolean force
) {

    public boolean forced() {
        return Boolean.TRUE.equals(force);
    }
}
