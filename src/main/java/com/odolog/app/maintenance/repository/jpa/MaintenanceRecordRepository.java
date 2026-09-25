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

    /** 종류로 좁힌 목록. 이력이 쌓이면 페이지를 넘겨 가며 찾게 된다 */
    Page<MaintenanceRecord> findByVehicleIdAndType(Long vehicleId, ServiceType type, Pageable pageable);


    Optional<MaintenanceRecord> findByIdAndVehicleId(Long id, Long vehicleId);

    /**
     * 종류별 최신 1건용. 전체를 정렬해 한 번에 읽기
     * 종류마다 findTopBy 면 15쿼리. 한 차량의 이력은 많아야 수백 건이라 메모리가 쌈
     */
    List<MaintenanceRecord> findByVehicleIdOrderByServiceDateDescIdDesc(Long vehicleId);

    /**
     * 한 사용자의 전체 정비 이력. 홈 요약 전용
     * Vehicle_Owner_Id = vehicle → owner → id 로 타고 들어가는 파생 쿼리
     */
    List<MaintenanceRecord> findByVehicle_Owner_IdOrderByServiceDateDescIdDesc(Long ownerId);

    void deleteByVehicleId(Long vehicleId);
}
