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
 * 내보낸 JSON 의 복원 요청. 패키지명 restore 는 import 가 예약어라서
 * 사용자 정보는 받지 않음. 내 계정에 기록만 추가
 * 검증은 등록 DTO 와 동일. 파일 경로로 느슨한 데이터 유입 방지
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

            /** 차량별 권장 주기. 옛 파일에는 없을 수 있어 null 허용 */
            @Size(max = 30) @Valid List<IntervalData> serviceIntervals
    ) {

        /** 옛 파일 대비 빈 목록 반환 */
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
