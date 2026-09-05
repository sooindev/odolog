package com.odolog.app.vehicle.dto.request;

import jakarta.validation.constraints.NotBlank;
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

        Integer modelYear
) {
}
