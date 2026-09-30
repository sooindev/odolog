package com.odolog.app.vehicle.dto.request.register;

import com.odolog.app.common.validation.limit.InputLimits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record VehicleRegisterRequest(

        @NotBlank
        @Size(max = 20)
        String plateNumber,

        @NotBlank
        @Size(max = 50)
        String manufacturer,

        @NotBlank
        @Size(max = 100)
        String modelName,

        // 연식 범위. 화면과 같은 제한
        @NotNull
        @Min(1900)
        @Max(2100)
        Integer modelYear,

        // 지금 계기판 값. 타던 차가 0km 로 시작하면 첫 기록부터 급증 판정·다음 정비가 어긋남
        @NotNull
        @PositiveOrZero
        @Max(InputLimits.MAX_ODOMETER)
        Integer odometer
) {
}
