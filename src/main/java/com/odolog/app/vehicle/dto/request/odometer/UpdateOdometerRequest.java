package com.odolog.app.vehicle.dto.request.odometer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateOdometerRequest(

        @NotNull
        @PositiveOrZero
        Integer odometer
) {
}
