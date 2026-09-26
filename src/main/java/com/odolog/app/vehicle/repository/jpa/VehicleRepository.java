package com.odolog.app.vehicle.repository.jpa;

import com.odolog.app.vehicle.domain.entity.Vehicle;
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
}
