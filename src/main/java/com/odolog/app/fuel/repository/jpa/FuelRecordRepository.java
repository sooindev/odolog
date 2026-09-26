package com.odolog.app.fuel.repository.jpa;

import com.odolog.app.fuel.domain.entity.FuelRecord;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FuelRecordRepository extends JpaRepository<FuelRecord, Long> {

    Page<FuelRecord> findByVehicleId(Long vehicleId, Pageable pageable);

    /** 타 차량 소속 기록 접근 차단 (404) */
    Optional<FuelRecord> findByPublicIdAndVehicleId(String publicId, Long vehicleId);

    /**
     * (주행거리, id) 순서에서 바로 앞 = 직전 주유. 목록·연비 계산의 정렬과 같은 기준
     * 주행거리만 보면 같은 값 2건이 페이지 경계에 걸릴 때 둘 다 더 앞 기록을 짝으로 잡는다
     */
    @Query("""
            select f from FuelRecord f
            where f.vehicle.id = :vehicleId
              and (f.odometer < :odometer or (f.odometer = :odometer and f.id < :id))
            order by f.odometer desc, f.id desc
            """)
    List<FuelRecord> findPreceding(@Param("vehicleId") Long vehicleId, @Param("odometer") int odometer,
                                   @Param("id") Long id, Limit limit);

    default Optional<FuelRecord> findPrevious(Long vehicleId, int odometer, Long id) {
        return findPreceding(vehicleId, odometer, id, Limit.of(1)).stream().findFirst();
    }

    /** 요약용 전체 조회. 페이지를 나누면 첫 기록과 마지막 기록이 못 만남 */
    List<FuelRecord> findAllByVehicleIdOrderByOdometerAscIdAsc(Long vehicleId);

    /** 한 사용자의 전체 주유 기록. 오름차순이라 차량별 연비 계산에 그대로 사용 */
    List<FuelRecord> findByVehicle_Owner_IdOrderByOdometerAscIdAsc(Long ownerId);

    void deleteByVehicleId(Long vehicleId);
}
