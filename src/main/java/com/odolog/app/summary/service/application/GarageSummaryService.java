package com.odolog.app.summary.service.application;

import com.odolog.app.fuel.domain.entity.FuelRecord;
import com.odolog.app.fuel.repository.jpa.FuelRecordRepository;
import com.odolog.app.fuel.domain.calculation.FuelEfficiency;
import com.odolog.app.maintenance.domain.calculation.NextService;
import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.entity.ServiceInterval;
import com.odolog.app.maintenance.domain.type.ServiceType;
import com.odolog.app.maintenance.repository.jpa.MaintenanceRecordRepository;
import com.odolog.app.maintenance.repository.jpa.ServiceIntervalRepository;
import com.odolog.app.summary.dto.response.garage.GarageSummaryResponse;
import com.odolog.app.summary.dto.response.garage.GarageSummaryResponse.MonthlyCost;
import com.odolog.app.summary.dto.response.garage.GarageSummaryResponse.RecentActivity;
import com.odolog.app.summary.dto.response.garage.GarageSummaryResponse.TypeCost;
import com.odolog.app.summary.dto.response.garage.GarageSummaryResponse.VehicleLine;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.vehicle.repository.jpa.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 홈 요약. vehicle · maintenance · fuel 을 모두 읽는 조회 전용
 * account 와 같은 조율 층이지만 그쪽은 순서 조율, 이쪽은 집계라 패키지를 나눔
 * 서비스가 아닌 리포지토리 주입 — 집계에는 소유권 검사·삭제 순서 같은 규칙이 불필요
 */
@Service
@Transactional(readOnly = true)
public class GarageSummaryService {

    private static final int MONTHS_SHOWN = 12;
    private static final int RECENT_LIMIT = 5;

    private final VehicleRepository vehicleRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final ServiceIntervalRepository serviceIntervalRepository;
    private final FuelRecordRepository fuelRecordRepository;

    public GarageSummaryService(VehicleRepository vehicleRepository,
                                MaintenanceRecordRepository maintenanceRecordRepository,
                                ServiceIntervalRepository serviceIntervalRepository,
                                FuelRecordRepository fuelRecordRepository) {
        this.vehicleRepository = vehicleRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.serviceIntervalRepository = serviceIntervalRepository;
        this.fuelRecordRepository = fuelRecordRepository;
    }

    public GarageSummaryResponse summarize(Long ownerId, LocalDate today) {
        // 쿼리 3번 (전에는 HTTP 왕복 1 + 차량수 × 2)
        List<Vehicle> vehicles = vehicleRepository.findAllByOwnerId(ownerId);
        List<MaintenanceRecord> records =
                maintenanceRecordRepository.findByVehicle_Owner_IdOrderByServiceDateDescIdDesc(ownerId);
        List<FuelRecord> fuels = fuelRecordRepository.findByVehicle_Owner_IdOrderByOdometerAscIdAsc(ownerId);

        long maintenanceCost = records.stream().mapToLong(MaintenanceRecord::getCost).sum();
        // 결제 금액을 안 적은 기록은 0 으로 친다 — 합계에서는 '없음' 과 0 이 같은 뜻이다
        long fuelCost = fuels.stream().mapToLong(FuelRecord::totalCostOrZero).sum();

        return new GarageSummaryResponse(
                vehicles.size(),
                vehicles.stream().mapToLong(Vehicle::getOdometer).sum(),
                records.size() + fuels.size(),
                maintenanceCost + fuelCost,
                maintenanceCost,
                fuelCost,
                monthly(records, fuels, today),
                byType(records),
                vehicleLines(vehicles, records, fuels,
                        serviceIntervalRepository.findByVehicle_Owner_Id(ownerId), today),
                recent(records, fuels, vehicles));
    }

    /** 빈 달도 0 으로 채움. 있는 달만 모으면 가로축이 등간격이 아니게 됨 */
    private List<MonthlyCost> monthly(List<MaintenanceRecord> records, List<FuelRecord> fuels,
                                      LocalDate today) {
        record Bucket(long maintenance, long fuel, int count) {
        }

        Map<YearMonth, Bucket> buckets = new LinkedHashMap<>();
        for (MaintenanceRecord record : records) {
            YearMonth key = YearMonth.from(record.getServiceDate());
            Bucket bucket = buckets.getOrDefault(key, new Bucket(0, 0, 0));
            buckets.put(key, new Bucket(bucket.maintenance() + record.getCost(), bucket.fuel(),
                    bucket.count() + 1));
        }
        for (FuelRecord record : fuels) {
            YearMonth key = YearMonth.from(record.getFueledAt());
            Bucket bucket = buckets.getOrDefault(key, new Bucket(0, 0, 0));
            buckets.put(key, new Bucket(bucket.maintenance(),
                    bucket.fuel() + record.totalCostOrZero(), bucket.count() + 1));
        }

        List<MonthlyCost> monthly = new ArrayList<>(MONTHS_SHOWN);
        YearMonth start = YearMonth.from(today).minusMonths(MONTHS_SHOWN - 1L);
        for (int i = 0; i < MONTHS_SHOWN; i++) {
            YearMonth month = start.plusMonths(i);
            Bucket bucket = buckets.getOrDefault(month, new Bucket(0, 0, 0));
            monthly.add(new MonthlyCost(month.toString(),
                    bucket.maintenance() + bucket.fuel(),
                    bucket.maintenance(), bucket.fuel(), bucket.count()));
        }
        return monthly;
    }

