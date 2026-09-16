package com.odolog.app.vehicle.repository.jpa;

import com.odolog.app.vehicle.domain.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Page<Vehicle> findByOwnerId(Long ownerId, Pageable pageable);

    // 탈퇴 때만 쓴다. 화면용 조회가 Pageable 을 받는 것과 달리 여기서는 "한 사람의 전부"가
    // 대상이라 페이지를 나눌 수가 없다 — 나누면 첫 장만 지우고 끝난다.
    List<Vehicle> findAllByOwnerId(Long ownerId);

    boolean existsByOwnerIdAndPlateNumber(Long ownerId, String plateNumber);
}
