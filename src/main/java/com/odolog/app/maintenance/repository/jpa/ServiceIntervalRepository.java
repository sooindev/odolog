package com.odolog.app.maintenance.repository.jpa;

import com.odolog.app.maintenance.domain.entity.ServiceInterval;
import com.odolog.app.maintenance.domain.type.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceIntervalRepository extends JpaRepository<ServiceInterval, Long> {

    List<ServiceInterval> findByVehicleId(Long vehicleId);

    Optional<ServiceInterval> findByVehicleIdAndType(Long vehicleId, ServiceType type);

    /** 한 사용자의 전부. 홈 요약이 차량마다 조회하지 않게 */
    List<ServiceInterval> findByVehicle_Owner_Id(Long ownerId);

    /** 차량 삭제 때 같이. FK 제약이라 차량보다 먼저 지워야 한다 */
    void deleteByVehicleId(Long vehicleId);
}
