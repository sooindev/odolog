package com.odolog.app.maintenance.dto.request;

import com.odolog.app.maintenance.domain.ServiceType;
import com.odolog.app.common.validation.InputLimits;
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
        LocalDate serviceDate,

        /** 비용 비움. 키 없음과 null 이 같게 도착해 플래그 별도(주유와 같은 방식) */
        Boolean clearCost,

        /** 주행거리 비움 */
        Boolean clearServiceOdometer
) {
}
