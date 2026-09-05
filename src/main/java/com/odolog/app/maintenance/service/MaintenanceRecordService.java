package com.odolog.app.maintenance.service;

import com.odolog.app.maintenance.domain.MaintenanceRecord;
import com.odolog.app.maintenance.domain.ServiceType;
import com.odolog.app.vehicle.domain.Vehicle;
import com.odolog.app.maintenance.dto.MaintenanceRecordRegisterRequest;
import com.odolog.app.maintenance.dto.MaintenanceRecordUpdateRequest;
import com.odolog.app.maintenance.dto.NextServiceResponse;
import com.odolog.app.maintenance.repository.MaintenanceRecordRepository;
import com.odolog.app.common.exception.ResourceNotFoundException;
import com.odolog.app.vehicle.service.VehicleService;
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

    @Transactional
    public MaintenanceRecord update(Long requesterId, Long vehicleId, Long recordId,
                                     MaintenanceRecordUpdateRequest request) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);
        MaintenanceRecord record = findRecordInVehicle(vehicleId, recordId);

        if (request.type() != null) {
            record.changeType(request.type());
        }
        if (request.description() != null) {
            record.changeDescription(request.description());
        }
        if (request.cost() != null) {
            record.changeCost(request.cost());
        }
        if (request.serviceOdometer() != null) {
            record.changeServiceOdometer(request.serviceOdometer());
        }
        if (request.serviceDate() != null) {
            record.changeServiceDate(request.serviceDate());
        }

        return record;
    }

    @Transactional
    public void delete(Long requesterId, Long vehicleId, Long recordId) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);
        MaintenanceRecord record = findRecordInVehicle(vehicleId, recordId);

        maintenanceRecordRepository.delete(record);
    }

    private MaintenanceRecord findRecordInVehicle(Long vehicleId, Long recordId) {
        return maintenanceRecordRepository.findByIdAndVehicleId(recordId, vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 정비 이력입니다: " + recordId));
    }
}
