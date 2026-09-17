package com.odolog.app.summary.service.application;

import com.odolog.app.fuel.domain.entity.FuelRecord;
import com.odolog.app.fuel.repository.jpa.FuelRecordRepository;
import com.odolog.app.fuel.domain.calculation.FuelEfficiency;
import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.type.ServiceType;
import com.odolog.app.maintenance.repository.jpa.MaintenanceRecordRepository;
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
 * 홈 화면 요약. vehicle · maintenance · fuel 을 모두 읽는 조회 전용 서비스다.
 *
 * <p><b>왜 새 패키지인가.</b> 회원 탈퇴({@code account})와 같은 문제다 — 세 기능을 모두 알아야
 * 하는데 어느 한 기능에 넣으면 의존이 역방향이 된다. 다만 탈퇴는 <i>순서를 조율</i>하고
 * 이쪽은 <i>읽어서 합친다</i>는 점이 달라, 성격이 섞이지 않게 패키지를 따로 뒀다.
 *
 * <p>리포지토리를 직접 쓰는 것은 의도적이다. 여기서 하는 일은 집계뿐이라 각 기능의 서비스가
 * 가진 비즈니스 규칙(소유권 검사, 삭제 순서 같은 것)이 필요 없다. 소유자 id 로 조회하므로
 * 애초에 남의 데이터가 섞이지 않는다.
 */
@Service
@Transactional(readOnly = true)
public class GarageSummaryService {

    private static final int MONTHS_SHOWN = 12;
    private static final int RECENT_LIMIT = 5;

    private final VehicleRepository vehicleRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final FuelRecordRepository fuelRecordRepository;

    public GarageSummaryService(VehicleRepository vehicleRepository,
                                MaintenanceRecordRepository maintenanceRecordRepository,
                                FuelRecordRepository fuelRecordRepository) {
        this.vehicleRepository = vehicleRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.fuelRecordRepository = fuelRecordRepository;
    }

    public GarageSummaryResponse summarize(Long ownerId, LocalDate today) {
        // 쿼리 3번. 전에는 HTTP 왕복이 1 + 차량수 × 2 번이었다.
        List<Vehicle> vehicles = vehicleRepository.findAllByOwnerId(ownerId);
        List<MaintenanceRecord> records =
                maintenanceRecordRepository.findByVehicle_Owner_IdOrderByServiceDateDescIdDesc(ownerId);
        List<FuelRecord> fuels = fuelRecordRepository.findByVehicle_Owner_IdOrderByOdometerAscIdAsc(ownerId);

        long maintenanceCost = records.stream().mapToLong(MaintenanceRecord::getCost).sum();
        long fuelCost = fuels.stream().mapToLong(FuelRecord::getTotalCost).sum();

        return new GarageSummaryResponse(
                vehicles.size(),
                vehicles.stream().mapToLong(Vehicle::getOdometer).sum(),
                records.size() + fuels.size(),
                maintenanceCost + fuelCost,
                maintenanceCost,
                fuelCost,
                monthly(records, fuels, today),
                byType(records),
                vehicleLines(vehicles, records, fuels),
                recent(records, fuels, vehicles));
    }

    /** 기록이 없는 달도 0 으로 채운다. 있는 달만 모으면 가로축이 등간격이 아니게 된다. */
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
            buckets.put(key, new Bucket(bucket.maintenance(), bucket.fuel() + record.getTotalCost(),
                    bucket.count() + 1));
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

    /** 기록이 있는 종류만. 0원짜리 줄로 화면을 채우지 않는다. */
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

    private List<VehicleLine> vehicleLines(List<Vehicle> vehicles, List<MaintenanceRecord> records,
                                           List<FuelRecord> fuels) {
        List<VehicleLine> lines = new ArrayList<>(vehicles.size());

        for (Vehicle vehicle : vehicles) {
            // getId() 는 LAZY 프록시에서도 초기화 없이 읽힌다 — FK 를 이미 들고 있기 때문.
            List<MaintenanceRecord> mine = records.stream()
                    .filter(record -> record.getVehicle().getId().equals(vehicle.getId()))
                    .toList();
            List<FuelRecord> myFuels = fuels.stream()
                    .filter(record -> record.getVehicle().getId().equals(vehicle.getId()))
                    .toList();

            lines.add(new VehicleLine(
                    vehicle.getId(), vehicle.getPlateNumber(), vehicle.getManufacturer(),
                    vehicle.getModelName(), vehicle.getOdometer(),
                    mine.size(),
                    // records 가 serviceDate 내림차순이라 첫 줄이 가장 최근이다.
                    mine.isEmpty() ? null : mine.get(0).getServiceDate(),
                    FuelEfficiency.of(myFuels).average()));
        }

        return lines;
    }

    private List<RecentActivity> recent(List<MaintenanceRecord> records, List<FuelRecord> fuels,
                                        List<Vehicle> vehicles) {
        /*
         * 이름은 이미 읽어 둔 차량에서 찾는다. record.getVehicle().getManufacturer() 로 읽으면
         * LAZY 프록시가 초기화되는데(getId() 와 달리 FK 만으로는 알 수 없는 값이다),
         * 같은 트랜잭션이라 1차 캐시가 받아 줄 뿐 그 사실에 기대는 코드가 된다.
         * 지도를 만들어 두면 로딩 순서와 무관하게 쿼리가 더 나가지 않는다.
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
                    vehicleId, names.get(vehicleId), record.getTotalCost(), null, record.getLiters()));
        }

        return all.stream()
                .sorted(Comparator.comparing(RecentActivity::date).reversed()
                        // 같은 날짜면 정비 먼저. kind 를 문자열로 비교하면 "FUEL" < "MAINTENANCE" 라
                        // 주유가 앞선다 — 순서를 알파벳에 맡기지 않고 직접 정한다.
                        .thenComparingInt(activity -> "MAINTENANCE".equals(activity.kind()) ? 0 : 1)
                        // 같은 종류끼리만 id 내림차순. 정비 3번과 주유 3번은 테이블이 달라 무관하다.
                        .thenComparing(Comparator.comparingLong(RecentActivity::recordId).reversed()))
                .limit(RECENT_LIMIT)
                .toList();
    }
}
