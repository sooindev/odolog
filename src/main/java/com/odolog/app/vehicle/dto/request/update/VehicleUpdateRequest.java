package com.odolog.app.vehicle.dto.request.update;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 부분 수정이라 전부 nullable — 보낸 필드만 바뀐다.
 *
 * 등록 DTO 의 @NotBlank 를 그대로 가져오면 안 된다. @NotBlank 는 null 도 막아서
 * "안 보냄" 자체가 불가능해지고, 그러면 제조사 하나 고치려 해도 네 필드를 다 보내야 한다.
 * @Size(min = 1) 과 @Pattern 은 둘 다 null 을 통과시키므로 부분 수정과 짝이 맞는다.
 *
 * odometer 는 여기 없다. 감소 금지라는 규칙이 붙어 있어 전용 엔드포인트가 따로 있다.
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
