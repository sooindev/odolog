package com.cartree.app.maintenance.dto;

import com.cartree.app.maintenance.domain.ServiceType;

public record NextServiceResponse(
        ServiceType type,
        Integer lastServiceOdometer,
        Integer nextServiceOdometer
) {
}
