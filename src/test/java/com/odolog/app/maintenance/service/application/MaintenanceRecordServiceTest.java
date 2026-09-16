package com.odolog.app.maintenance.service.application;

import com.odolog.app.common.exception.type.ResourceNotFoundException;
import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.type.ServiceType;
import com.odolog.app.maintenance.dto.request.register.MaintenanceRecordRegisterRequest;
import com.odolog.app.maintenance.dto.request.update.MaintenanceRecordUpdateRequest;
import com.odolog.app.maintenance.dto.response.schedule.NextServiceResponse;
import com.odolog.app.maintenance.repository.jpa.MaintenanceRecordRepository;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.vehicle.service.application.VehicleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceRecordServiceTest {

    @Mock
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Mock
    private VehicleService vehicleService;

    @InjectMocks
    private MaintenanceRecordService maintenanceRecordService;

    private Vehicle createVehicle(Long id) {
        Vehicle vehicle = new Vehicle(null, "12가3456", "현대", "아반떼", 2023);
        ReflectionTestUtils.setField(vehicle, "id", id);
        return vehicle;
    }

    @Test
    @DisplayName("정비 이력 등록 성공")
    void registerSuccess() {
        Vehicle vehicle = createVehicle(10L);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(maintenanceRecordRepository.save(any(MaintenanceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MaintenanceRecordRegisterRequest request = new MaintenanceRecordRegisterRequest(
                ServiceType.ENGINE_OIL, "정기 교체", 50000, 40000, LocalDate.of(2026, 1, 1));

        MaintenanceRecord saved = maintenanceRecordService.register(1L, 10L, request);

        assertThat(saved.getVehicle()).isEqualTo(vehicle);
        assertThat(saved.getType()).isEqualTo(ServiceType.ENGINE_OIL);
    }

    @Test
    @DisplayName("최근 이력이 있으면 권장 주기를 더해 다음 정비 시점을 계산한다")
    void calculateNextServiceWithHistory() {
        Vehicle vehicle = createVehicle(10L);
        MaintenanceRecord lastRecord = new MaintenanceRecord(vehicle, ServiceType.ENGINE_OIL, null,
                50000, 40000, LocalDate.of(2026, 1, 1));
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(maintenanceRecordRepository.findTopByVehicleIdAndTypeOrderByServiceDateDescIdDesc(10L, ServiceType.ENGINE_OIL))
                .thenReturn(Optional.of(lastRecord));

        NextServiceResponse response = maintenanceRecordService.calculateNextService(1L, 10L, ServiceType.ENGINE_OIL);

        assertThat(response.lastServiceOdometer()).isEqualTo(40000);
        assertThat(response.nextServiceOdometer()).isEqualTo(45000);
        assertThat(response.lastServiceDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(response.nextServiceDate()).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @Test
    @DisplayName("권장 주기가 없는 종류(OTHER)는 다음 정비 시점을 계산하지 않는다")
    void calculateNextServiceWithoutInterval() {
        Vehicle vehicle = createVehicle(10L);
        MaintenanceRecord lastRecord = new MaintenanceRecord(vehicle, ServiceType.OTHER, null,
                10000, 40000, LocalDate.of(2026, 1, 1));
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(maintenanceRecordRepository.findTopByVehicleIdAndTypeOrderByServiceDateDescIdDesc(10L, ServiceType.OTHER))
                .thenReturn(Optional.of(lastRecord));

        NextServiceResponse response = maintenanceRecordService.calculateNextService(1L, 10L, ServiceType.OTHER);

        assertThat(response.lastServiceOdometer()).isEqualTo(40000);
        assertThat(response.nextServiceOdometer()).isNull();
        assertThat(response.nextServiceDate()).isNull();
    }

    @Test
    @DisplayName("이력이 아예 없으면 둘 다 null이다")
    void calculateNextServiceWithoutHistory() {
        Vehicle vehicle = createVehicle(10L);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(maintenanceRecordRepository.findTopByVehicleIdAndTypeOrderByServiceDateDescIdDesc(10L, ServiceType.TIRE))
                .thenReturn(Optional.empty());

        NextServiceResponse response = maintenanceRecordService.calculateNextService(1L, 10L, ServiceType.TIRE);

        assertThat(response.lastServiceOdometer()).isNull();
        assertThat(response.nextServiceOdometer()).isNull();
        assertThat(response.lastServiceDate()).isNull();
        assertThat(response.nextServiceDate()).isNull();
    }

    @Test
    @DisplayName("정비 이력 단건 조회 성공")
    void findOneSuccess() {
        Vehicle vehicle = createVehicle(10L);
        MaintenanceRecord record = new MaintenanceRecord(vehicle, ServiceType.ENGINE_OIL, "정기 교체",
                50000, 40000, LocalDate.of(2026, 1, 1));
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(maintenanceRecordRepository.findByIdAndVehicleId(100L, 10L)).thenReturn(Optional.of(record));

        MaintenanceRecord found = maintenanceRecordService.findOne(1L, 10L, 100L);

        assertThat(found).isEqualTo(record);
    }

    @Test
    @DisplayName("수정 요청에 보낸 필드만 반영된다")
    void updatePartialFields() {
        Vehicle vehicle = createVehicle(10L);
        MaintenanceRecord record = new MaintenanceRecord(vehicle, ServiceType.ENGINE_OIL, "기존 메모",
                50000, 40000, LocalDate.of(2026, 1, 1));
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(maintenanceRecordRepository.findByIdAndVehicleId(100L, 10L)).thenReturn(Optional.of(record));

        MaintenanceRecordUpdateRequest request = new MaintenanceRecordUpdateRequest(
                null, "수정된 메모", null, null, null);

        MaintenanceRecord updated = maintenanceRecordService.update(1L, 10L, 100L, request);

        assertThat(updated.getDescription()).isEqualTo("수정된 메모");
        assertThat(updated.getCost()).isEqualTo(50000);
        assertThat(updated.getType()).isEqualTo(ServiceType.ENGINE_OIL);
    }

    @Test
    @DisplayName("다른 차량 소속의 정비 이력 id로 접근하면 ResourceNotFoundException")
    void updateRecordNotBelongingToVehicle() {
        Vehicle vehicle = createVehicle(10L);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(maintenanceRecordRepository.findByIdAndVehicleId(999L, 10L)).thenReturn(Optional.empty());

        MaintenanceRecordUpdateRequest request = new MaintenanceRecordUpdateRequest(
                null, "수정된 메모", null, null, null);

        assertThatThrownBy(() -> maintenanceRecordService.update(1L, 10L, 999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private MaintenanceRecord record(Long id, Vehicle vehicle, ServiceType type,
                                     int odometer, LocalDate date) {
        MaintenanceRecord record = new MaintenanceRecord(vehicle, type, null, 0, odometer, date);
        ReflectionTestUtils.setField(record, "id", id);
        return record;
    }

    @Test
    @DisplayName("주기가 개월만 있는 종류(와이퍼)는 날짜만 계산된다")
    void nextServiceWithMonthsOnly() {
        Vehicle vehicle = createVehicle(10L);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(maintenanceRecordRepository.findTopByVehicleIdAndTypeOrderByServiceDateDescIdDesc(
                10L, ServiceType.WIPER))
                .thenReturn(Optional.of(record(1L, vehicle, ServiceType.WIPER, 30000,
                        LocalDate.of(2026, 3, 10))));

        NextServiceResponse response =
                maintenanceRecordService.calculateNextService(1L, 10L, ServiceType.WIPER);

        assertThat(response.nextServiceOdometer()).isNull();
        assertThat(response.nextServiceDate()).isEqualTo(LocalDate.of(2027, 3, 10));
    }

    @Test
    @DisplayName("주기가 주행거리만 있는 종류(타이밍 벨트)는 거리만 계산된다")
    void nextServiceWithKmOnly() {
        Vehicle vehicle = createVehicle(10L);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(maintenanceRecordRepository.findTopByVehicleIdAndTypeOrderByServiceDateDescIdDesc(
                10L, ServiceType.TIMING_BELT))
                .thenReturn(Optional.of(record(1L, vehicle, ServiceType.TIMING_BELT, 90000,
                        LocalDate.of(2026, 3, 10))));

        NextServiceResponse response =
                maintenanceRecordService.calculateNextService(1L, 10L, ServiceType.TIMING_BELT);

        assertThat(response.nextServiceOdometer()).isEqualTo(190000);
        assertThat(response.nextServiceDate()).isNull();
    }

    @Test
    @DisplayName("전체 조회는 이력이 있는 종류만, 종류별 최신 1건으로 돌려준다")
    void calculateAllNextServices() {
        Vehicle vehicle = createVehicle(10L);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        // 정렬된 목록이라 같은 종류는 앞에 있는 것이 최신이다.
        when(maintenanceRecordRepository.findByVehicleIdOrderByServiceDateDescIdDesc(10L))
                .thenReturn(List.of(
                        record(3L, vehicle, ServiceType.ENGINE_OIL, 20000, LocalDate.of(2026, 9, 1)),
                        record(2L, vehicle, ServiceType.TRANSMISSION_FLUID, 15000, LocalDate.of(2026, 6, 1)),
                        record(1L, vehicle, ServiceType.ENGINE_OIL, 10000, LocalDate.of(2026, 1, 1))));

        List<NextServiceResponse> responses =
                maintenanceRecordService.calculateAllNextServices(1L, 10L);

        // 15개 종류 중 이력이 있는 2개만. 없는 종류로 빈 줄을 채우지 않는다.
        assertThat(responses).hasSize(2);
        // enum 선언 순서대로 — ENGINE_OIL 이 TRANSMISSION_FLUID 보다 앞이다.
        assertThat(responses).extracting(NextServiceResponse::type)
                .containsExactly(ServiceType.ENGINE_OIL, ServiceType.TRANSMISSION_FLUID);
        // 엔진오일은 나중 것(20000)이 잡혀야 한다. 10000 이 잡히면 정렬 전제가 깨진 것이다.
        assertThat(responses.get(0).lastServiceOdometer()).isEqualTo(20000);
        assertThat(responses.get(0).nextServiceOdometer()).isEqualTo(25000);
    }

    @Test
    @DisplayName("이력이 하나도 없으면 전체 조회 결과가 빈 목록이다")
    void calculateAllNextServicesEmpty() {
        Vehicle vehicle = createVehicle(10L);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(maintenanceRecordRepository.findByVehicleIdOrderByServiceDateDescIdDesc(10L))
                .thenReturn(List.of());

        assertThat(maintenanceRecordService.calculateAllNextServices(1L, 10L)).isEmpty();
    }
}
