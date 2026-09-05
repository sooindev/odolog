package com.odolog.app.maintenance.dto;

import com.odolog.app.maintenance.domain.ServiceType;

public record NextServiceResponse(
        ServiceType type,
        Integer lastServiceOdometer,
        Integer nextServiceOdometer
) {
}
