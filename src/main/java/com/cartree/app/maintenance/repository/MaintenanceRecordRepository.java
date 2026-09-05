package com.cartree.app.maintenance.repository;

import com.cartree.app.maintenance.domain.MaintenanceRecord;
import com.cartree.app.maintenance.domain.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findByVehicleIdOrderByServiceDateDesc(Long vehicleId);

    Optional<MaintenanceRecord> findTopByVehicleIdAndTypeOrderByServiceDateDesc(Long vehicleId, ServiceType type);

    Optional<MaintenanceRecord> findByIdAndVehicleId(Long id, Long vehicleId);

    void deleteByVehicleId(Long vehicleId);
}
