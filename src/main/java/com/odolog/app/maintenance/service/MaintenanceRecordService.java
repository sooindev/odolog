package com.odolog.app.maintenance.service;

import com.odolog.app.maintenance.domain.MaintenanceRecord;
import com.odolog.app.maintenance.domain.ServiceType;
import com.odolog.app.vehicle.domain.Vehicle;
import com.odolog.app.maintenance.dto.request.MaintenanceRecordRegisterRequest;
import com.odolog.app.maintenance.dto.request.MaintenanceRecordUpdateRequest;
import com.odolog.app.maintenance.dto.response.NextServiceResponse;
import com.odolog.app.maintenance.repository.MaintenanceRecordRepository;
import com.odolog.app.common.exception.ResourceNotFoundException;
import com.odolog.app.vehicle.service.VehicleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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

    public Page<MaintenanceRecord> findByVehicle(Long requesterId, Long vehicleId, Pageable pageable) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);
        return maintenanceRecordRepository.findByVehicleId(vehicleId, pageable);
    }

    public NextServiceResponse calculateNextService(Long requesterId, Long vehicleId, ServiceType type) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);

        return maintenanceRecordRepository.findTopByVehicleIdAndTypeOrderByServiceDateDesc(vehicleId, type)
                .map(record -> {
                    Integer intervalKm = type.getRecommendedIntervalKm();
                    Integer nextOdometer = (intervalKm == null) ? null : record.getServiceOdometer() + intervalKm;

                    Integer intervalMonths = type.getRecommendedIntervalMonths();
                    LocalDate nextDate = (intervalMonths == null) ? null
                            : record.getServiceDate().plusMonths(intervalMonths);

                    return new NextServiceResponse(type, record.getServiceOdometer(), nextOdometer,
                            record.getServiceDate(), nextDate);
                })
                .orElse(new NextServiceResponse(type, null, null, null, null));
    }

    public MaintenanceRecord findOne(Long requesterId, Long vehicleId, Long recordId) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);
        return findRecordInVehicle(vehicleId, recordId);
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
