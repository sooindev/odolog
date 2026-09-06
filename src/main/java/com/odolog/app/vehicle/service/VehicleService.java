package com.odolog.app.vehicle.service;

import com.odolog.app.common.exception.ConflictException;
import com.odolog.app.user.domain.User;
import com.odolog.app.vehicle.domain.Vehicle;
import com.odolog.app.vehicle.dto.request.UpdateOdometerRequest;
import com.odolog.app.vehicle.dto.request.VehicleRegisterRequest;
import com.odolog.app.common.exception.ForbiddenAccessException;
import com.odolog.app.common.exception.ResourceNotFoundException;
import com.odolog.app.maintenance.repository.MaintenanceRecordRepository;
import com.odolog.app.user.repository.UserRepository;
import com.odolog.app.vehicle.repository.VehicleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository,
                           MaintenanceRecordRepository maintenanceRecordRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    @Transactional
    public Vehicle register(Long ownerId, VehicleRegisterRequest request) {
        if (vehicleRepository.existsByOwnerIdAndPlateNumber(ownerId, request.plateNumber())) {
            throw new ConflictException("이미 등록하신 차량 번호입니다: " + request.plateNumber());
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 사용자입니다: " + ownerId));

        Vehicle vehicle = new Vehicle(owner, request.plateNumber(), request.manufacturer(),
                request.modelName(), request.modelYear());

        return vehicleRepository.save(vehicle);
    }

    public Page<Vehicle> findMyVehicles(Long ownerId, Pageable pageable) {
        return vehicleRepository.findByOwnerId(ownerId, pageable);
    }

    @Transactional
    public Vehicle updateOdometer(Long requesterId, Long vehicleId, UpdateOdometerRequest request) {
        Vehicle vehicle = findOwnedVehicle(requesterId, vehicleId);
        vehicle.updateOdometer(request.odometer());

        return vehicle;
    }

    @Transactional
    public void delete(Long requesterId, Long vehicleId) {
        Vehicle vehicle = findOwnedVehicle(requesterId, vehicleId);
        maintenanceRecordRepository.deleteByVehicleId(vehicle.getId());
        vehicleRepository.delete(vehicle);
    }

    public Vehicle findOwnedVehicle(Long requesterId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 차량입니다: " + vehicleId));

        if (!vehicle.getOwner().getId().equals(requesterId)) {
            throw new ForbiddenAccessException("본인 소유의 차량만 접근할 수 있습니다.");
        }

        return vehicle;
    }
}
