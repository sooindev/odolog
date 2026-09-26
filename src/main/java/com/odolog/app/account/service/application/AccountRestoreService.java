package com.odolog.app.account.service.application;

import com.odolog.app.account.dto.request.restore.AccountRestoreRequest;
import com.odolog.app.account.dto.response.restore.AccountRestoreResponse;
import com.odolog.app.fuel.domain.entity.FuelRecord;
import com.odolog.app.fuel.repository.jpa.FuelRecordRepository;
import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.entity.ServiceInterval;
import com.odolog.app.maintenance.domain.type.ServiceType;
import com.odolog.app.maintenance.repository.jpa.MaintenanceRecordRepository;
import com.odolog.app.maintenance.repository.jpa.ServiceIntervalRepository;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.service.application.UserService;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.vehicle.repository.jpa.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.odolog.app.common.text.InputText;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 내보낸 JSON 복원. 내보내기의 짝
 * 같은 번호판이면 기록만 추가, 차량 정보는 유지
 * 같은 기록은 건너뜀. 두 번 넣어도 두 배가 되지 않게
 * 하나라도 실패하면 전체 롤백
 */
@Service
@Transactional(readOnly = true)
public class AccountRestoreService {

    private final UserService userService;
    private final VehicleRepository vehicleRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final ServiceIntervalRepository serviceIntervalRepository;
    private final FuelRecordRepository fuelRecordRepository;

    public AccountRestoreService(UserService userService,
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

    @Transactional
    public AccountRestoreResponse restore(Long userId, AccountRestoreRequest request) {
        User owner = userService.findById(userId);

        Map<String, Vehicle> byPlate = new LinkedHashMap<>();
        for (Vehicle vehicle : vehicleRepository.findAllByOwnerId(userId)) {
            byPlate.put(plateKey(vehicle.getPlateNumber()), vehicle);
        }

        int addedVehicles = 0;
        int mergedVehicles = 0;
        int addedMaintenance = 0;
        int addedFuel = 0;
        int skipped = 0;
        int addedIntervals = 0;

        for (AccountRestoreRequest.VehicleData data : request.vehicles()) {
            String plateNumber = InputText.strip(data.plateNumber());
            Vehicle vehicle = byPlate.get(plateKey(plateNumber));

            if (vehicle == null) {
                vehicle = vehicleRepository.save(new Vehicle(owner, plateNumber,
                        InputText.strip(data.manufacturer()), InputText.strip(data.modelName()),
                        data.modelYear()));
                byPlate.put(plateKey(plateNumber), vehicle);
                addedVehicles++;
            } else {
                mergedVehicles++;
            }

            // 기존 기록 열쇠 일괄 수집. 건별 조회 시 기록 수만큼 쿼리
            Set<String> existingMaintenance = new HashSet<>();
            for (MaintenanceRecord record : maintenanceRecordRepository
                    .findByVehicleIdOrderByServiceDateDescIdDesc(vehicle.getId())) {
                existingMaintenance.add(maintenanceKey(record));
            }

            Set<String> existingFuel = new HashSet<>();
            for (FuelRecord record : fuelRecordRepository
                    .findAllByVehicleIdOrderByOdometerAscIdAsc(vehicle.getId())) {
                existingFuel.add(fuelKey(record));
            }

            for (AccountRestoreRequest.MaintenanceData record : data.maintenanceRecords()) {
                String key = maintenanceKey(record);
                if (!existingMaintenance.add(key)) {
                    skipped++;
                    continue;
                }

                maintenanceRecordRepository.save(new MaintenanceRecord(vehicle, record.type(),
                        blankToNull(record.description()), record.cost(),
                        record.serviceOdometer(), record.serviceDate()));
                addedMaintenance++;
            }

            for (AccountRestoreRequest.FuelData record : data.fuelRecords()) {
                String key = fuelKey(record);
                if (!existingFuel.add(key)) {
                    skipped++;
                    continue;
                }

                // 기준점은 생성자에 없어 별도 지정
                FuelRecord fuel = new FuelRecord(vehicle, record.fueledAt(), record.odometer(),
                        record.liters(), record.totalCost(), blankToNull(record.memo()));
                fuel.changeResetPoint(record.resetPoint());

                fuelRecordRepository.save(fuel);
                addedFuel++;
            }

            // 이미 설정된 종류는 유지
            Set<ServiceType> settled = new HashSet<>();
            for (ServiceInterval interval : serviceIntervalRepository.findByVehicleId(vehicle.getId())) {
                settled.add(interval.getType());
            }

            for (AccountRestoreRequest.IntervalData interval : data.serviceIntervals()) {
                if (!settled.add(interval.type())
                        || (interval.intervalKm() == null && interval.intervalMonths() == null)) {
                    continue;
                }

                serviceIntervalRepository.save(new ServiceInterval(vehicle, interval.type(),
                        interval.intervalKm(), interval.intervalMonths()));
                addedIntervals++;
            }

            // 기록 추가 후 한 번만. 파일 값과 기존 값 중 큰 쪽
            vehicle.liftOdometerTo(data.odometer());
        }

        return new AccountRestoreResponse(addedVehicles, addedMaintenance, addedFuel,
                mergedVehicles, skipped, addedIntervals);
    }

    // 중복 판정 열쇠. JSON 에 id 가 없어 내용으로 판정
    // 정비: 종류·날짜·주행거리 / 주유: 날짜·주행거리
    private String maintenanceKey(MaintenanceRecord record) {
        return record.getType() + "|" + record.getServiceDate() + "|" + record.getServiceOdometer();
    }

    private String maintenanceKey(AccountRestoreRequest.MaintenanceData record) {
        return record.type() + "|" + record.serviceDate() + "|" + record.serviceOdometer();
    }

    private String fuelKey(FuelRecord record) {
        return record.getFueledAt() + "|" + record.getOdometer();
    }

    private String fuelKey(AccountRestoreRequest.FuelData record) {
        return record.fueledAt() + "|" + record.odometer();
    }

    /** 같은 차 판정 열쇠. DB 유니크와 같은 기준(앞뒤 공백·대소문자 무시) */
    private String plateKey(String plateNumber) {
        return plateNumber.strip().toLowerCase(Locale.ROOT);
    }

    /** 빈 문자열은 null */
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
