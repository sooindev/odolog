package com.odolog.app.maintenance.repository;

import com.odolog.app.maintenance.domain.MaintenanceRecord;
import com.odolog.app.maintenance.domain.ServiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    Page<MaintenanceRecord> findByVehicleId(Long vehicleId, Pageable pageable);

    Optional<MaintenanceRecord> findTopByVehicleIdAndTypeOrderByServiceDateDescIdDesc(Long vehicleId, ServiceType type);

    Optional<MaintenanceRecord> findByIdAndVehicleId(Long id, Long vehicleId);

    void deleteByVehicleId(Long vehicleId);
}
