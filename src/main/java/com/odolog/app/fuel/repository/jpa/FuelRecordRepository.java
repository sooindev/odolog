package com.odolog.app.fuel.repository.jpa;

import com.odolog.app.fuel.domain.entity.FuelRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FuelRecordRepository extends JpaRepository<FuelRecord, Long> {

    Page<FuelRecord> findByVehicleId(Long vehicleId, Pageable pageable);

    /** 다른 차량 소속 기록의 id 로 접근하는 것을 404 로 막는다. */
    Optional<FuelRecord> findByIdAndVehicleId(Long id, Long vehicleId);

    /**
     * 주행거리가 이 값보다 작은 것 중 가장 큰 것 = 바로 직전 주유.
     *
     * <p>연비는 "직전 주유 이후 달린 거리 ÷ 이번에 넣은 양"이라 직전 한 건만 있으면 된다.
     * 동점(같은 주행거리에 두 번 기록)일 때를 위해 id 를 2차 기준으로 둔다 —
     * 정비 이력의 findTopBy...OrderByServiceDateDescIdDesc 와 같은 이유다.
     */
    Optional<FuelRecord> findTopByVehicleIdAndOdometerLessThanOrderByOdometerDescIdDesc(
            Long vehicleId, int odometer);

    /** 요약(평균 연비)은 전체를 봐야 한다. 페이지를 나누면 첫 기록과 마지막 기록을 못 만난다. */
    List<FuelRecord> findAllByVehicleIdOrderByOdometerAscIdAsc(Long vehicleId);

    /** 한 사용자의 모든 주유 기록. 주행거리 오름차순이라 차량별 연비 계산에 그대로 쓴다. */
    List<FuelRecord> findByVehicle_Owner_IdOrderByOdometerAscIdAsc(Long ownerId);

    void deleteByVehicleId(Long vehicleId);
}
