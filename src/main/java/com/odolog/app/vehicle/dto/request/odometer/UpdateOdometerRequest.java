package com.odolog.app.vehicle.dto.request.odometer;

import jakarta.validation.constraints.PositiveOrZero;

public record UpdateOdometerRequest(

        @PositiveOrZero
        int odometer
) {
}
