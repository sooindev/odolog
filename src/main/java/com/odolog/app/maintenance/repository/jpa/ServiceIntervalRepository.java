package com.odolog.app.maintenance.repository.jpa;

import com.odolog.app.maintenance.domain.entity.ServiceInterval;
import com.odolog.app.maintenance.domain.type.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceIntervalRepository extends JpaRepository<ServiceInterval, Long> {

    List<ServiceInterval> findByVehicleId(Long vehicleId);

    Optional<ServiceInterval> findByVehicleIdAndType(Long vehicleId, ServiceType type);

    /** 한 사용자의 전체 주기. 차량별 조회 방지 */
    List<ServiceInterval> findByVehicle_Owner_Id(Long ownerId);

    /** 차량 삭제 시 함께. FK 제약이라 차량보다 먼저 */
    void deleteByVehicleId(Long vehicleId);
}
