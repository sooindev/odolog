package com.odolog.app.maintenance.dto.request.update;

import com.odolog.app.maintenance.domain.type.ServiceType;
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
