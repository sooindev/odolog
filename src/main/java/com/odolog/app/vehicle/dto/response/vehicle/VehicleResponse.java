package com.odolog.app.vehicle.dto.response.vehicle;

import com.odolog.app.vehicle.domain.entity.Vehicle;

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
