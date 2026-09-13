package com.odolog.app.vehicle.repository.jpa;

import com.odolog.app.vehicle.domain.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Page<Vehicle> findByOwnerId(Long ownerId, Pageable pageable);

    boolean existsByOwnerIdAndPlateNumber(Long ownerId, String plateNumber);
}
