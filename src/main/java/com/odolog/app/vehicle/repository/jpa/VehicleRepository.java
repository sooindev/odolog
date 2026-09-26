package com.odolog.app.vehicle.repository.jpa;

import com.odolog.app.vehicle.domain.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Page<Vehicle> findByOwnerId(Long ownerId, Pageable pageable);

    // 탈퇴 전용. 대상이 "한 사람의 전부"라 페이지를 나눌 수 없음 (나누면 첫 장만 지움)
    List<Vehicle> findAllByOwnerId(Long ownerId);

    /** URL 의 공개 id 로 찾기. 숫자 id 는 서버 밖으로 나가지 않는다 */
    Optional<Vehicle> findByPublicId(String publicId);

    boolean existsByOwnerIdAndPlateNumber(Long ownerId, String plateNumber);
}
