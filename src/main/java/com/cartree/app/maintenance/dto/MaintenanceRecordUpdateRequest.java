package com.cartree.app.maintenance.dto;

import com.cartree.app.maintenance.domain.ServiceType;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MaintenanceRecordUpdateRequest(

        ServiceType type,

        @Size(max = 255)
        String description,

        @PositiveOrZero
        Integer cost,

        @PositiveOrZero
        Integer serviceOdometer,

        LocalDate serviceDate
) {
}
