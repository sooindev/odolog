package com.odolog.app.maintenance.dto.response.schedule;

import com.odolog.app.maintenance.domain.type.ServiceType;

import java.time.LocalDate;

public record NextServiceResponse(
        ServiceType type,
        Integer lastServiceOdometer,
        Integer nextServiceOdometer,
        LocalDate lastServiceDate,
        LocalDate nextServiceDate
) {
}
