package com.odolog.app.maintenance.repository.jpa;

import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.type.ServiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    Page<MaintenanceRecord> findByVehicleId(Long vehicleId, Pageable pageable);

    /** 종류별 목록 */
    Page<MaintenanceRecord> findByVehicleIdAndType(Long vehicleId, ServiceType type, Pageable pageable);


    Optional<MaintenanceRecord> findByPublicIdAndVehicleId(String publicId, Long vehicleId);

    /** 종류별 최신 1건용 전체 조회. 종류마다 조회 시 15쿼리 */
    List<MaintenanceRecord> findByVehicleIdOrderByServiceDateDescIdDesc(Long vehicleId);

    /** 한 사용자의 전체 정비 이력. 홈 요약 전용 */
    List<MaintenanceRecord> findByVehicle_Owner_IdOrderByServiceDateDescIdDesc(Long ownerId);

    void deleteByVehicleId(Long vehicleId);
}
