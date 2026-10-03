package com.odolog.app.account.service;

import com.odolog.app.common.exception.ErrorCode;
import com.odolog.app.common.exception.type.InvalidRequestException;
import com.odolog.app.common.CurrencyCode;
import com.odolog.app.account.dto.request.AccountRestoreRequest;
import com.odolog.app.account.dto.response.AccountRestoreResponse;
import com.odolog.app.fuel.domain.FuelRecord;
import com.odolog.app.fuel.FuelRecordRepository;
import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.entity.ServiceInterval;
import com.odolog.app.maintenance.domain.ServiceType;
import com.odolog.app.maintenance.repository.MaintenanceRecordRepository;
import com.odolog.app.maintenance.repository.ServiceIntervalRepository;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.service.application.UserService;
import com.odolog.app.user.service.UserToday;
import com.odolog.app.vehicle.Vehicle;
import com.odolog.app.vehicle.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.odolog.app.common.InputText;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
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

    /** 파일 하나의 기록 총량 상한. 차량별 상한만으로는 50대 × 1만 건이 한 트랜잭션 */
    private static final int MAX_TOTAL_RECORDS = 20_000;

    private final UserService userService;
    private final VehicleRepository vehicleRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final ServiceIntervalRepository serviceIntervalRepository;
    private final FuelRecordRepository fuelRecordRepository;
    private final UserToday userToday;

    public AccountRestoreService(UserService userService,
                                 VehicleRepository vehicleRepository,
                                 MaintenanceRecordRepository maintenanceRecordRepository,
                                 ServiceIntervalRepository serviceIntervalRepository,
                                 FuelRecordRepository fuelRecordRepository,
                                 UserToday userToday) {
        this.userService = userService;
        this.vehicleRepository = vehicleRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.serviceIntervalRepository = serviceIntervalRepository;
        this.fuelRecordRepository = fuelRecordRepository;
        this.userToday = userToday;
    }

    @Transactional
    public AccountRestoreResponse restore(Long userId, AccountRestoreRequest request) {
        // 가장 먼저 잠금. 같은 파일을 동시에 두 번 넣어도 두 번째는 첫 번째가 넣은 기록을 보고 건너뜀
        User owner = userService.findByIdForUpdate(userId);
        rejectOversized(request);
        rejectFutureDates(userToday.of(owner), request);

        int addedVehicles = 0;
        int mergedVehicles = 0;
        int addedMaintenance = 0;
        int addedFuel = 0;
        int skipped = 0;
        int addedIntervals = 0;

        for (AccountRestoreRequest.VehicleData data : request.vehicles()) {
            String plateNumber = InputText.required(data.plateNumber(), "plateNumber");
            // 같은 차인지는 DB 정렬 규칙으로. 파일 안에서 앞서 만든 차도 찾음(IDENTITY 라 저장 즉시 INSERT)
            Vehicle vehicle = vehicleRepository.findByOwnerIdAndPlateNumber(userId, plateNumber).orElse(null);
            boolean created = vehicle == null;

            if (created) {
                vehicle = vehicleRepository.save(new Vehicle(owner, plateNumber,
                        InputText.required(data.manufacturer(), "manufacturer"),
                        InputText.required(data.modelName(), "modelName"),
                        data.modelYear()));
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
                        blankToNull(record.description()), record.cost(), currencyOf(record.currency()),
                        record.serviceOdometer(), record.serviceDate()));
                addedMaintenance++;
            }

            for (AccountRestoreRequest.FuelData record : data.fuelRecords()) {
                String key = fuelKey(record);
                if (!existingFuel.add(key)) {
                    skipped++;
                    continue;
                }

                // 기준점은 생성자에 없어 별도 지정. 새 차량만, 있던 차량은 지금의 연비 기준 유지
                FuelRecord fuel = new FuelRecord(vehicle, record.fueledAt(), record.odometer(),
                        record.liters(), record.totalCost(), currencyOf(record.currency()),
                        blankToNull(record.memo()));
                fuel.changeResetPoint(created && record.resetPoint());

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

            // 기록 추가 후 한 번만. 파일 값·기록의 주행거리·기존 값 중 가장 큰 쪽(등록과 같은 규칙)
            vehicle.liftOdometerTo(Math.max(data.odometer(), highestOdometer(data)));
        }

        return new AccountRestoreResponse(addedVehicles, addedMaintenance, addedFuel,
                mergedVehicles, skipped, addedIntervals);
    }

    private void rejectOversized(AccountRestoreRequest request) {
        int total = 0;
        for (AccountRestoreRequest.VehicleData data : request.vehicles()) {
            total += data.maintenanceRecords().size() + data.fuelRecords().size();
        }
        if (total > MAX_TOTAL_RECORDS) {
            throw new InvalidRequestException(ErrorCode.BAD_REQUEST,
                    "한 파일에 기록 " + MAX_TOTAL_RECORDS + "건까지 가져올 수 있습니다: " + total);
        }
    }

    /** 저장 전에 전부 검사. 하나라도 미래면 아무것도 안 들어감. 오늘은 한 번만 계산 */
    private void rejectFutureDates(LocalDate today, AccountRestoreRequest request) {
        for (AccountRestoreRequest.VehicleData data : request.vehicles()) {
            for (AccountRestoreRequest.MaintenanceData record : data.maintenanceRecords()) {
                rejectFuture(today, record.serviceDate(), "maintenanceRecords.serviceDate");
            }
            for (AccountRestoreRequest.FuelData record : data.fuelRecords()) {
                rejectFuture(today, record.fueledAt(), "fuelRecords.fueledAt");
            }
        }
    }

    private void rejectFuture(LocalDate today, LocalDate date, String field) {
        if (date != null && date.isAfter(today)) {
            throw new InvalidRequestException(ErrorCode.FUTURE_DATE,
                    field + ": 오늘 이후 날짜는 입력할 수 없습니다.", field);
        }
    }

    /** 파일의 통화. 칸 없는 옛 파일은 원화, 지금 사용자 설정이 아님 */
    private String currencyOf(String code) {
        if (code == null) {
            return CurrencyCode.LEGACY;
        }
        if (!CurrencyCode.isKnown(code)) {
            throw new InvalidRequestException(ErrorCode.UNSUPPORTED_CURRENCY,
                    "currency: 지원하지 않는 통화입니다: " + code, "currency");
        }
        return code;
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

    /** 파일 기록 중 가장 큰 주행거리. 없으면 0 */
    private int highestOdometer(AccountRestoreRequest.VehicleData data) {
        int highest = 0;
        for (AccountRestoreRequest.MaintenanceData record : data.maintenanceRecords()) {
            if (record.serviceOdometer() != null) {
                highest = Math.max(highest, record.serviceOdometer());
            }
        }
        for (AccountRestoreRequest.FuelData record : data.fuelRecords()) {
            highest = Math.max(highest, record.odometer());
        }
        return highest;
    }

    /** 빈 문자열은 null */
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
