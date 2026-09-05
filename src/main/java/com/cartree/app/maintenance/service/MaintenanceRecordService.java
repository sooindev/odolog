package com.cartree.app.maintenance.service;

import com.cartree.app.maintenance.domain.MaintenanceRecord;
import com.cartree.app.maintenance.domain.ServiceType;
import com.cartree.app.vehicle.domain.Vehicle;
import com.cartree.app.maintenance.dto.MaintenanceRecordRegisterRequest;
import com.cartree.app.maintenance.dto.NextServiceResponse;
import com.cartree.app.maintenance.repository.MaintenanceRecordRepository;
import com.cartree.app.vehicle.service.VehicleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MaintenanceRecordService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final VehicleService vehicleService;

    public MaintenanceRecordService(MaintenanceRecordRepository maintenanceRecordRepository,
                                     VehicleService vehicleService) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.vehicleService = vehicleService;
    }

    @Transactional
    public MaintenanceRecord register(Long requesterId, Long vehicleId, MaintenanceRecordRegisterRequest request) {
        Vehicle vehicle = vehicleService.findOwnedVehicle(requesterId, vehicleId);

        MaintenanceRecord record = new MaintenanceRecord(vehicle, request.type(), request.description(),
                request.cost(), request.serviceOdometer(), request.serviceDate());

        return maintenanceRecordRepository.save(record);
    }

    public List<MaintenanceRecord> findByVehicle(Long requesterId, Long vehicleId) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);
        return maintenanceRecordRepository.findByVehicleIdOrderByServiceDateDesc(vehicleId);
    }

    public NextServiceResponse calculateNextService(Long requesterId, Long vehicleId, ServiceType type) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);

        return maintenanceRecordRepository.findTopByVehicleIdAndTypeOrderByServiceDateDesc(vehicleId, type)
                .map(record -> {
                    Integer interval = type.getRecommendedIntervalKm();
                    Integer nextOdometer = (interval == null) ? null : record.getServiceOdometer() + interval;
                    return new NextServiceResponse(type, record.getServiceOdometer(), nextOdometer);
                })
                .orElse(new NextServiceResponse(type, null, null));
    }
}
