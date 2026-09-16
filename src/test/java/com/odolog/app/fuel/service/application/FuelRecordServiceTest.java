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

    @Test
    @DisplayName("직전 기록이 없으면 연비와 주행거리가 null 이다")
    void firstRecordHasNoEfficiency() {
        Vehicle vehicle = vehicle(0);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(fuelRecordRepository.save(any(FuelRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(fuelRecordRepository.findTopByVehicleIdAndOdometerLessThanOrderByOdometerDescIdDesc(
                eq(10L), anyInt())).thenReturn(Optional.empty());

        FuelRecordResponse response = fuelRecordService.register(1L, 10L,
                new FuelRecordRegisterRequest(LocalDate.of(2026, 9, 1), 10000,
                        new BigDecimal("30.00"), 60000, null));

        assertThat(response.distance()).isNull();
        assertThat(response.efficiency()).isNull();
        // 단가는 직전과 무관하게 언제나 나온다. 60000 / 30 = 2000
        assertThat(response.pricePerLiter()).isEqualTo(2000);
    }

    @Test
    @DisplayName("연비는 직전 주유 이후 달린 거리를 이번 주유량으로 나눈 값이다")
    void efficiencyFromPrevious() {
        Vehicle vehicle = vehicle(10000);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(fuelRecordRepository.save(any(FuelRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(fuelRecordRepository.findTopByVehicleIdAndOdometerLessThanOrderByOdometerDescIdDesc(
                eq(10L), anyInt()))
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
        when(fuelRecordRepository.findTopByVehicleIdAndOdometerLessThanOrderByOdometerDescIdDesc(
                eq(10L), anyInt())).thenReturn(Optional.empty());

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
        when(fuelRecordRepository.findTopByVehicleIdAndOdometerLessThanOrderByOdometerDescIdDesc(
                eq(10L), anyInt())).thenReturn(Optional.empty());

        fuelRecordService.register(1L, 10L, new FuelRecordRegisterRequest(
                LocalDate.of(2026, 1, 1), 10000, new BigDecimal("30.00"), 60000, null));

        assertThat(vehicle.getOdometer()).isEqualTo(50000);
    }

    @Test
    @DisplayName("목록의 마지막 행만 직전 기록을 따로 조회한다 — 쿼리는 2번뿐")
    void listQueriesPreviousOnlyOnce() {
        Vehicle vehicle = vehicle(11000);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);

        // 내림차순: 11000 → 10500 이 한 페이지. 10500 의 짝(10000)은 다음 페이지에 있다.
        List<FuelRecord> items = List.of(
                record(3L, vehicle, 11000, "25.00", 50000),
                record(2L, vehicle, 10500, "25.00", 50000));
        Pageable pageable = PageRequest.of(0, 2);
        when(fuelRecordRepository.findByVehicleId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(items, pageable, 3));
        when(fuelRecordRepository.findTopByVehicleIdAndOdometerLessThanOrderByOdometerDescIdDesc(10L, 10500))
                .thenReturn(Optional.of(record(1L, vehicle, 10000, "30.00", 60000)));

        Page<FuelRecordResponse> page = fuelRecordService.findByVehicle(1L, 10L, pageable);

        // 첫 행은 페이지 안쪽 행끼리 짝이 맞는다 (11000 - 10500) / 25
        assertThat(page.getContent().get(0).efficiency()).isEqualByComparingTo("20.00");
        // 마지막 행은 페이지 밖에서 가져온 짝 (10500 - 10000) / 25
        assertThat(page.getContent().get(1).efficiency()).isEqualByComparingTo("20.00");

        // 직전 조회는 딱 한 번 — 행마다 부르면 N+1 이다.
        verify(fuelRecordRepository)
                .findTopByVehicleIdAndOdometerLessThanOrderByOdometerDescIdDesc(anyLong(), anyInt());
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
        // 1000km ÷ (80 - 30)L = 20.00 km/L. 첫 30L 를 안 빼면 12.50 이 나온다.
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
        when(fuelRecordRepository.findTopByVehicleIdAndOdometerLessThanOrderByOdometerDescIdDesc(
                eq(10L), anyInt())).thenReturn(Optional.empty());

        // 자리수를 잘못 넣었다가 고치는 흔한 경우.
        fuelRecordService.update(1L, 10L, 1L,
                new FuelRecordUpdateRequest(null, 100000, null, null, null));

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
        when(fuelRecordRepository.findTopByVehicleIdAndOdometerLessThanOrderByOdometerDescIdDesc(
                eq(10L), anyInt())).thenReturn(Optional.empty());

        fuelRecordService.update(1L, 10L, 1L,
                new FuelRecordUpdateRequest(null, 15000, null, null, null));

        assertThat(existing.getOdometer()).isEqualTo(15000);
        assertThat(vehicle.getOdometer()).isEqualTo(50000);
    }
}
