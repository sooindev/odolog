package com.odolog.app.maintenance.dto.response;

import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.ServiceType;

import java.time.LocalDate;

public record MaintenanceRecordResponse(
        /** 공개 id(12자) */
        String id,
        ServiceType type,
        String description,
        /** 안 적었으면 null */
        Integer cost,
        String currency,
        /** 안 적었으면 null */
        Integer serviceOdometer,
        LocalDate serviceDate
) {

    public static MaintenanceRecordResponse from(MaintenanceRecord record) {
        return new MaintenanceRecordResponse(
                record.getPublicId(),
                record.getType(),
                record.getDescription(),
                record.getCost(),
                record.getCurrency(),
                record.getServiceOdometer(),
                record.getServiceDate()
        );
    }
}
