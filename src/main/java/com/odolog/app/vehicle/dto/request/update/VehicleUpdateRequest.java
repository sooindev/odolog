package com.odolog.app.vehicle.dto.request.update;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 부분 수정이라 전부 nullable — 보낸 필드만 변경
 * NotBlank 대신 Size(min=1) + Pattern — NotBlank 는 null 도 막아 "안 보냄"이 불가능해짐
 * odometer 제외 — 감소 금지 규칙이 붙어 전용 엔드포인트가 따로 있음
 */
public record VehicleUpdateRequest(

        @Size(min = 1, max = 20)
        @Pattern(regexp = ".*\\S.*", message = "공백만으로는 설정할 수 없습니다")
        String plateNumber,

        @Size(min = 1, max = 50)
        @Pattern(regexp = ".*\\S.*", message = "공백만으로는 설정할 수 없습니다")
        String manufacturer,

        @Size(min = 1, max = 100)
        @Pattern(regexp = ".*\\S.*", message = "공백만으로는 설정할 수 없습니다")
        String modelName,

        @Min(1900)
        @Max(2100)
        Integer modelYear
) {
}
