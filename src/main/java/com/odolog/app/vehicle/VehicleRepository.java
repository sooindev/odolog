package com.odolog.app.vehicle;

import com.odolog.app.vehicle.domain.Vehicle;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Page<Vehicle> findByOwnerId(Long ownerId, Pageable pageable);

    // 탈퇴 전용 전체 조회. 페이지 분할 시 첫 장만 삭제되는 문제
    List<Vehicle> findAllByOwnerId(Long ownerId);

    /** URL 의 공개 id 로 조회 */
    Optional<Vehicle> findByPublicId(String publicId);

    /**
     * 행 잠금 조회. 기록 쓰기와 차량 삭제를 한 줄로 세움
     * 잠그지 않으면 차량을 지우는 동안 들어온 기록이 남아 마지막 차량 DELETE 가 FK 로 실패
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Vehicle> findLockedByPublicId(String publicId);

    /** 탈퇴용 잠금 조회. 위와 같은 이유 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Vehicle> findLockedByOwnerId(Long ownerId);

    boolean existsByOwnerIdAndPlateNumber(Long ownerId, String plateNumber);

    /** 자기 자신 제외 중복 검사. 같은 번호판인지는 DB 정렬 규칙이 판단(전각·악센트까지) */
    boolean existsByOwnerIdAndPlateNumberAndIdNot(Long ownerId, String plateNumber, Long id);

    /**
     * 가져오기의 같은 차 찾기. 판단 기준은 위와 같이 DB
     * 행 잠금. 같은 차를 지우는 중이면 기다렸다가 결과를 보고 진행(안 잠그면 지워진 차에 기록을 넣어 FK 500)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Vehicle> findLockedByOwnerIdAndPlateNumber(Long ownerId, String plateNumber);
}
