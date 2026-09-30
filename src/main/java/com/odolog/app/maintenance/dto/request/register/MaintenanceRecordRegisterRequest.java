package com.odolog.app.maintenance.dto.request.register;

import com.odolog.app.maintenance.domain.type.ServiceType;
import com.odolog.app.common.validation.limit.InputLimits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MaintenanceRecordRegisterRequest(

        @NotNull
        ServiceType type,

        @Size(max = 255)
        String description,

        // 비용·주행거리는 선택. 모르면 비움(0 으로 채우지 않음)
        @PositiveOrZero
        @Max(InputLimits.MAX_AMOUNT)
        Integer cost,

        @PositiveOrZero
        @Max(InputLimits.MAX_ODOMETER)
        Integer serviceOdometer,

        @NotNull
        LocalDate serviceDate
) {
}
