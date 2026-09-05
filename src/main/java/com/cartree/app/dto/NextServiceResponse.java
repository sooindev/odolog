package com.cartree.app.dto;

import com.cartree.app.domain.ServiceType;

public record NextServiceResponse(
        ServiceType type,
        Integer lastServiceOdometer,
        Integer nextServiceOdometer
) {
}
