package com.odolog.app.vehicle.dto.response.vehicle;

import com.odolog.app.vehicle.domain.entity.Vehicle;

public record VehicleResponse(
        /** 공개 id(12자) */
        String id,
        String plateNumber,
        String manufacturer,
        String modelName,
        Integer modelYear,
        int odometer,
        /**
         * 권장 주기가 지난 정비 종류 수
         * 목록 조회에서만 채움. 그 밖에서는 null(0 은 지난 것 없음이라는 뜻)
         */
        Integer overdueServiceCount
) {

    /** 목록 밖 단건 응답. 지남 수 미집계 */
    public static VehicleResponse from(Vehicle vehicle) {
        return of(vehicle, null);
    }

    public static VehicleResponse of(Vehicle vehicle, Integer overdueServiceCount) {
        return new VehicleResponse(
                // 공개 id
                vehicle.getPublicId(),
                vehicle.getPlateNumber(),
                vehicle.getManufacturer(),
                vehicle.getModelName(),
                vehicle.getModelYear(),
                vehicle.getOdometer(),
                overdueServiceCount
        );
    }
}
