package com.odolog.app.account.dto.request.restore;

import com.odolog.app.common.validation.limit.InputLimits;
import com.odolog.app.maintenance.domain.type.ServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 내보낸 JSON 을 그대로 되돌려받는다
 *
 * 패키지가 restore 인 이유는 import 가 자바 예약어라서다
 *
 * 내보내기(AccountExportResponse)와 모양이 같지만 **사용자 정보는 받지 않는다** —
 * 가져오기는 "내 계정에 기록을 더하는 것" 이지 계정을 바꾸는 것이 아니다.
 * 그래서 남의 파일을 넣어도 내 계정에 붙고, 이메일·닉네임은 아무 영향이 없다
 *
 * 제약은 등록 DTO 와 같은 것을 건다. 파일로 들어온다고 검증을 느슨하게 하면
 * 화면으로는 못 만드는 데이터가 파일로는 들어간다
 */
public record AccountRestoreRequest(

        @NotNull
        @Size(max = 50, message = "한 번에 차량 50대까지 가져올 수 있습니다")
        @Valid
        List<VehicleData> vehicles
) {

    public record VehicleData(
            @NotBlank @Size(max = 20) String plateNumber,
            @NotBlank @Size(max = 50) String manufacturer,
            @NotBlank @Size(max = 100) String modelName,
            @Min(1900) @Max(2100) Integer modelYear,
            @PositiveOrZero @Max(InputLimits.MAX_ODOMETER) int odometer,

            @NotNull @Size(max = 5000) @Valid List<MaintenanceData> maintenanceRecords,
            @NotNull @Size(max = 5000) @Valid List<FuelData> fuelRecords,

            /**
             * 차량별 권장 주기. 옛 파일에는 없을 수 있어 null 을 허용한다
             * 빠져 있으면 기본값으로 복원되는데, 그건 "설정이 없던 상태" 와 같아 문제가 없다
             */
            @Size(max = 30) @Valid List<IntervalData> serviceIntervals
    ) {

        /** 옛 파일 대비. null 을 그대로 돌리면 부르는 쪽이 매번 검사해야 한다 */
        public List<IntervalData> serviceIntervals() {
            return serviceIntervals == null ? List.of() : serviceIntervals;
        }
    }

    public record IntervalData(
            @NotNull ServiceType type,
            @Positive @Max(500_000) Integer intervalKm,
            @Positive @Max(120) Integer intervalMonths
    ) {
    }

    public record MaintenanceData(
            @NotNull ServiceType type,
            @Size(max = 255) String description,
            @NotNull @PositiveOrZero @Max(InputLimits.MAX_AMOUNT) Integer cost,
            @NotNull @PositiveOrZero @Max(InputLimits.MAX_ODOMETER) Integer serviceOdometer,
            @NotNull @PastOrPresent LocalDate serviceDate
    ) {
    }

    public record FuelData(
            @NotNull @PastOrPresent LocalDate fueledAt,
            @NotNull @PositiveOrZero @Max(InputLimits.MAX_ODOMETER) Integer odometer,
            @Positive @Digits(integer = 4, fraction = 2) @DecimalMax("9999.99") BigDecimal liters,
            @PositiveOrZero @Max(InputLimits.MAX_AMOUNT) Integer totalCost,
            @Size(max = 255) String memo,
            boolean resetPoint
    ) {
    }
}
