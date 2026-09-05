package com.odolog.app.vehicle.dto.request;

import jakarta.validation.constraints.PositiveOrZero;

public record UpdateOdometerRequest(

        @PositiveOrZero
        int odometer
) {
}
