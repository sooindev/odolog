package com.odolog.app.account.dto.response.export;

import com.odolog.app.fuel.domain.entity.FuelRecord;
import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.vehicle.domain.entity.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 계정의 기록 전부. 백업용이라 화면 응답과 달리 계산값을 넣지 않는다
 * 연비·단가는 읽을 때 만들어지는 값이라 여기 담으면 복원 시 원본과 어긋날 수 있다
 *
 * 차량 밑에 이력을 중첩한다 — 평평하게 내보내면 어느 기록이 어느 차의 것인지
 * id 를 따라가야 알 수 있는데, 그 id 는 우리 DB 안에서만 뜻이 있다
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
            // 비밀번호 해시는 담지 않는다. 백업에 넣을 이유가 없고 새어 나갈 경로만 늘린다
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
            List<FuelData> fuelRecords
    ) {
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
            Map<Long, List<FuelRecord>> fuelByVehicle) {

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
                                .toList()))
                .toList();

        return new AccountExportResponse(exportedAt, UserData.from(user), vehicleData);
    }
}
