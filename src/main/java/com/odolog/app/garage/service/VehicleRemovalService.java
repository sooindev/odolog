package com.odolog.app.garage.service;

import com.odolog.app.fuel.FuelRecordService;
import com.odolog.app.maintenance.MaintenanceRecordService;
import com.odolog.app.vehicle.Vehicle;
import com.odolog.app.vehicle.VehicleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 차량 삭제. vehicle·maintenance·fuel 에 걸친 조율 층
 * 순서만 결정, 실제 삭제는 각 기능 담당
 */
@Service
@Transactional(readOnly = true)
public class VehicleRemovalService {

    private final VehicleService vehicleService;
    private final MaintenanceRecordService maintenanceRecordService;
    private final FuelRecordService fuelRecordService;

    public VehicleRemovalService(VehicleService vehicleService,
                                 MaintenanceRecordService maintenanceRecordService,
                                 FuelRecordService fuelRecordService) {
        this.vehicleService = vehicleService;
        this.maintenanceRecordService = maintenanceRecordService;
        this.fuelRecordService = fuelRecordService;
    }

    @Transactional
    public void delete(Long requesterId, String vehicleId) {
        removeWithRecords(List.of(vehicleService.findOwnedVehicle(requesterId, vehicleId)));
    }

    /** 한 사용자의 차량 일괄 삭제. 회원 탈퇴 전용 */
    @Transactional
    public void deleteAllOwnedBy(Long ownerId) {
        removeWithRecords(vehicleService.findAllOwnedBy(ownerId));
    }

    // 자식 먼저, 차량 나중. FK 제약
    private void removeWithRecords(List<Vehicle> vehicles) {
        for (Vehicle vehicle : vehicles) {
            maintenanceRecordService.deleteAllOf(vehicle.getId());
            fuelRecordService.deleteAllOf(vehicle.getId());
        }
        vehicleService.remove(vehicles);
    }
}
