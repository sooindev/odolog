package com.cartree.app.vehicle.repository;

import com.cartree.app.user.domain.User;
import com.cartree.app.vehicle.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByOwner(User owner);

    List<Vehicle> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<Vehicle> findByPlateNumber(String plateNumber);

    boolean existsByPlateNumber(String plateNumber);
}
