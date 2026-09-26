package com.odolog.app.summary.service.application;

import com.odolog.app.fuel.domain.entity.FuelRecord;
import com.odolog.app.fuel.repository.jpa.FuelRecordRepository;
import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.type.ServiceType;
import com.odolog.app.maintenance.repository.jpa.MaintenanceRecordRepository;
import com.odolog.app.maintenance.repository.jpa.ServiceIntervalRepository;
import com.odolog.app.summary.dto.response.garage.GarageSummaryResponse;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.vehicle.repository.jpa.VehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GarageSummaryServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Mock
    private ServiceIntervalRepository serviceIntervalRepository;

    @Mock
    private FuelRecordRepository fuelRecordRepository;

    @InjectMocks
    private GarageSummaryService garageSummaryService;

    private Vehicle vehicle(Long id, String plate, int odometer) {
        User owner = new User("owner@odolog.com", "encoded", "차주", null);
        ReflectionTestUtils.setField(owner, "id", 1L);
        Vehicle vehicle = new Vehicle(owner, plate, "현대", "아반떼", 2023);
        ReflectionTestUtils.setField(vehicle, "id", id);
        vehicle.updateOdometer(odometer);
        return vehicle;
    }

    private MaintenanceRecord record(Long id, Vehicle vehicle, ServiceType type, int cost, LocalDate date) {
        MaintenanceRecord record = new MaintenanceRecord(vehicle, type, null, cost, 10000, date);
        ReflectionTestUtils.setField(record, "id", id);
        // 공개 id 도 읽을 수 있게 고정 — 무작위면 단언을 쓸 수 없다
        ReflectionTestUtils.setField(record, "publicId", "R" + id);
        return record;
    }

    private FuelRecord fuel(Long id, Vehicle vehicle, int odometer, String liters, int cost, LocalDate date) {
        FuelRecord record = new FuelRecord(vehicle, date, odometer, new BigDecimal(liters), cost, null);
        ReflectionTestUtils.setField(record, "id", id);
        // 공개 id 도 읽을 수 있게 고정 — 무작위면 단언을 쓸 수 없다
        ReflectionTestUtils.setField(record, "publicId", "R" + id);
        return record;
    }

    private void given(List<Vehicle> vehicles, List<MaintenanceRecord> records, List<FuelRecord> fuels) {
        when(vehicleRepository.findAllByOwnerId(1L)).thenReturn(vehicles);
        when(maintenanceRecordRepository.findByVehicle_Owner_IdOrderByServiceDateDescIdDesc(1L))
                .thenReturn(records);
        when(fuelRecordRepository.findByVehicle_Owner_IdOrderByOdometerAscIdAsc(1L)).thenReturn(fuels);
    }

    @Test
    @DisplayName("총 비용은 정비비와 유류비를 합친 값이고, 구성도 함께 준다")
    void totalsIncludeFuel() {
        Vehicle car = vehicle(10L, "12가3456", 50000);
        given(List.of(car),
                List.of(record(1L, car, ServiceType.ENGINE_OIL, 80000, LocalDate.of(2026, 9, 1))),
                List.of(fuel(1L, car, 49000, "30.00", 68000, LocalDate.of(2026, 9, 5))));

        GarageSummaryResponse summary = garageSummaryService.summarize(1L, TODAY);

        assertThat(summary.vehicleCount()).isEqualTo(1);
        assertThat(summary.totalOdometer()).isEqualTo(50000);
        assertThat(summary.recordCount()).isEqualTo(2);
        assertThat(summary.maintenanceCost()).isEqualTo(80000);
        assertThat(summary.fuelCost()).isEqualTo(68000);
        assertThat(summary.totalCost()).isEqualTo(148000);
    }

    @Test
    @DisplayName("월별은 항상 12칸이고 기록 없는 달은 0이다")
    void monthlyAlwaysTwelve() {
        Vehicle car = vehicle(10L, "12가3456", 50000);
        given(List.of(car),
                List.of(record(1L, car, ServiceType.ENGINE_OIL, 80000, LocalDate.of(2026, 9, 1))),
                List.of(fuel(1L, car, 49000, "30.00", 68000, LocalDate.of(2026, 9, 5))));

        List<GarageSummaryResponse.MonthlyCost> monthly = garageSummaryService.summarize(1L, TODAY).monthly();

        assertThat(monthly).hasSize(12);
        // 2026-09 기준 12칸이면 2025-10 시작
        assertThat(monthly.get(0).month()).isEqualTo("2025-10");
        assertThat(monthly.get(11).month()).isEqualTo("2026-09");

        GarageSummaryResponse.MonthlyCost september = monthly.get(11);
        assertThat(september.maintenanceCost()).isEqualTo(80000);
        assertThat(september.fuelCost()).isEqualTo(68000);
        assertThat(september.cost()).isEqualTo(148000);
        assertThat(september.count()).isEqualTo(2);

        assertThat(monthly.get(0).cost()).isZero();
    }

    @Test
    @DisplayName("종류별은 기록 있는 종류만, 비용 내림차순이다")
    void byTypeOnlyRecorded() {
        Vehicle car = vehicle(10L, "12가3456", 50000);
        given(List.of(car), List.of(
                record(1L, car, ServiceType.ENGINE_OIL, 50000, LocalDate.of(2026, 9, 1)),
                record(2L, car, ServiceType.TRANSMISSION_FLUID, 200000, LocalDate.of(2026, 8, 1)),
                record(3L, car, ServiceType.ENGINE_OIL, 50000, LocalDate.of(2026, 7, 1))),
                List.of());

        List<GarageSummaryResponse.TypeCost> byType = garageSummaryService.summarize(1L, TODAY).byType();

        // 15종 중 기록 있는 2종만
        assertThat(byType).hasSize(2);
        assertThat(byType.get(0).type()).isEqualTo(ServiceType.TRANSMISSION_FLUID);
        assertThat(byType.get(0).cost()).isEqualTo(200000);
        assertThat(byType.get(1).type()).isEqualTo(ServiceType.ENGINE_OIL);
        assertThat(byType.get(1).cost()).isEqualTo(100000);
        assertThat(byType.get(1).count()).isEqualTo(2);
    }

    @Test
    @DisplayName("차량별 평균 연비는 첫 주유량을 뺀 나머지로 나눈다")
    void averageEfficiencyExcludesFirstFill() {
        Vehicle car = vehicle(10L, "12가3456", 11000);
        given(List.of(car), List.of(), List.of(
                fuel(1L, car, 10000, "30.00", 60000, LocalDate.of(2026, 7, 1)),
                fuel(2L, car, 10500, "25.00", 50000, LocalDate.of(2026, 8, 1)),
                fuel(3L, car, 11000, "25.00", 50000, LocalDate.of(2026, 9, 1))));

        GarageSummaryResponse.VehicleLine line = garageSummaryService.summarize(1L, TODAY).vehicles().get(0);

        // 1000km ÷ (80 - 30)L = 20.00. 첫 30L 를 안 빼면 12.50
        assertThat(line.averageEfficiency()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("주유 기록이 1건뿐이면 평균 연비를 낼 수 없다")
    void averageEfficiencyNeedsTwo() {
        Vehicle car = vehicle(10L, "12가3456", 10000);
        given(List.of(car), List.of(),
                List.of(fuel(1L, car, 10000, "30.00", 60000, LocalDate.of(2026, 9, 1))));

        assertThat(garageSummaryService.summarize(1L, TODAY).vehicles().get(0).averageEfficiency()).isNull();
    }

    @Test
    @DisplayName("최근 활동: 금액을 안 적은 주유는 0 이 아니라 null — 한 건 표시에서 0 은 \"0원에 넣었다\" 로 읽힌다")
    void recentKeepsUnknownCostAsNull() {
        Vehicle car = vehicle(10L, "12가3456", 50000);
        FuelRecord bare = new FuelRecord(car, LocalDate.of(2026, 9, 12), 50000, null, null, null);
        ReflectionTestUtils.setField(bare, "id", 1L);
        given(List.of(car), List.of(), List.of(bare));

        GarageSummaryResponse summary = garageSummaryService.summarize(1L, TODAY);

        assertThat(summary.recent().get(0).cost()).isNull();
        // 합계에서는 여전히 0 으로 친다 — 더할 때만은 "없음" 과 0 이 같은 뜻
        assertThat(summary.fuelCost()).isZero();
    }

    @Test
    @DisplayName("최근 활동은 날짜 내림차순, 같은 날이면 정비가 주유보다 앞선다")
    void recentOrder() {
        Vehicle car = vehicle(10L, "12가3456", 50000);
        LocalDate sameDay = LocalDate.of(2026, 9, 10);
        given(List.of(car), List.of(
                record(9L, car, ServiceType.ENGINE_OIL, 80000, sameDay),
                record(7L, car, ServiceType.TIRE, 300000, sameDay)),
                List.of(
                        fuel(3L, car, 49000, "30.00", 68000, sameDay),
                        fuel(1L, car, 50000, "30.00", 70000, LocalDate.of(2026, 9, 12))));

        List<GarageSummaryResponse.RecentActivity> recent = garageSummaryService.summarize(1L, TODAY).recent();

        assertThat(recent).extracting(GarageSummaryResponse.RecentActivity::kind)
                .containsExactly("FUEL", "MAINTENANCE", "MAINTENANCE", "FUEL");
        // 같은 날짜면 정비 먼저. 문자열 정렬이면 FUEL 이 앞서 깨짐
        assertThat(recent.get(1).recordId()).isEqualTo("R9");
        assertThat(recent.get(2).recordId()).isEqualTo("R7");
        // 차량 이름은 이미 읽어 둔 목록에서
        assertThat(recent.get(0).vehicleName()).isEqualTo("현대 아반떼");
    }

    @Test
    @DisplayName("차량이 없으면 전부 0이고 월별은 여전히 12칸이다")
    void emptyGarage() {
        given(List.of(), List.of(), List.of());

        GarageSummaryResponse summary = garageSummaryService.summarize(1L, TODAY);

        assertThat(summary.vehicleCount()).isZero();
        assertThat(summary.totalCost()).isZero();
        assertThat(summary.vehicles()).isEmpty();
        assertThat(summary.recent()).isEmpty();
        assertThat(summary.byType()).isEmpty();
        assertThat(summary.monthly()).hasSize(12);
    }
}
