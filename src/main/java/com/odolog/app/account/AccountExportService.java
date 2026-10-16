package com.odolog.app.account;

import com.odolog.app.account.dto.response.AccountExportResponse;
import com.odolog.app.fuel.FuelRecordRepository;
import com.odolog.app.fuel.domain.FuelRecord;
import com.odolog.app.maintenance.MaintenanceRecordRepository;
import com.odolog.app.maintenance.ServiceIntervalRepository;
import com.odolog.app.maintenance.domain.MaintenanceRecord;
import com.odolog.app.maintenance.domain.ServiceInterval;
import com.odolog.app.user.UserService;
import com.odolog.app.user.domain.User;
import com.odolog.app.vehicle.VehicleRepository;
import com.odolog.app.vehicle.domain.Vehicle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 계정 기록 전체 내보내기. 탈퇴의 반대편
 * 읽기만 하므로 서비스 대신 리포지토리 주입. 소유자 id 조회라 남의 데이터 없음
 * 쿼리 5번(사용자·차량·정비·주유·주기). 차량별로 조회하면 차량 수만큼 증가
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

    /** exportedAt 은 밖에서 주입. 테스트 고정용. 파일의 시각은 전부 사용자 시간대 + 오프셋 */
    public AccountExportResponse export(Long userId, Instant exportedAt) {
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
