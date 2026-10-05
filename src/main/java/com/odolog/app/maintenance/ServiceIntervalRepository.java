package com.odolog.app.maintenance;

import com.odolog.app.maintenance.domain.ServiceInterval;
import com.odolog.app.maintenance.domain.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceIntervalRepository extends JpaRepository<ServiceInterval, Long> {

    List<ServiceInterval> findByVehicleId(Long vehicleId);

    Optional<ServiceInterval> findByVehicleIdAndType(Long vehicleId, ServiceType type);

    /** 한 사용자의 전체 주기. 차량별 조회 방지 */
    List<ServiceInterval> findByVehicle_Owner_Id(Long ownerId);

    /**
     * 차량 삭제 시 함께. FK 제약이라 차량보다 먼저
     * 차량 하나의 행을 DELETE 한 문장으로. 메서드 이름만 쓰면 전부 읽은 뒤 한 줄씩 지움
     * 앞서 넣은 행이 먼저 반영되게 flush
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from ServiceInterval r where r.vehicle.id = :vehicleId")
    int deleteByVehicleId(@Param("vehicleId") Long vehicleId);
}
