package com.odolog.app.maintenance.dto.response.record;

import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.type.ServiceType;

import java.time.LocalDate;

public record MaintenanceRecordResponse(
        /** 공개 id(12자) */
        String id,
        ServiceType type,
        String description,
        int cost,
        int serviceOdometer,
        LocalDate serviceDate
) {

    public static MaintenanceRecordResponse from(MaintenanceRecord record) {
        return new MaintenanceRecordResponse(
                record.getPublicId(),
                record.getType(),
                record.getDescription(),
                record.getCost(),
                record.getServiceOdometer(),
                record.getServiceDate()
        );
    }
}
