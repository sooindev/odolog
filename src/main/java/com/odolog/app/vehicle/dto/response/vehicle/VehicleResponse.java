package com.odolog.app.vehicle.dto.response.vehicle;

import com.odolog.app.vehicle.domain.entity.Vehicle;

public record VehicleResponse(
        Long id,
        String plateNumber,
        String manufacturer,
        String modelName,
        Integer modelYear,
        int odometer,
        /**
         * 권장 주기가 지난 정비 종류 수 (2026-09-25)
         * 홈에만 있고 목록에는 없어서, 같은 질문("어느 차에 할 일이 있나")에 두 화면이
         * 다르게 답하고 있었다. 헤더의 '내 차량' 으로 곧장 들어간 사람은 못 봤다
         */
        int overdueServiceCount
) {

    /** 목록 밖(등록·수정·주행거리 갱신)의 단건 응답. 그 자리에서는 지남을 세지 않는다 */
    public static VehicleResponse from(Vehicle vehicle) {
        return of(vehicle, 0);
    }

    public static VehicleResponse of(Vehicle vehicle, int overdueServiceCount) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getPlateNumber(),
                vehicle.getManufacturer(),
                vehicle.getModelName(),
                vehicle.getModelYear(),
                vehicle.getOdometer(),
                overdueServiceCount
        );
    }
}
