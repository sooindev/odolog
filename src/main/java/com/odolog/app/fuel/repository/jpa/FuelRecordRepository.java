package com.odolog.app.fuel.repository.jpa;

import com.odolog.app.fuel.domain.entity.FuelRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FuelRecordRepository extends JpaRepository<FuelRecord, Long> {

    Page<FuelRecord> findByVehicleId(Long vehicleId, Pageable pageable);

    /** 타 차량 소속 기록 접근 차단 (404) */
    Optional<FuelRecord> findByIdAndVehicleId(Long id, Long vehicleId);

    /**
     * 이 값보다 작은 주행거리 중 최대 = 직전 주유
     * 동점(같은 주행거리 2건) 대비로 id 를 2차 기준에 둠
     */
    Optional<FuelRecord> findTopByVehicleIdAndOdometerLessThanOrderByOdometerDescIdDesc(
            Long vehicleId, int odometer);

    /** 요약용 전체 조회. 페이지를 나누면 첫 기록과 마지막 기록이 못 만남 */
    List<FuelRecord> findAllByVehicleIdOrderByOdometerAscIdAsc(Long vehicleId);

    /** 한 사용자의 전체 주유 기록. 오름차순이라 차량별 연비 계산에 그대로 사용 */
    List<FuelRecord> findByVehicle_Owner_IdOrderByOdometerAscIdAsc(Long ownerId);

    void deleteByVehicleId(Long vehicleId);
}
