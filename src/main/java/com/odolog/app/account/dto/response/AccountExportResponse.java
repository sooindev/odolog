package com.odolog.app.account.dto.response;

import com.odolog.app.fuel.domain.FuelRecord;
import com.odolog.app.maintenance.domain.MaintenanceRecord;
import com.odolog.app.maintenance.domain.ServiceInterval;
import com.odolog.app.user.domain.User;
import com.odolog.app.vehicle.domain.Vehicle;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * 계정 기록 전체. 백업용이라 계산값(연비·단가) 제외
 * 기록은 차량 밑에 중첩. 우리 DB 의 id 없이도 소속 파악 가능
 * 시각은 전부 사용자 시간대 + 오프셋. 오프셋이 없으면 같은 파일 안의 시각끼리도 비교할 수 없음
 */
public record AccountExportResponse(
        OffsetDateTime exportedAt,
        UserData user,
        List<VehicleData> vehicles
) {

    public record UserData(
            String email,
            String nickname,
            OffsetDateTime createdAt
    ) {
        static UserData from(User user, ZoneId zone) {
            // 비밀번호 해시 제외
            return new UserData(user.getEmail(), user.getNickname(), at(user.getCreatedAt(), zone));
        }
    }

    public record VehicleData(
            String plateNumber,
            String manufacturer,
            String modelName,
            Integer modelYear,
            int odometer,
            OffsetDateTime createdAt,
            List<MaintenanceData> maintenanceRecords,
            List<FuelData> fuelRecords,
            /** 차량별 권장 주기. 빠지면 복원한 차가 기본값으로 돌아감 */
            List<IntervalData> serviceIntervals
    ) {
    }

    public record IntervalData(
            String type,
            Integer intervalKm,
            Integer intervalMonths
    ) {
        static IntervalData from(ServiceInterval interval) {
            return new IntervalData(interval.getType().name(),
                    interval.getIntervalKm(), interval.getIntervalMonths());
        }
    }

    public record MaintenanceData(
            String type,
            String description,
            /** 안 적었으면 null */
            Integer cost,
            /** ISO 4217. 금액은 이 통화의 최소 단위 */
            String currency,
            Integer serviceOdometer,
            LocalDate serviceDate,
            OffsetDateTime createdAt
    ) {
        static MaintenanceData from(MaintenanceRecord record, ZoneId zone) {
            return new MaintenanceData(
                    record.getType().name(),
                    record.getDescription(),
                    record.getCost(),
                    record.getCurrency(),
                    record.getServiceOdometer(),
                    record.getServiceDate(),
                    at(record.getCreatedAt(), zone));
        }
    }

    public record FuelData(
            LocalDate fueledAt,
            int odometer,
            BigDecimal liters,
            Integer totalCost,
            String currency,
            String memo,
            boolean resetPoint,
            boolean fullTank,
            OffsetDateTime createdAt
    ) {
        static FuelData from(FuelRecord record, ZoneId zone) {
            return new FuelData(
                    record.getFueledAt(),
                    record.getOdometer(),
                    record.getLiters(),
                    record.getTotalCost(),
                    record.getCurrency(),
                    record.getMemo(),
                    record.isResetPoint(),
                    record.isFullTank(),
                    at(record.getCreatedAt(), zone));
        }
    }

    /** 기록 시각(JVM 시간대로 저장된 로컬 시각) → 사용자 시간대 + 오프셋 */
    private static OffsetDateTime at(LocalDateTime stored, ZoneId zone) {
        return stored == null ? null
                : stored.atZone(ZoneId.systemDefault()).withZoneSameInstant(zone).toOffsetDateTime();
    }

    public static AccountExportResponse of(
            Instant exportedAt,
            User user,
            List<Vehicle> vehicles,
            Map<Long, List<MaintenanceRecord>> maintenanceByVehicle,
            Map<Long, List<FuelRecord>> fuelByVehicle,
            Map<Long, List<ServiceInterval>> intervalsByVehicle) {

        ZoneId zone = ZoneId.of(user.getTimeZone());
        List<VehicleData> vehicleData = vehicles.stream()
                .map(vehicle -> new VehicleData(
                        vehicle.getPlateNumber(),
                        vehicle.getManufacturer(),
                        vehicle.getModelName(),
                        vehicle.getModelYear(),
                        vehicle.getOdometer(),
                        at(vehicle.getCreatedAt(), zone),
                        maintenanceByVehicle.getOrDefault(vehicle.getId(), List.of()).stream()
                                .map(record -> MaintenanceData.from(record, zone))
                                .toList(),
                        fuelByVehicle.getOrDefault(vehicle.getId(), List.of()).stream()
                                .map(record -> FuelData.from(record, zone))
                                .toList(),
                        intervalsByVehicle.getOrDefault(vehicle.getId(), List.of()).stream()
                                .map(IntervalData::from)
                                .toList()))
                .toList();

        return new AccountExportResponse(exportedAt.atZone(zone).toOffsetDateTime(),
                UserData.from(user, zone), vehicleData);
    }
}
