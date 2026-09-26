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
 * 홈 요약. vehicle·maintenance·fuel 을 읽는 조회 전용 조율 층
 * 집계에는 비즈니스 규칙이 필요 없어 리포지토리 주입
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
        // 쿼리 3번
        List<Vehicle> vehicles = vehicleRepository.findAllByOwnerId(ownerId);
        List<MaintenanceRecord> records =
                maintenanceRecordRepository.findByVehicle_Owner_IdOrderByServiceDateDescIdDesc(ownerId);
        List<FuelRecord> fuels = fuelRecordRepository.findByVehicle_Owner_IdOrderByOdometerAscIdAsc(ownerId);

        long maintenanceCost = records.stream().mapToLong(MaintenanceRecord::getCost).sum();
        // 금액을 안 적은 기록은 합계에서 0
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

    /** 빈 달도 0 으로 채움. 가로축 등간격 유지 */
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

    /** 기록 있는 종류만 */
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

    /** 한 번 읽어 차량별로 분배. 차량별 조회 방지 */
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
            // getId() 는 LAZY 프록시 초기화 없이 조회
            List<MaintenanceRecord> mine = records.stream()
                    .filter(record -> record.getVehicle().getId().equals(vehicle.getId()))
                    .toList();
            List<FuelRecord> myFuels = fuels.stream()
                    .filter(record -> record.getVehicle().getId().equals(vehicle.getId()))
                    .toList();

            // 이미 읽은 이력으로 계산. 추가 쿼리 없음
            long overdue = NextService.of(mine, intervalsOf(overrides, vehicle.getId()),
                            vehicle.getOdometer(), today).stream()
                    .filter(NextService::overdue)
                    .count();

            lines.add(new VehicleLine(
                    vehicle.getPublicId(), vehicle.getPlateNumber(), vehicle.getManufacturer(),
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
        // 차량 이름은 이미 읽은 목록에서 조회. LAZY 프록시 초기화 회피
        Map<Long, String> names = new LinkedHashMap<>();
        // 숫자 PK → 공개 id
        Map<Long, String> publicIds = new LinkedHashMap<>();
        for (Vehicle vehicle : vehicles) {
            names.put(vehicle.getId(), vehicle.getManufacturer() + " " + vehicle.getModelName());
            publicIds.put(vehicle.getId(), vehicle.getPublicId());
        }

        // 정렬용 숫자 id 보관. 공개 id 는 순서 정보 없음
        record Candidate(RecentActivity activity, long internalId) {
        }

        List<Candidate> all = new ArrayList<>(records.size() + fuels.size());
        for (MaintenanceRecord record : records) {
            Long vehicleId = record.getVehicle().getId();
            all.add(new Candidate(new RecentActivity("MAINTENANCE", record.getPublicId(),
                    record.getServiceDate(), publicIds.get(vehicleId), names.get(vehicleId),
                    (long) record.getCost(), record.getType(), null), record.getId()));
        }
        for (FuelRecord record : fuels) {
            Long vehicleId = record.getVehicle().getId();
            all.add(new Candidate(new RecentActivity("FUEL", record.getPublicId(),
                    record.getFueledAt(), publicIds.get(vehicleId), names.get(vehicleId),
                    record.getTotalCost() == null ? null : record.getTotalCost().longValue(),
                    null, record.getLiters()), record.getId()));
        }

        return all.stream()
                .sorted(Comparator.comparing((Candidate candidate) -> candidate.activity().date()).reversed()
                        // 같은 날짜면 정비 먼저
                        .thenComparingInt(candidate -> "MAINTENANCE".equals(candidate.activity().kind()) ? 0 : 1)
                        // 같은 종류끼리만 id 내림차순. 테이블이 달라 id 끼리 무관
                        .thenComparing(Comparator.comparingLong(Candidate::internalId).reversed()))
                .limit(RECENT_LIMIT)
                .map(Candidate::activity)
                .toList();
    }
}
