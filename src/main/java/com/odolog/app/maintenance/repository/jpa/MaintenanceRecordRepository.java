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


    Optional<MaintenanceRecord> findByIdAndVehicleId(Long id, Long vehicleId);

    /**
     * 종류별 최신 1건을 한 번에 구하려고 전체를 정렬해서 가져온다.
     *
     * <p>종류마다 findTopBy... 를 부르면 종류 수만큼(지금 15번) 쿼리가 나간다.
     * 한 번 읽어서 자바에서 종류별 첫 줄만 집는 편이 싸다 — 한 차량의 이력은
     * 많아야 수백 건이라 메모리에 올려도 무방하다.
     */
    List<MaintenanceRecord> findByVehicleIdOrderByServiceDateDescIdDesc(Long vehicleId);

    /**
     * 한 사용자의 모든 정비 이력. 홈 요약에서만 쓴다.
     *
     * <p>{@code Vehicle_Owner_Id} 는 vehicle → owner → id 를 타고 들어가라는 뜻이다.
     * 차량마다 따로 조회하면 차량 수만큼 쿼리가 나가는데, 요약은 어차피 전부 더할 것이라
     * 한 번에 읽는 편이 싸다.
     */
    List<MaintenanceRecord> findByVehicle_Owner_IdOrderByServiceDateDescIdDesc(Long ownerId);

    void deleteByVehicleId(Long vehicleId);
}
