package com.odolog.app.garage.service;

import com.odolog.app.fuel.FuelRecordService;
import com.odolog.app.maintenance.MaintenanceRecordService;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.vehicle.Vehicle;
import com.odolog.app.vehicle.VehicleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleRemovalServiceTest {

    @Mock
    private VehicleService vehicleService;

    @Mock
    private MaintenanceRecordService maintenanceRecordService;

    @Mock
    private FuelRecordService fuelRecordService;

    @InjectMocks
    private VehicleRemovalService vehicleRemovalService;

    private Vehicle createVehicle(Long id) {
        Vehicle vehicle = new Vehicle(new User("owner@odolog.com", "encoded", "닉네임"),
                "12가3456", "현대", "아반떼", 2023);
        ReflectionTestUtils.setField(vehicle, "id", id);
        return vehicle;
    }

    @Test
    @DisplayName("차량 삭제 시 정비·주유 기록을 먼저 지운 뒤 차량을 지운다")
    void deleteRemovesRecordsBeforeVehicle() {
        Vehicle vehicle = createVehicle(10L);
        when(vehicleService.findOwnedVehicleForUpdate(1L, "V10")).thenReturn(vehicle);

        vehicleRemovalService.delete(1L, "V10");

        // 자식(정비·주유) 먼저, 차량 마지막
        InOrder order = inOrder(maintenanceRecordService, fuelRecordService, vehicleService);
        order.verify(maintenanceRecordService).deleteAllOf(10L);
        order.verify(fuelRecordService).deleteAllOf(10L);
        order.verify(vehicleService).remove(List.of(vehicle));
    }

    @Test
    @DisplayName("소유 차량 일괄 삭제는 차량마다 기록을 먼저 지운 뒤 차량을 한 번에 지운다")
    void deleteAllOwnedByRemovesRecordsFirst() {
        Vehicle first = createVehicle(10L);
        Vehicle second = createVehicle(11L);
        when(vehicleService.findAllOwnedByForUpdate(1L)).thenReturn(List.of(first, second));

        vehicleRemovalService.deleteAllOwnedBy(1L);

        InOrder order = inOrder(maintenanceRecordService, fuelRecordService, vehicleService);
        order.verify(maintenanceRecordService).deleteAllOf(10L);
        order.verify(fuelRecordService).deleteAllOf(10L);
        order.verify(maintenanceRecordService).deleteAllOf(11L);
        order.verify(fuelRecordService).deleteAllOf(11L);
        order.verify(vehicleService).remove(List.of(first, second));
    }
}
