package com.odolog.app.vehicle.dto;

import com.odolog.app.vehicle.domain.Vehicle;

public record VehicleResponse(
        Long id,
        String plateNumber,
        String manufacturer,
        String modelName,
        Integer modelYear,
        int odometer
) {

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getPlateNumber(),
                vehicle.getManufacturer(),
                vehicle.getModelName(),
                vehicle.getModelYear(),
                vehicle.getOdometer()
        );
    }
}
