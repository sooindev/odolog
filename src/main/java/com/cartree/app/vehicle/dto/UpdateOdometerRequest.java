package com.cartree.app.vehicle.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record UpdateOdometerRequest(

        @PositiveOrZero
        int odometer
) {
}
