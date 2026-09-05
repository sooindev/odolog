package com.cartree.app.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record UpdateOdometerRequest(

        @PositiveOrZero
        int odometer
) {
}
