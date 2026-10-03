package com.odolog.app.vehicle.dto;

import com.odolog.app.vehicle.Vehicle;

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
        Integer overdueServiceCount,
        /** 곧 해야 할 정비 종류 수. 지난 것은 빼고 셈. 목록에서만 채움 */
        Integer dueSoonServiceCount
) {

    /** 목록 밖 단건 응답. 지남 수 미집계 */
    public static VehicleResponse from(Vehicle vehicle) {
        return of(vehicle, null, null);
    }

    public static VehicleResponse of(Vehicle vehicle, Integer overdueServiceCount, Integer dueSoonServiceCount) {
        return new VehicleResponse(
                // 공개 id
                vehicle.getPublicId(),
                vehicle.getPlateNumber(),
                vehicle.getManufacturer(),
                vehicle.getModelName(),
                vehicle.getModelYear(),
                vehicle.getOdometer(),
                overdueServiceCount,
                dueSoonServiceCount
        );
    }
}
