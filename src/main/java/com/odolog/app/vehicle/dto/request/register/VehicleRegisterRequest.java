package com.odolog.app.vehicle.dto.request.register;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

        // 화면에만 있던 제한. API 는 null 도 999999 도 통과시켰음
        @NotNull
        @Min(1900)
        @Max(2100)
        Integer modelYear
) {
}
