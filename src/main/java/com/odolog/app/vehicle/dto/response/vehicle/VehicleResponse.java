package com.odolog.app.vehicle.dto.response.vehicle;

import com.odolog.app.vehicle.domain.entity.Vehicle;

public record VehicleResponse(
        /** 공개 id(12자). 숫자 PK 가 아니다 */
        String id,
        String plateNumber,
        String manufacturer,
        String modelName,
        Integer modelYear,
        int odometer,
        /**
         * 권장 주기가 지난 정비 종류 수 (2026-09-25)
         * 홈에만 있고 목록에는 없어서, 같은 질문("어느 차에 할 일이 있나")에 두 화면이
         * 다르게 답하고 있었다. 헤더의 '내 차량' 으로 곧장 들어간 사람은 못 봤다
         *
         * **목록 조회에서만 채워진다. 그 밖에서는 0 이 아니라 null 이다** —
         * 0 은 "지난 게 없다" 라는 뜻이고 여기서 하려는 말은 "안 셌다" 다.
         * 연비가 구간 미성립일 때 0 이 아니라 null 인 것과 같은 규칙
         */
        Integer overdueServiceCount
) {

    /** 목록 밖(등록·수정·주행거리 갱신)의 단건 응답. 그 자리에서는 지남을 세지 않는다 */
    public static VehicleResponse from(Vehicle vehicle) {
        return of(vehicle, null);
    }

    public static VehicleResponse of(Vehicle vehicle, Integer overdueServiceCount) {
        return new VehicleResponse(
                // 숫자 PK 가 아니라 공개 id. 화면은 이 값만 안다
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
