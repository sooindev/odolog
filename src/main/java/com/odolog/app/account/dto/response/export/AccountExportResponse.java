package com.odolog.app.account.dto.response.export;

import com.odolog.app.fuel.domain.entity.FuelRecord;
import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.entity.ServiceInterval;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.vehicle.domain.entity.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 계정 기록 전체. 백업용이라 계산값(연비·단가) 제외
 * 기록은 차량 밑에 중첩. 우리 DB 의 id 없이도 소속 파악 가능
 */
public record AccountExportResponse(
        LocalDateTime exportedAt,
        UserData user,
        List<VehicleData> vehicles
) {

    public record UserData(
            String email,
            String nickname,
            String phone,
            LocalDateTime createdAt
    ) {
        static UserData from(User user) {
            // 비밀번호 해시 제외
            return new UserData(user.getEmail(), user.getNickname(), user.getPhone(), user.getCreatedAt());
        }
    }

    public record VehicleData(
            String plateNumber,
            String manufacturer,
            String modelName,
            Integer modelYear,
            int odometer,
            LocalDateTime createdAt,
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
            int cost,
            int serviceOdometer,
            LocalDate serviceDate,
            LocalDateTime createdAt
    ) {
        static MaintenanceData from(MaintenanceRecord record) {
            return new MaintenanceData(
                    record.getType().name(),
                    record.getDescription(),
                    record.getCost(),
                    record.getServiceOdometer(),
                    record.getServiceDate(),
                    record.getCreatedAt());
        }
    }

    public record FuelData(
            LocalDate fueledAt,
            int odometer,
            BigDecimal liters,
            Integer totalCost,
            String memo,
            boolean resetPoint,
            LocalDateTime createdAt
    ) {
        static FuelData from(FuelRecord record) {
            return new FuelData(
                    record.getFueledAt(),
                    record.getOdometer(),
                    record.getLiters(),
                    record.getTotalCost(),
                    record.getMemo(),
                    record.isResetPoint(),
                    record.getCreatedAt());
        }
    }

    public static AccountExportResponse of(
            LocalDateTime exportedAt,
            User user,
            List<Vehicle> vehicles,
            Map<Long, List<MaintenanceRecord>> maintenanceByVehicle,
            Map<Long, List<FuelRecord>> fuelByVehicle,
            Map<Long, List<ServiceInterval>> intervalsByVehicle) {

        List<VehicleData> vehicleData = vehicles.stream()
                .map(vehicle -> new VehicleData(
                        vehicle.getPlateNumber(),
                        vehicle.getManufacturer(),
                        vehicle.getModelName(),
                        vehicle.getModelYear(),
                        vehicle.getOdometer(),
                        vehicle.getCreatedAt(),
                        maintenanceByVehicle.getOrDefault(vehicle.getId(), List.of()).stream()
                                .map(MaintenanceData::from)
                                .toList(),
                        fuelByVehicle.getOrDefault(vehicle.getId(), List.of()).stream()
                                .map(FuelData::from)
                                .toList(),
                        intervalsByVehicle.getOrDefault(vehicle.getId(), List.of()).stream()
                                .map(IntervalData::from)
                                .toList()))
                .toList();

        return new AccountExportResponse(exportedAt, UserData.from(user), vehicleData);
    }
}
