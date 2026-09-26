package com.odolog.app.account.service.application;

import com.odolog.app.account.dto.response.export.AccountExportResponse;
import com.odolog.app.fuel.domain.entity.FuelRecord;
import com.odolog.app.fuel.repository.jpa.FuelRecordRepository;
import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.entity.ServiceInterval;
import com.odolog.app.maintenance.repository.jpa.MaintenanceRecordRepository;
import com.odolog.app.maintenance.repository.jpa.ServiceIntervalRepository;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.service.application.UserService;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.vehicle.repository.jpa.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 계정 기록 전체 내보내기. 탈퇴의 반대편
 * 읽기만 하므로 서비스 대신 리포지토리 주입. 소유자 id 조회라 남의 데이터 없음
 * 쿼리 4번. 차량별 조회 시 차량 수만큼 증가
 */
@Service
@Transactional(readOnly = true)
public class AccountExportService {

    private final UserService userService;
    private final VehicleRepository vehicleRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final ServiceIntervalRepository serviceIntervalRepository;
    private final FuelRecordRepository fuelRecordRepository;

    public AccountExportService(UserService userService,
                                VehicleRepository vehicleRepository,
                                MaintenanceRecordRepository maintenanceRecordRepository,
                                ServiceIntervalRepository serviceIntervalRepository,
                                FuelRecordRepository fuelRecordRepository) {
        this.userService = userService;
        this.vehicleRepository = vehicleRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.serviceIntervalRepository = serviceIntervalRepository;
        this.fuelRecordRepository = fuelRecordRepository;
    }

    /** exportedAt 은 밖에서 주입. 테스트 고정용 */
    public AccountExportResponse export(Long userId, LocalDateTime exportedAt) {
        User user = userService.findById(userId);
        List<Vehicle> vehicles = vehicleRepository.findAllByOwnerId(userId);

        Map<Long, List<MaintenanceRecord>> maintenanceByVehicle =
                maintenanceRecordRepository.findByVehicle_Owner_IdOrderByServiceDateDescIdDesc(userId).stream()
                        .collect(Collectors.groupingBy(record -> record.getVehicle().getId()));

        Map<Long, List<FuelRecord>> fuelByVehicle =
                fuelRecordRepository.findByVehicle_Owner_IdOrderByOdometerAscIdAsc(userId).stream()
                        .collect(Collectors.groupingBy(record -> record.getVehicle().getId()));

        Map<Long, List<ServiceInterval>> intervalsByVehicle =
                serviceIntervalRepository.findByVehicle_Owner_Id(userId).stream()
                        .collect(Collectors.groupingBy(interval -> interval.getVehicle().getId()));

        return AccountExportResponse.of(exportedAt, user, vehicles,
                maintenanceByVehicle, fuelByVehicle, intervalsByVehicle);
    }
}
