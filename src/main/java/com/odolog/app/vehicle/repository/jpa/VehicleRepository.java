package com.odolog.app.vehicle.repository.jpa;

import com.odolog.app.vehicle.domain.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Page<Vehicle> findByOwnerId(Long ownerId, Pageable pageable);

    // 탈퇴 전용. 대상이 "한 사람의 전부"라 페이지를 나눌 수 없음 (나누면 첫 장만 지움)
    List<Vehicle> findAllByOwnerId(Long ownerId);

    boolean existsByOwnerIdAndPlateNumber(Long ownerId, String plateNumber);
}
