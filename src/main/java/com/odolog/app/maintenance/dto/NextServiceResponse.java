package com.odolog.app.maintenance.dto;

import com.odolog.app.maintenance.domain.ServiceType;

import java.time.LocalDate;

public record NextServiceResponse(
        ServiceType type,
        Integer lastServiceOdometer,
        Integer nextServiceOdometer,
        LocalDate lastServiceDate,
        LocalDate nextServiceDate
) {
}
