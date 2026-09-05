package com.cartree.app.repository;

import com.cartree.app.domain.MaintenanceRecord;
import com.cartree.app.domain.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findByVehicleIdOrderByServiceDateDesc(Long vehicleId);

    Optional<MaintenanceRecord> findTopByVehicleIdAndTypeOrderByServiceDateDesc(Long vehicleId, ServiceType type);
}
