package com.cartree.app.service;

import com.cartree.app.domain.User;
import com.cartree.app.domain.Vehicle;
import com.cartree.app.dto.UpdateOdometerRequest;
import com.cartree.app.dto.VehicleRegisterRequest;
import com.cartree.app.exception.ForbiddenAccessException;
import com.cartree.app.exception.ResourceNotFoundException;
import com.cartree.app.repository.UserRepository;
import com.cartree.app.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Vehicle register(Long ownerId, VehicleRegisterRequest request) {
        if (vehicleRepository.existsByPlateNumber(request.plateNumber())) {
            throw new IllegalArgumentException("이미 등록된 차량 번호입니다: " + request.plateNumber());
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 사용자입니다: " + ownerId));

        Vehicle vehicle = new Vehicle(owner, request.plateNumber(), request.manufacturer(),
                request.modelName(), request.modelYear());

        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> findMyVehicles(Long ownerId) {
        return vehicleRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Transactional
    public Vehicle updateOdometer(Long requesterId, Long vehicleId, UpdateOdometerRequest request) {
        Vehicle vehicle = findOwnedVehicle(requesterId, vehicleId);
        vehicle.updateOdometer(request.odometer());

        return vehicle;
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
