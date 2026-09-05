package com.cartree.app.service;

import com.cartree.app.domain.MaintenanceRecord;
import com.cartree.app.domain.ServiceType;
import com.cartree.app.domain.Vehicle;
import com.cartree.app.dto.MaintenanceRecordRegisterRequest;
import com.cartree.app.dto.NextServiceResponse;
import com.cartree.app.exception.ForbiddenAccessException;
import com.cartree.app.exception.ResourceNotFoundException;
import com.cartree.app.repository.MaintenanceRecordRepository;
import com.cartree.app.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MaintenanceRecordService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final VehicleRepository vehicleRepository;

    public MaintenanceRecordService(MaintenanceRecordRepository maintenanceRecordRepository,
                                     VehicleRepository vehicleRepository) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public MaintenanceRecord register(Long requesterId, Long vehicleId, MaintenanceRecordRegisterRequest request) {
        Vehicle vehicle = findOwnedVehicle(requesterId, vehicleId);

        MaintenanceRecord record = new MaintenanceRecord(vehicle, request.type(), request.description(),
                request.cost(), request.serviceOdometer(), request.serviceDate());

        return maintenanceRecordRepository.save(record);
    }

    public List<MaintenanceRecord> findByVehicle(Long requesterId, Long vehicleId) {
        findOwnedVehicle(requesterId, vehicleId);
        return maintenanceRecordRepository.findByVehicleIdOrderByServiceDateDesc(vehicleId);
    }

    public NextServiceResponse calculateNextService(Long requesterId, Long vehicleId, ServiceType type) {
        findOwnedVehicle(requesterId, vehicleId);

        return maintenanceRecordRepository.findTopByVehicleIdAndTypeOrderByServiceDateDesc(vehicleId, type)
                .map(record -> {
                    Integer interval = type.getRecommendedIntervalKm();
                    Integer nextOdometer = (interval == null) ? null : record.getServiceOdometer() + interval;
                    return new NextServiceResponse(type, record.getServiceOdometer(), nextOdometer);
                })
                .orElse(new NextServiceResponse(type, null, null));
    }

    private Vehicle findOwnedVehicle(Long requesterId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 차량입니다: " + vehicleId));

        if (!vehicle.getOwner().getId().equals(requesterId)) {
            throw new ForbiddenAccessException("본인 소유의 차량만 접근할 수 있습니다.");
        }

        return vehicle;
    }
}