    /** 기록 있는 종류만. 0원짜리 줄 제외 */
    private List<TypeCost> byType(List<MaintenanceRecord> records) {
        Map<ServiceType, long[]> sums = new EnumMap<>(ServiceType.class);
        for (MaintenanceRecord record : records) {
            long[] entry = sums.computeIfAbsent(record.getType(), key -> new long[2]);
            entry[0] += record.getCost();
            entry[1]++;
        }

        return sums.entrySet().stream()
                .map(entry -> new TypeCost(entry.getKey(), entry.getValue()[0], (int) entry.getValue()[1]))
                .sorted(Comparator.comparingLong(TypeCost::cost).reversed())
                .toList();
    }

    /** 차량마다 조회하면 차량 수만큼 쿼리가 는다. 한 번 읽어 와서 나눈다 */
    private List<ServiceInterval> intervalsOf(List<ServiceInterval> all, Long vehicleId) {
        return all.stream()
                .filter(interval -> interval.getVehicle().getId().equals(vehicleId))
                .toList();
    }

    private List<VehicleLine> vehicleLines(List<Vehicle> vehicles, List<MaintenanceRecord> records,
                                           List<FuelRecord> fuels, List<ServiceInterval> overrides,
                                           LocalDate today) {
        List<VehicleLine> lines = new ArrayList<>(vehicles.size());

        for (Vehicle vehicle : vehicles) {
            // getId() 는 LAZY 프록시에서도 초기화 없이 읽힘 — FK 를 이미 들고 있음
            List<MaintenanceRecord> mine = records.stream()
                    .filter(record -> record.getVehicle().getId().equals(vehicle.getId()))
                    .toList();
            List<FuelRecord> myFuels = fuels.stream()
                    .filter(record -> record.getVehicle().getId().equals(vehicle.getId()))
                    .toList();

            // 이미 읽어 둔 이력으로 센다 — 추가 쿼리 없음
            long overdue = NextService.of(mine, intervalsOf(overrides, vehicle.getId()),
                            vehicle.getOdometer(), today).stream()
                    .filter(NextService::overdue)
                    .count();

            lines.add(new VehicleLine(
                    vehicle.getId(), vehicle.getPlateNumber(), vehicle.getManufacturer(),
                    vehicle.getModelName(), vehicle.getOdometer(),
                    mine.size(),
                    // serviceDate 내림차순이라 첫 줄이 최근
                    mine.isEmpty() ? null : mine.get(0).getServiceDate(),
                    FuelEfficiency.of(myFuels).average(),
                    (int) overdue));
        }

        return lines;
    }

    private List<RecentActivity> recent(List<MaintenanceRecord> records, List<FuelRecord> fuels,
                                        List<Vehicle> vehicles) {
        /*
         * 차량 이름은 이미 읽어 둔 목록에서 찾기
         * getVehicle().getManufacturer() 는 LAZY 프록시를 초기화 — getId() 와 달리 FK 만으로는 모름
         * 1차 캐시가 받아 주긴 하나 그 사실에 기대는 코드가 됨
         */
        Map<Long, String> names = new LinkedHashMap<>();
        for (Vehicle vehicle : vehicles) {
            names.put(vehicle.getId(), vehicle.getManufacturer() + " " + vehicle.getModelName());
        }

        List<RecentActivity> all = new ArrayList<>(records.size() + fuels.size());
        for (MaintenanceRecord record : records) {
            Long vehicleId = record.getVehicle().getId();
            all.add(new RecentActivity("MAINTENANCE", record.getId(), record.getServiceDate(),
                    vehicleId, names.get(vehicleId), record.getCost(), record.getType(), null));
        }
        for (FuelRecord record : fuels) {
            Long vehicleId = record.getVehicle().getId();
            all.add(new RecentActivity("FUEL", record.getId(), record.getFueledAt(),
                    vehicleId, names.get(vehicleId), record.totalCostOrZero(), null,
                    record.getLiters()));
        }

        return all.stream()
                .sorted(Comparator.comparing(RecentActivity::date).reversed()
                        // 같은 날짜면 정비 먼저. 문자열 비교면 "FUEL" < "MAINTENANCE" 로 뒤집힘
                        .thenComparingInt(activity -> "MAINTENANCE".equals(activity.kind()) ? 0 : 1)
                        // 같은 종류끼리만 id 내림차순 — 테이블이 달라 id 는 서로 무관
                        .thenComparing(Comparator.comparingLong(RecentActivity::recordId).reversed()))
                .limit(RECENT_LIMIT)
                .toList();
    }
}
