package com.odolog.app.maintenance.dto.request.update;

import com.odolog.app.maintenance.domain.type.ServiceType;
import com.odolog.app.common.validation.limit.InputLimits;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MaintenanceRecordUpdateRequest(

        ServiceType type,

        @Size(max = 255)
        String description,

        @PositiveOrZero
        @Max(InputLimits.MAX_AMOUNT)
        Integer cost,

        @PositiveOrZero
        @Max(InputLimits.MAX_ODOMETER)
        Integer serviceOdometer,

        @PastOrPresent
        LocalDate serviceDate
) {
}
