package com.cartree.app.maintenance.service;

import com.cartree.app.common.exception.ResourceNotFoundException;
import com.cartree.app.maintenance.domain.MaintenanceRecord;
import com.cartree.app.maintenance.domain.ServiceType;
import com.cartree.app.maintenance.dto.MaintenanceRecordRegisterRequest;
import com.cartree.app.maintenance.dto.MaintenanceRecordUpdateRequest;
import com.cartree.app.maintenance.dto.NextServiceResponse;
import com.cartree.app.maintenance.repository.MaintenanceRecordRepository;
import com.cartree.app.vehicle.domain.Vehicle;
import com.cartree.app.vehicle.service.VehicleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
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
        when(maintenanceRecordRepository.findTopByVehicleIdAndTypeOrderByServiceDateDesc(10L, ServiceType.ENGINE_OIL))
                .thenReturn(Optional.of(lastRecord));

        NextServiceResponse response = maintenanceRecordService.calculateNextService(1L, 10L, ServiceType.ENGINE_OIL);

        assertThat(response.lastServiceOdometer()).isEqualTo(40000);
        assertThat(response.nextServiceOdometer()).isEqualTo(45000);
    }

    @Test
    @DisplayName("권장 주기가 없는 종류(OTHER)는 다음 정비 시점을 계산하지 않는다")
    void calculateNextServiceWithoutInterval() {
        Vehicle vehicle = createVehicle(10L);
        MaintenanceRecord lastRecord = new MaintenanceRecord(vehicle, ServiceType.OTHER, null,
                10000, 40000, LocalDate.of(2026, 1, 1));
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(maintenanceRecordRepository.findTopByVehicleIdAndTypeOrderByServiceDateDesc(10L, ServiceType.OTHER))
                .thenReturn(Optional.of(lastRecord));

        NextServiceResponse response = maintenanceRecordService.calculateNextService(1L, 10L, ServiceType.OTHER);

        assertThat(response.lastServiceOdometer()).isEqualTo(40000);
        assertThat(response.nextServiceOdometer()).isNull();
    }

    @Test
    @DisplayName("이력이 아예 없으면 둘 다 null이다")
    void calculateNextServiceWithoutHistory() {
        Vehicle vehicle = createVehicle(10L);
        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);
        when(maintenanceRecordRepository.findTopByVehicleIdAndTypeOrderByServiceDateDesc(10L, ServiceType.TIRE))
                .thenReturn(Optional.empty());

        NextServiceResponse response = maintenanceRecordService.calculateNextService(1L, 10L, ServiceType.TIRE);

        assertThat(response.lastServiceOdometer()).isNull();
        assertThat(response.nextServiceOdometer()).isNull();
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
}
