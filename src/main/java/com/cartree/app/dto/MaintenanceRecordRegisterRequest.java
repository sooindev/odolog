package com.cartree.app.dto;

import com.cartree.app.domain.ServiceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MaintenanceRecordRegisterRequest(

        @NotNull
        ServiceType type,

        @Size(max = 255)
        String description,

        @PositiveOrZero
        int cost,

        @PositiveOrZero
        int serviceOdometer,

        @NotNull
        LocalDate serviceDate
) {
}
