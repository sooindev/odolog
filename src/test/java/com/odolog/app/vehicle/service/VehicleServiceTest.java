package com.odolog.app.vehicle.service;

import com.odolog.app.common.exception.ForbiddenAccessException;
import com.odolog.app.common.exception.ResourceNotFoundException;
import com.odolog.app.maintenance.repository.MaintenanceRecordRepository;
import com.odolog.app.user.domain.User;
import com.odolog.app.user.repository.UserRepository;
import com.odolog.app.vehicle.domain.Vehicle;
import com.odolog.app.vehicle.dto.UpdateOdometerRequest;
import com.odolog.app.vehicle.dto.VehicleRegisterRequest;
import com.odolog.app.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private User createOwner(Long id) {
        User owner = new User("owner@odolog.com", "encoded", "닉네임", "010-0000-0000");
        ReflectionTestUtils.setField(owner, "id", id);
        return owner;
    }

    private Vehicle createVehicle(Long id, User owner) {
        Vehicle vehicle = new Vehicle(owner, "12가3456", "현대", "아반떼", 2023);
        ReflectionTestUtils.setField(vehicle, "id", id);
        return vehicle;
    }

    @Test
    @DisplayName("이미 등록된 번호판이면 예외가 발생하고 저장하지 않는다")
    void registerDuplicatePlateNumber() {
        VehicleRegisterRequest request = new VehicleRegisterRequest("12가3456", "현대", "아반떼", 2023);
        when(vehicleRepository.existsByPlateNumber("12가3456")).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.register(1L, request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("차량 등록 성공")
    void registerSuccess() {
        User owner = createOwner(1L);
        VehicleRegisterRequest request = new VehicleRegisterRequest("12가3456", "현대", "아반떼", 2023);
        when(vehicleRepository.existsByPlateNumber("12가3456")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vehicle saved = vehicleService.register(1L, request);

        assertThat(saved.getOwner()).isEqualTo(owner);
        assertThat(saved.getPlateNumber()).isEqualTo("12가3456");
    }

    @Test
    @DisplayName("존재하지 않는 차량에 접근하면 ResourceNotFoundException")
    void findOwnedVehicleNotFound() {
        when(vehicleRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.findOwnedVehicle(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("본인 소유가 아닌 차량에 접근하면 ForbiddenAccessException")
    void findOwnedVehicleForbidden() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.findOwnedVehicle(999L, 10L))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    @DisplayName("주행거리가 감소하면 예외가 발생한다")
    void updateOdometerDecreaseFails() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        vehicle.updateOdometer(50000);
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.updateOdometer(1L, 10L, new UpdateOdometerRequest(40000)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("차량 삭제 시 정비 이력을 먼저 지운 뒤 차량을 지운다")
    void deleteRemovesMaintenanceRecordsBeforeVehicle() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));

        vehicleService.delete(1L, 10L);

        InOrder order = inOrder(maintenanceRecordRepository, vehicleRepository);
        order.verify(maintenanceRecordRepository).deleteByVehicleId(10L);
        order.verify(vehicleRepository).delete(vehicle);
    }
}
