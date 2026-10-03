package com.odolog.app.vehicle;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Page<Vehicle> findByOwnerId(Long ownerId, Pageable pageable);

    // 탈퇴 전용 전체 조회. 페이지 분할 시 첫 장만 삭제되는 문제
    List<Vehicle> findAllByOwnerId(Long ownerId);

    /** URL 의 공개 id 로 조회 */
    Optional<Vehicle> findByPublicId(String publicId);

    boolean existsByOwnerIdAndPlateNumber(Long ownerId, String plateNumber);

    /** 자기 자신 제외 중복 검사. 같은 번호판인지는 DB 정렬 규칙이 판단(전각·악센트까지) */
    boolean existsByOwnerIdAndPlateNumberAndIdNot(Long ownerId, String plateNumber, Long id);

    /** 가져오기의 같은 차 찾기. 판단 기준은 위와 같이 DB */
    Optional<Vehicle> findByOwnerIdAndPlateNumber(Long ownerId, String plateNumber);
}
