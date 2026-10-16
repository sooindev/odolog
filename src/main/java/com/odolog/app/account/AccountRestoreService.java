package com.odolog.app.account;

import com.odolog.app.account.dto.request.AccountRestoreRequest;
import com.odolog.app.account.dto.response.AccountRestoreResponse;
import com.odolog.app.common.exception.ErrorCode;
import com.odolog.app.common.exception.type.InvalidRequestException;
import com.odolog.app.common.validation.CurrencyCode;
import com.odolog.app.common.validation.InputText;
import com.odolog.app.fuel.FuelRecordRepository;
import com.odolog.app.fuel.domain.FuelRecord;
import com.odolog.app.maintenance.MaintenanceRecordRepository;
import com.odolog.app.maintenance.ServiceIntervalRepository;
import com.odolog.app.maintenance.domain.MaintenanceRecord;
import com.odolog.app.maintenance.domain.ServiceInterval;
import com.odolog.app.maintenance.domain.ServiceType;
import com.odolog.app.user.UserService;
import com.odolog.app.user.UserToday;
import com.odolog.app.user.domain.User;
import com.odolog.app.vehicle.VehicleRepository;
import com.odolog.app.vehicle.domain.Vehicle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    /** 한 파일의 차량 수 상한 */
    static final int MAX_VEHICLES = 50;
    /** 차량 하나의 정비·주유 각각의 상한 */
    static final int MAX_RECORDS_PER_KIND = 5_000;
    /** 파일 하나의 기록 총량 상한. 차량별 상한만으로는 50대 × 1만 건이 한 트랜잭션 */
    static final int MAX_TOTAL_RECORDS = 20_000;

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
        rejectOutOfRangeDates(userToday.of(owner), request);

        Tally tally = new Tally();
        for (AccountRestoreRequest.VehicleData data : request.vehicles()) {
            restoreVehicle(userId, owner, data, tally);
        }

        return new AccountRestoreResponse(tally.addedVehicles, tally.addedMaintenance, tally.addedFuel,
                tally.mergedVehicles, tally.skipped, tally.addedIntervals);
    }

    /** 차량 하나. 같은 번호판이면 찾은 차에, 없으면 새로 만들어 기록·주기 추가 */
    private void restoreVehicle(Long userId, User owner, AccountRestoreRequest.VehicleData data, Tally tally) {
        String plateNumber = InputText.plateNumber(data.plateNumber(), "plateNumber");
        // 같은 차인지는 DB 정렬 규칙으로. 파일 안에서 앞서 만든 차도 찾음(IDENTITY 라 저장 즉시 INSERT)
        Vehicle vehicle = vehicleRepository.findLockedByOwnerIdAndPlateNumber(userId, plateNumber).orElse(null);
        boolean created = vehicle == null;

        if (created) {
            vehicle = vehicleRepository.save(new Vehicle(owner, plateNumber,
                    InputText.required(data.manufacturer(), "manufacturer"),
                    InputText.required(data.modelName(), "modelName"),
                    data.modelYear()));
            tally.addedVehicles++;
        }

        // 이번에 실제로 넣은 기록 중 가장 큰 주행거리. 있던 차량은 이것만 따라 올림
        int addedBefore = tally.addedMaintenance + tally.addedFuel + tally.addedIntervals;
        int addedHighest = Math.max(
                restoreMaintenance(vehicle, data.maintenanceRecords(), tally),
                restoreFuel(vehicle, created, data.fuelRecords(), tally));
        restoreIntervals(vehicle, data.serviceIntervals(), tally);

        // 있던 차량은 실제로 무언가 붙였을 때만 셈. 전부 건너뛴 차까지 세면 "기록만 붙였습니다" 가 거짓
        if (!created && tally.addedMaintenance + tally.addedFuel + tally.addedIntervals > addedBefore) {
            tally.mergedVehicles++;
        }

        // 기록 추가 후 한 번만. 등록과 같은 규칙(올리기만)
        // 있던 차량은 파일의 차량 값·건너뛴 기록을 보지 않음. 옛 파일이 손으로 낮춘 주행거리를 되살림
        vehicle.liftOdometerTo(created ? Math.max(data.odometer(), addedHighest) : addedHighest);
    }

    /** 정비 기록 추가. 넣은 기록 중 가장 큰 주행거리 반환(없으면 0) */
    private int restoreMaintenance(Vehicle vehicle, List<AccountRestoreRequest.MaintenanceData> records,
                                   Tally tally) {
        // 가져오기 전에 있던 기록의 열쇠. 건별 조회 시 기록 수만큼 쿼리
        // 파일 안의 기록끼리는 비교하지 않음. 같은 날·같은 주행거리의 서로 다른 기록이 사라짐
        Set<String> existing = new HashSet<>();
        for (MaintenanceRecord record : maintenanceRecordRepository
                .findByVehicleIdOrderByServiceDateDescIdDesc(vehicle.getId())) {
            existing.add(RestoreKeys.of(record));
        }

        int highest = 0;
        for (AccountRestoreRequest.MaintenanceData record : inInsertOrder(records)) {
            String currency = currencyOf(record.currency());
            if (existing.contains(RestoreKeys.of(record, currency))) {
                tally.skipped++;
                continue;
            }

            maintenanceRecordRepository.save(new MaintenanceRecord(vehicle, record.type(),
                    InputText.optional(record.description()), record.cost(), currency,
                    record.serviceOdometer(), record.serviceDate()));
            tally.addedMaintenance++;
            if (record.serviceOdometer() != null) {
                highest = Math.max(highest, record.serviceOdometer());
            }
        }
        return highest;
    }

    /** 주유 기록 추가. 넣은 기록 중 가장 큰 주행거리 반환(없으면 0) */
    private int restoreFuel(Vehicle vehicle, boolean created, List<AccountRestoreRequest.FuelData> records,
                            Tally tally) {
        Set<String> existing = new HashSet<>();
        for (FuelRecord record : fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(vehicle.getId())) {
            existing.add(RestoreKeys.of(record));
        }

        // 기준점은 차량당 하나. 옛 파일에 여럿이면 주행거리가 가장 큰(최근) 것만. 요약도 최근 것을 씀
        AccountRestoreRequest.FuelData latestResetPoint = null;
        for (AccountRestoreRequest.FuelData record : records) {
            if (record.resetPoint()
                    && (latestResetPoint == null || record.odometer() >= latestResetPoint.odometer())) {
                latestResetPoint = record;
            }
        }

        int highest = 0;
        for (AccountRestoreRequest.FuelData record : records) {
            String currency = currencyOf(record.currency());
            if (existing.contains(RestoreKeys.of(record, currency))) {
                tally.skipped++;
                continue;
            }

            // 기준점은 생성자에 없어 별도 지정. 새 차량만, 있던 차량은 지금의 연비 기준 유지
            FuelRecord fuel = new FuelRecord(vehicle, record.fueledAt(), record.odometer(),
                    record.liters(), record.totalCost(), currency, InputText.optional(record.memo()));
            fuel.changeResetPoint(created && record == latestResetPoint);
            fuel.changeFullTank(!Boolean.FALSE.equals(record.fullTank()));

            fuelRecordRepository.save(fuel);
            tally.addedFuel++;
            highest = Math.max(highest, record.odometer());
        }
        return highest;
    }

    /** 차량별 주기 추가. 이미 설정된 종류는 유지 */
    private void restoreIntervals(Vehicle vehicle, List<AccountRestoreRequest.IntervalData> intervals,
                                  Tally tally) {
        Set<ServiceType> settled = new HashSet<>();
        for (ServiceInterval interval : serviceIntervalRepository.findByVehicleId(vehicle.getId())) {
            settled.add(interval.getType());
        }

        for (AccountRestoreRequest.IntervalData interval : intervals) {
            // 빈 항목 먼저 거름. 순서가 반대면 빈 항목이 같은 종류의 뒤 항목을 막음
            if (interval.intervalKm() == null && interval.intervalMonths() == null) {
                continue;
            }
            if (!settled.add(interval.type())) {
                continue;
            }

            serviceIntervalRepository.save(new ServiceInterval(vehicle, interval.type(),
                    interval.intervalKm(), interval.intervalMonths()));
            tally.addedIntervals++;
        }
    }

    /** 상한 셋을 한 코드로. 화면이 어떤 상한인지 문구로 밝힘 */
    private void rejectOversized(AccountRestoreRequest request) {
        if (request.vehicles().size() > MAX_VEHICLES) {
            throw tooLarge("차량 " + MAX_VEHICLES + "대까지 가져올 수 있습니다: " + request.vehicles().size());
        }
        int total = 0;
        for (AccountRestoreRequest.VehicleData data : request.vehicles()) {
            if (data.maintenanceRecords().size() > MAX_RECORDS_PER_KIND
                    || data.fuelRecords().size() > MAX_RECORDS_PER_KIND) {
                throw tooLarge("차량 하나에 정비·주유 각 " + MAX_RECORDS_PER_KIND + "건까지 가져올 수 있습니다");
            }
            total += data.maintenanceRecords().size() + data.fuelRecords().size();
        }
        if (total > MAX_TOTAL_RECORDS) {
            throw tooLarge("한 파일에 기록 " + MAX_TOTAL_RECORDS + "건까지 가져올 수 있습니다: " + total);
        }
    }

    private static InvalidRequestException tooLarge(String message) {
        return new InvalidRequestException(ErrorCode.IMPORT_TOO_LARGE, message);
    }

    /** 저장 전에 전부 검사. 하나라도 범위 밖(미래·1900년 이전)이면 아무것도 안 들어감. 오늘은 한 번만 계산 */
    private void rejectOutOfRangeDates(LocalDate today, AccountRestoreRequest request) {
        for (AccountRestoreRequest.VehicleData data : request.vehicles()) {
            for (AccountRestoreRequest.MaintenanceData record : data.maintenanceRecords()) {
                UserToday.rejectOutOfRange(today, record.serviceDate(), "maintenanceRecords.serviceDate");
            }
            for (AccountRestoreRequest.FuelData record : data.fuelRecords()) {
                UserToday.rejectOutOfRange(today, record.fueledAt(), "fuelRecords.fueledAt");
            }
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

    /**
     * 정비는 날짜 오름차순으로 넣음. 내보낸 파일은 최신순이라 그대로 넣으면 id 가 뒤집혀
     * 같은 날 두 건 중 "나중 것" 이 바뀌고 다음 정비 계산이 달라짐
     * 같은 날짜끼리는 파일 순서를 거꾸로(내보내기의 id 내림차순 → 오름차순)
     */
    private List<AccountRestoreRequest.MaintenanceData> inInsertOrder(
            List<AccountRestoreRequest.MaintenanceData> records) {
        List<AccountRestoreRequest.MaintenanceData> ordered = new ArrayList<>(records);
        Collections.reverse(ordered);
        ordered.sort(Comparator.comparing(AccountRestoreRequest.MaintenanceData::serviceDate));
        return ordered;
    }

    /** 결과 집계. 응답의 여섯 수 */
    private static final class Tally {
        private int addedVehicles;
        private int mergedVehicles;
        private int addedMaintenance;
        private int addedFuel;
        private int skipped;
        private int addedIntervals;
    }
}
