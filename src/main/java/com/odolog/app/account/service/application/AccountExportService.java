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
 * 계정의 기록 전부를 한 덩어리로 내보낸다. 탈퇴의 거울상이다 —
 * 한쪽은 전부 지우고 한쪽은 전부 가져간다
 *
 * 리포지토리를 직접 받는다. 원본을 그대로 읽기만 하므로 각 기능의 비즈니스 규칙이 필요 없고,
 * 소유자 id 로 조회하므로 남의 데이터가 섞이지 않는다 (summary 와 같은 이유).
 * 같은 패키지의 AccountWithdrawalService 가 서비스를 받는 것과 다른데, 그쪽은 삭제 순서를
 * 조율해야 하기 때문이다
 *
 * 쿼리는 4번이다. 차량마다 따로 조회하면 차량 수만큼 늘어난다
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

    /** exportedAt 을 밖에서 받는다 — 안에서 now() 를 부르면 테스트에서 고정할 수 없다 */
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
