package com.odolog.app.fuel.service.application;

import com.odolog.app.fuel.domain.entity.FuelRecord;
import com.odolog.app.fuel.dto.request.register.FuelRecordRegisterRequest;
import com.odolog.app.fuel.dto.request.update.FuelRecordUpdateRequest;
import com.odolog.app.fuel.dto.response.record.FuelRecordResponse;
import com.odolog.app.fuel.dto.response.summary.FuelSummaryResponse;
import com.odolog.app.fuel.repository.jpa.FuelRecordRepository;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.vehicle.service.application.VehicleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelRecordServiceTest {

    @Mock
    private FuelRecordRepository fuelRecordRepository;

    @Mock
    private VehicleService vehicleService;

    @InjectMocks
    private FuelRecordService fuelRecordService;

    private Vehicle vehicle(int odometer) {
        User owner = new User("owner@odolog.com", "encoded", "차주", null);
        ReflectionTestUtils.setField(owner, "id", 1L);
        Vehicle vehicle = new Vehicle(owner, "12가3456", "현대", "아반떼", 2020);
        ReflectionTestUtils.setField(vehicle, "id", 10L);
        vehicle.updateOdometer(odometer);
        return vehicle;
    }

    private FuelRecord record(Long id, Vehicle vehicle, int odometer, String liters, int cost) {
        FuelRecord record = new FuelRecord(vehicle, LocalDate.of(2026, 9, 1), odometer,
                new BigDecimal(liters), cost, null);
        ReflectionTestUtils.setField(record, "id", id);
        return record;
    }

    /** 주유량도 금액도 안 적고 저장한 기록 */
    private FuelRecord bare(Long id, Vehicle vehicle, int odometer) {
        FuelRecord record = new FuelRecord(vehicle, LocalDate.of(2026, 9, 1), odometer, null, null, null);
        ReflectionTestUtils.setField(record, "id", id);
        return record;
    }

    @Test
    @DisplayName("직전 기록이 없으면 연비와 주행거리가 null 이다")
    void firstRecordHasNoEfficiency() {
        Vehicle vehicle = vehicle(0);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(fuelRecordRepository.save(any(FuelRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(fuelRecordRepository.findPrevious(
                eq(10L), anyInt(), any())).thenReturn(Optional.empty());

        FuelRecordResponse response = fuelRecordService.register(1L, 10L,
                new FuelRecordRegisterRequest(LocalDate.of(2026, 9, 1), 10000,
                        new BigDecimal("30.00"), 60000, null));

        assertThat(response.distance()).isNull();
        assertThat(response.efficiency()).isNull();
        // 단가는 직전과 무관. 60000 / 30 = 2000
        assertThat(response.pricePerLiter()).isEqualTo(2000);
    }

    @Test
    @DisplayName("연비는 직전 주유 이후 달린 거리를 이번 주유량으로 나눈 값이다")
    void efficiencyFromPrevious() {
        Vehicle vehicle = vehicle(10000);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(fuelRecordRepository.save(any(FuelRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(fuelRecordRepository.findPrevious(
                eq(10L), anyInt(), any()))
                .thenReturn(Optional.of(record(1L, vehicle, 10000, "30.00", 60000)));

        FuelRecordResponse response = fuelRecordService.register(1L, 10L,
                new FuelRecordRegisterRequest(LocalDate.of(2026, 9, 10), 10500,
                        new BigDecimal("25.00"), 50000, null));

        assertThat(response.distance()).isEqualTo(500);
        assertThat(response.efficiency()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("주유 기록의 주행거리가 더 크면 차량의 주행거리도 따라 올라간다")
    void registerUpdatesVehicleOdometer() {
        Vehicle vehicle = vehicle(9000);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(fuelRecordRepository.save(any(FuelRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(fuelRecordRepository.findPrevious(
                eq(10L), anyInt(), any())).thenReturn(Optional.empty());

        fuelRecordService.register(1L, 10L, new FuelRecordRegisterRequest(
                LocalDate.of(2026, 9, 1), 10000, new BigDecimal("30.00"), 60000, null));

        assertThat(vehicle.getOdometer()).isEqualTo(10000);
    }

    @Test
    @DisplayName("과거 주유를 뒤늦게 입력해도 차량의 주행거리는 내려가지 않는다")
    void registerDoesNotLowerVehicleOdometer() {
        Vehicle vehicle = vehicle(50000);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(fuelRecordRepository.save(any(FuelRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(fuelRecordRepository.findPrevious(
                eq(10L), anyInt(), any())).thenReturn(Optional.empty());

        fuelRecordService.register(1L, 10L, new FuelRecordRegisterRequest(
                LocalDate.of(2026, 1, 1), 10000, new BigDecimal("30.00"), 60000, null));

        assertThat(vehicle.getOdometer()).isEqualTo(50000);
    }

    @Test
    @DisplayName("목록의 마지막 행만 직전 기록을 따로 조회한다 — 행마다 조회하면 N+1")
    void listQueriesPreviousOnlyOnce() {
        Vehicle vehicle = vehicle(11000);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);

        // 내림차순 11000 → 10500 이 한 페이지. 10500 의 짝(10000)은 다음 페이지
        List<FuelRecord> items = List.of(
                record(3L, vehicle, 11000, "25.00", 50000),
                record(2L, vehicle, 10500, "25.00", 50000));
        Pageable pageable = PageRequest.of(0, 2);
        when(fuelRecordRepository.findByVehicleId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(items, pageable, 3));
        when(fuelRecordRepository.findPrevious(eq(10L), eq(10500), any()))
                .thenReturn(Optional.of(record(1L, vehicle, 10000, "30.00", 60000)));

        Page<FuelRecordResponse> page = fuelRecordService.findByVehicle(1L, 10L, pageable);

        // 첫 행은 페이지 안쪽끼리 짝 (11000 - 10500) / 25
        assertThat(page.getContent().get(0).efficiency()).isEqualByComparingTo("20.00");
        // 마지막 행은 페이지 밖에서 가져온 짝 (10500 - 10000) / 25
        assertThat(page.getContent().get(1).efficiency()).isEqualByComparingTo("20.00");

        // 직전 조회는 한 번뿐 — 행마다면 N+1
        verify(fuelRecordRepository)
                .findPrevious(anyLong(), anyInt(), any());
    }

    @Test
    @DisplayName("주유량·금액을 안 적은 기록은 연비도 단가도 0 이 아니라 null 이다")
    void bareRecordHasNoDerivedValues() {
        Vehicle vehicle = vehicle(11000);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);

        Pageable pageable = PageRequest.of(0, 2);
        when(fuelRecordRepository.findByVehicleId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        bare(2L, vehicle, 10500),
                        record(1L, vehicle, 10000, "30.00", 60000)), pageable, 2));

        FuelRecordResponse bare = fuelRecordService.findByVehicle(1L, 10L, pageable).getContent().get(0);

        // 거리는 안다 — 모르는 것은 "얼마나 넣었나" 뿐이다
        assertThat(bare.distance()).isEqualTo(500);
        // 0 을 주면 "연비 0km/L 인 차" 와 구분되지 않는다
        assertThat(bare.efficiency()).isNull();
        assertThat(bare.pricePerLiter()).isNull();
        assertThat(bare.liters()).isNull();
        assertThat(bare.totalCost()).isNull();
    }

    @Test
    @DisplayName("메모를 비우면 빈 문자열이 아니라 null 로 저장한다")
    void blankMemoBecomesNull() {
        // "없음" 이 null 과 '' 두 모양이면 내보낸 JSON 에도 그대로 나간다 (phone 과 같은 규칙)
        Vehicle vehicle = vehicle(10000);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(fuelRecordRepository.save(any(FuelRecord.class))).thenAnswer(call -> call.getArgument(0));

        fuelRecordService.register(1L, 10L, new FuelRecordRegisterRequest(
                LocalDate.of(2026, 9, 1), 10500, new BigDecimal("25.00"), 50000, "   "));

        ArgumentCaptor<FuelRecord> saved = ArgumentCaptor.forClass(FuelRecord.class);
        verify(fuelRecordRepository).save(saved.capture());
        assertThat(saved.getValue().getMemo()).isNull();
    }

    @Test
    @DisplayName("총 유류비가 20억을 넘어도 음수가 되지 않는다")
    void totalCostDoesNotOverflow() {
        /*
         * int 로 누적하던 시절 4,000,000,000 원이 -294,967,296 으로 찍혔다.
         * 홈 요약은 long 이라 같은 데이터에 4,000,000,000 을 주던 상태 —
         * 같은 값을 두 화면이 다르게 말했다.
         */
        Vehicle vehicle = vehicle(11000);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(10L)).thenReturn(List.of(
                record(1L, vehicle, 10000, "10.00", 2_000_000_000),
                record(2L, vehicle, 10500, "10.00", 2_000_000_000)));

        assertThat(fuelRecordService.summary(1L, 10L).totalCost()).isEqualTo(4_000_000_000L);
    }

    @Test
    @DisplayName("요약의 합계는 적힌 것만 더한다 — 안 적은 기록이 0 으로 섞이지 않는다")
    void summarySkipsUnrecordedValues() {
        Vehicle vehicle = vehicle(11000);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(10L)).thenReturn(List.of(
                record(1L, vehicle, 10000, "30.00", 60000),
                bare(2L, vehicle, 10500),
                record(3L, vehicle, 11000, "25.00", 50000)));

        FuelSummaryResponse summary = fuelRecordService.summary(1L, 10L);

        // 건수는 셋 — 기록 자체는 있었던 일이다
        assertThat(summary.recordCount()).isEqualTo(3);
        assertThat(summary.totalCost()).isEqualTo(110000);
        assertThat(summary.totalLiters()).isEqualByComparingTo("55.00");
    }

    @Test
    @DisplayName("기록이 빠진 것으로 보이는 구간은 목록 행에 표시된다")
    void listMarksMissingRecordSegment() {
        Vehicle vehicle = vehicle(12000);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);

        // 평소 400km/40L(10km/L) 인데 마지막 구간만 800km — 기록 하나가 빠졌거나 지워진 모양
        List<FuelRecord> ascending = List.of(
                record(1L, vehicle, 10000, "40.00", 60000),
                record(2L, vehicle, 10400, "40.00", 60000),
                record(3L, vehicle, 10800, "40.00", 60000),
                record(4L, vehicle, 11200, "40.00", 60000),
                record(5L, vehicle, 12000, "40.00", 60000));
        when(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(10L)).thenReturn(ascending);

        // 화면 목록은 내림차순
        Pageable pageable = PageRequest.of(0, 2);
        when(fuelRecordRepository.findByVehicleId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ascending.get(4), ascending.get(3)), pageable, 5));

        Page<FuelRecordResponse> page = fuelRecordService.findByVehicle(1L, 10L, pageable);

        FuelRecordResponse suspicious = page.getContent().get(0);
        // 값은 지우지 않는다 — 무엇이 이상한지 보려면 20.00 이 남아 있어야 한다
        assertThat(suspicious.efficiency()).isEqualByComparingTo("20.00");
        assertThat(suspicious.missingRecordSuspected()).isTrue();
        // 50 을 넘지 않아 '불가능'은 아니다. 표시가 둘이면 무엇을 하라는 건지 흐려진다
        assertThat(suspicious.efficiencySuspicious()).isFalse();

        // 평소 구간은 아무 표시도 없다
        assertThat(page.getContent().get(1).missingRecordSuspected()).isFalse();
    }

    @Test
    @DisplayName("기준을 페이지가 아니라 이력 전체에서 잡는다 — 같은 행이 페이지마다 달리 판정되면 안 된다")
    void baselineComesFromWholeHistory() {
        Vehicle vehicle = vehicle(12000);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);

        List<FuelRecord> ascending = List.of(
                record(1L, vehicle, 10000, "40.00", 60000),
                record(2L, vehicle, 10400, "40.00", 60000),
                record(3L, vehicle, 10800, "40.00", 60000),
                record(4L, vehicle, 11200, "40.00", 60000),
                record(5L, vehicle, 12000, "40.00", 60000));
        when(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(10L)).thenReturn(ascending);

        Pageable pageable = PageRequest.of(0, 2);
        when(fuelRecordRepository.findByVehicleId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ascending.get(4), ascending.get(3)), pageable, 5));

        fuelRecordService.findByVehicle(1L, 10L, pageable);

        // 이 페이지에는 구간이 하나뿐이라, 전체를 안 읽으면 '평소'를 못 구해 아무것도 못 잡는다
        verify(fuelRecordRepository).findAllByVehicleIdOrderByOdometerAscIdAsc(10L);
    }

    @Test
    @DisplayName("평균 연비는 첫 주유량을 뺀 나머지로 나눈다")
    void averageEfficiencyExcludesFirstFill() {
        Vehicle vehicle = vehicle(11000);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(10L)).thenReturn(List.of(
                record(1L, vehicle, 10000, "30.00", 60000),
                record(2L, vehicle, 10500, "25.00", 50000),
                record(3L, vehicle, 11000, "25.00", 50000)));

        FuelSummaryResponse summary = fuelRecordService.summary(1L, 10L);

        assertThat(summary.recordCount()).isEqualTo(3);
        assertThat(summary.totalCost()).isEqualTo(160000);
        assertThat(summary.totalLiters()).isEqualByComparingTo("80.00");
        assertThat(summary.totalDistance()).isEqualTo(1000);
        // 1000km ÷ (80 - 30)L = 20.00. 첫 30L 를 안 빼면 12.50
        assertThat(summary.averageEfficiency()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("기록이 1건뿐이면 평균 연비를 낼 수 없다")
    void averageEfficiencyNeedsTwoRecords() {
        Vehicle vehicle = vehicle(10000);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(10L))
                .thenReturn(List.of(record(1L, vehicle, 10000, "30.00", 60000)));

        FuelSummaryResponse summary = fuelRecordService.summary(1L, 10L);

        assertThat(summary.totalDistance()).isNull();
        assertThat(summary.averageEfficiency()).isNull();
        assertThat(summary.totalCost()).isEqualTo(60000);
    }

    @Test
    @DisplayName("주유 기록을 수정해 주행거리를 올리면 차량 주행거리도 따라 올라간다")
    void updateLiftsVehicleOdometer() {
        Vehicle vehicle = vehicle(10000);
        FuelRecord existing = record(1L, vehicle, 10000, "30.00", 60000);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(fuelRecordRepository.findByIdAndVehicleId(1L, 10L)).thenReturn(Optional.of(existing));
        when(fuelRecordRepository.findPrevious(
                eq(10L), anyInt(), any())).thenReturn(Optional.empty());

        // 자리수 오타 정정
        fuelRecordService.update(1L, 10L, 1L,
                new FuelRecordUpdateRequest(null, 100000, null, null, null, null, null, null));

        assertThat(existing.getOdometer()).isEqualTo(100000);
        assertThat(vehicle.getOdometer()).isEqualTo(100000);
    }

    @Test
    @DisplayName("수정으로 주행거리를 낮춰도 차량 주행거리는 내려가지 않는다")
    void updateDoesNotLowerVehicleOdometer() {
        Vehicle vehicle = vehicle(50000);
        FuelRecord existing = record(1L, vehicle, 20000, "30.00", 60000);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(fuelRecordRepository.findByIdAndVehicleId(1L, 10L)).thenReturn(Optional.of(existing));
        when(fuelRecordRepository.findPrevious(
                eq(10L), anyInt(), any())).thenReturn(Optional.empty());

        fuelRecordService.update(1L, 10L, 1L,
                new FuelRecordUpdateRequest(null, 15000, null, null, null, null, null, null));

        assertThat(existing.getOdometer()).isEqualTo(15000);
        assertThat(vehicle.getOdometer()).isEqualTo(50000);
    }

    private FuelRecord resetPointAt(Long id, Vehicle vehicle, int odometer, String liters, int cost) {
        FuelRecord record = record(id, vehicle, odometer, liters, cost);
        record.changeResetPoint(true);
        return record;
    }

    @Test
    @DisplayName("연비 초기화 이후 구간만으로 평균을 낸다")
    void averageEfficiencySinceResetPoint() {
        Vehicle vehicle = vehicle(30000);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(10L)).thenReturn(List.of(
                // 초기화 이전 — 주행거리 오입력으로 연비가 엉망인 구간
                record(1L, vehicle, 10000, "90.00", 180000),
                record(2L, vehicle, 10100, "90.00", 180000),
                // 기준점
                resetPointAt(3L, vehicle, 20000, "30.00", 60000),
                record(4L, vehicle, 20500, "25.00", 50000),
                record(5L, vehicle, 21000, "25.00", 50000)));

        FuelSummaryResponse summary = fuelRecordService.summary(1L, 10L);

        // 1000km ÷ (80 - 30)L = 20.00. 초기화를 무시하면 11,000km 구간이 끼어듦
        assertThat(summary.totalDistance()).isEqualTo(1000);
        assertThat(summary.averageEfficiency()).isEqualByComparingTo("20.00");

        // 건수·비용·주유량은 전체 기준. 초기화 대상은 연비뿐
        assertThat(summary.recordCount()).isEqualTo(5);
        assertThat(summary.totalCost()).isEqualTo(520000);
        assertThat(summary.totalLiters()).isEqualByComparingTo("260.00");

        // 요약이 두 id 를 함께 줌 — 없으면 화면이 목록을 한 번 더 받아야 함
        assertThat(summary.latestRecordId()).isEqualTo(5L);
        assertThat(summary.resetPointId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("기준점이 가장 마지막 기록이면 평균 연비를 낼 수 없다 — 다음 주유부터 계산된다")
    void resetPointAtLatestLeavesNoAverage() {
        Vehicle vehicle = vehicle(20000);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(10L)).thenReturn(List.of(
                record(1L, vehicle, 10000, "30.00", 60000),
                resetPointAt(2L, vehicle, 20000, "30.00", 60000)));

        FuelSummaryResponse summary = fuelRecordService.summary(1L, 10L);

        assertThat(summary.averageEfficiency()).isNull();
        assertThat(summary.totalDistance()).isNull();
    }

    @Test
    @DisplayName("기준점으로 찍힌 기록 자체는 구간 연비가 없다 — 직전과의 연결이 끊긴다")
    void resetPointBreaksSegment() {
        Vehicle vehicle = vehicle(20000);
        FuelRecord existing = record(2L, vehicle, 20000, "25.00", 50000);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(fuelRecordRepository.findByIdAndVehicleId(2L, 10L)).thenReturn(Optional.of(existing));
        when(fuelRecordRepository.findPrevious(
                eq(10L), anyInt(), any())).thenReturn(Optional.of(record(1L, vehicle, 19500, "30.00", 60000)));

        // 끄기 전 500km ÷ 25L = 20.00
        FuelRecordResponse before = fuelRecordService.update(1L, 10L, 2L,
                new FuelRecordUpdateRequest(null, null, null, null, null, null, null, false));
        assertThat(before.efficiency()).isEqualByComparingTo("20.00");

        FuelRecordResponse after = fuelRecordService.update(1L, 10L, 2L,
                new FuelRecordUpdateRequest(null, null, null, null, null, null, null, true));

        assertThat(after.resetPoint()).isTrue();
        assertThat(after.efficiency()).isNull();
        assertThat(after.distance()).isNull();
    }
}
