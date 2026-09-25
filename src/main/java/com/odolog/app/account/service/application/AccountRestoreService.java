package com.odolog.app.account.service.application;

import com.odolog.app.account.dto.request.restore.AccountRestoreRequest;
import com.odolog.app.account.dto.response.restore.AccountRestoreResponse;
import com.odolog.app.fuel.domain.entity.FuelRecord;
import com.odolog.app.fuel.repository.jpa.FuelRecordRepository;
import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.repository.jpa.MaintenanceRecordRepository;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.service.application.UserService;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.vehicle.repository.jpa.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 내보낸 JSON 을 되돌려 넣는다. 내보내기의 짝이다
 *
 * 왜 필요했나: 화면이 "탈퇴하면 복구되지 않습니다. 지우기 전에 받아 두세요" 라고 말하면서
 * 받은 파일로 할 수 있는 일이 없었다. **복원할 수 없으면 백업이 아니라 기념품이다**
 *
 * 규칙 셋:
 *   1. 같은 번호판이 있으면 그 차량에 기록만 붙인다. 차량 정보는 건드리지 않는다 —
 *      파일이 옛날 것일 수 있는데 지금 값을 덮어쓸 이유가 없다
 *   2. 같은 기록은 건너뛴다. 같은 파일을 두 번 넣어도 두 배가 되지 않아야 한다 —
 *      "실수로 두 번 눌렀더니 기록이 두 배" 가 이 기능에서 가장 나쁜 결과다
 *   3. 하나라도 검증에 걸리면 전부 안 들어간다(@Transactional). 부분 성공은
 *      "무엇이 들어갔는지 모르는" 상태를 남긴다
 *
 * account 패키지인 이유는 내보내기와 같다 — 여러 기능을 동시에 알아도 되는 조율 층이다.
 * 다만 내보내기와 달리 **쓰기** 라서, 차량 등록 규칙(번호판 중복)을 우회하지 않도록
 * 여기서 직접 확인한다
 */
@Service
@Transactional(readOnly = true)
public class AccountRestoreService {

    private final UserService userService;
    private final VehicleRepository vehicleRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final FuelRecordRepository fuelRecordRepository;

    public AccountRestoreService(UserService userService,
                                 VehicleRepository vehicleRepository,
                                 MaintenanceRecordRepository maintenanceRecordRepository,
                                 FuelRecordRepository fuelRecordRepository) {
        this.userService = userService;
        this.vehicleRepository = vehicleRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.fuelRecordRepository = fuelRecordRepository;
    }

    @Transactional
    public AccountRestoreResponse restore(Long userId, AccountRestoreRequest request) {
        User owner = userService.findById(userId);

        Map<String, Vehicle> byPlate = new LinkedHashMap<>();
        for (Vehicle vehicle : vehicleRepository.findAllByOwnerId(userId)) {
            byPlate.put(vehicle.getPlateNumber(), vehicle);
        }

        int addedVehicles = 0;
        int mergedVehicles = 0;
        int addedMaintenance = 0;
        int addedFuel = 0;
        int skipped = 0;

        for (AccountRestoreRequest.VehicleData data : request.vehicles()) {
            Vehicle vehicle = byPlate.get(data.plateNumber());

            if (vehicle == null) {
                vehicle = vehicleRepository.save(new Vehicle(owner, data.plateNumber(),
                        data.manufacturer(), data.modelName(), data.modelYear()));
                byPlate.put(data.plateNumber(), vehicle);
                addedVehicles++;
            } else {
                mergedVehicles++;
            }

            // 이미 있는 기록의 열쇠를 먼저 모은다. 한 건씩 조회하면 기록 수만큼 쿼리가 난다
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

                // 기준점은 생성자에 없어 따로 찍는다. save() 의 반환이 아니라 넘긴 객체에 —
                // 같은 인스턴스이고, 반환에 기대면 리포지토리 구현에 매달리는 코드가 된다
                FuelRecord fuel = new FuelRecord(vehicle, record.fueledAt(), record.odometer(),
                        record.liters(), record.totalCost(), blankToNull(record.memo()));
                fuel.changeResetPoint(record.resetPoint());

                fuelRecordRepository.save(fuel);
                addedFuel++;
            }

            // 기록을 넣은 뒤 한 번만. 파일의 값과 기록들 중 큰 쪽으로 맞춰진다
            vehicle.liftOdometerTo(data.odometer());
        }

        return new AccountRestoreResponse(addedVehicles, addedMaintenance, addedFuel,
                mergedVehicles, skipped);
    }

    /*
     * 중복 판정 열쇠
     *
     * id 로는 판정할 수 없다 — 내보낸 JSON 에 id 가 없기 때문이다(우리 DB 안에서만 뜻이 있는
     * 값이라 일부러 뺐다). 대신 사람이 보기에 같은 기록이면 같다고 본다.
     *
     * 정비는 종류·날짜·주행거리, 주유는 날짜·주행거리다. 같은 날 같은 계기판에서
     * 같은 종류의 정비를 두 번 받는 일은 없다
     */
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

    /** 빈 문자열은 "없음" 으로. 서비스들이 쓰는 규칙과 같다 */
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
