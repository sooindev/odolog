package com.odolog.app.vehicle.service.application;

import com.odolog.app.common.exception.type.ConflictException;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.vehicle.dto.request.odometer.UpdateOdometerRequest;
import com.odolog.app.vehicle.dto.request.register.VehicleRegisterRequest;
import com.odolog.app.vehicle.dto.request.update.VehicleUpdateRequest;
import com.odolog.app.common.exception.type.ResourceNotFoundException;
import com.odolog.app.fuel.repository.jpa.FuelRecordRepository;
import com.odolog.app.maintenance.repository.jpa.MaintenanceRecordRepository;
import com.odolog.app.user.repository.jpa.UserRepository;
import com.odolog.app.vehicle.repository.jpa.VehicleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    // 서비스가 아니라 리포지토리 주입 — 서비스끼리면 진짜 순환 참조
    // (FuelRecordService 가 VehicleService 를 이미 씀)
    private final FuelRecordRepository fuelRecordRepository;

    public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository,
                           MaintenanceRecordRepository maintenanceRecordRepository,
                           FuelRecordRepository fuelRecordRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.fuelRecordRepository = fuelRecordRepository;
    }

    @Transactional
    public Vehicle register(Long ownerId, VehicleRegisterRequest request) {
        if (vehicleRepository.existsByOwnerIdAndPlateNumber(ownerId, request.plateNumber())) {
            throw new ConflictException("이미 등록하신 차량 번호입니다: " + request.plateNumber());
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 사용자입니다: " + ownerId));

        Vehicle vehicle = new Vehicle(owner, request.plateNumber(), request.manufacturer(),
                request.modelName(), request.modelYear());

        return vehicleRepository.save(vehicle);
    }

    public Page<Vehicle> findMyVehicles(Long ownerId, Pageable pageable) {
        return vehicleRepository.findByOwnerId(ownerId, pageable);
    }

    @Transactional
    public Vehicle update(Long requesterId, Long vehicleId, VehicleUpdateRequest request) {
        Vehicle vehicle = findOwnedVehicle(requesterId, vehicleId);

        // 번호판 먼저. 다른 필드를 먼저 바꾸면 dirty 상태가 되고 exists 직전에 자동 flush —
        // 방금 쓴 값을 다시 조회해 자기를 중복으로 판정
        if (request.plateNumber() != null && !request.plateNumber().equals(vehicle.getPlateNumber())) {
            // 값이 실제로 바뀔 때만 검사. 없으면 제조사만 고쳐도 자기가 중복으로 잡혀 409
            // ...AndIdNot 쿼리를 새로 만드는 대신 조건으로 해결 — 리포지토리가 안 늘어남
            if (vehicleRepository.existsByOwnerIdAndPlateNumber(requesterId, request.plateNumber())) {
                throw new ConflictException("이미 등록하신 차량 번호입니다: " + request.plateNumber());
            }
            vehicle.changePlateNumber(request.plateNumber());
        }
        if (request.manufacturer() != null) {
            vehicle.changeManufacturer(request.manufacturer());
        }
        if (request.modelName() != null) {
            vehicle.changeModelName(request.modelName());
        }
        if (request.modelYear() != null) {
            vehicle.changeModelYear(request.modelYear());
        }

        // save() 불필요. 영속 상태라 dirty checking 이 UPDATE 생성
        return vehicle;
    }

    @Transactional
    public Vehicle updateOdometer(Long requesterId, Long vehicleId, UpdateOdometerRequest request) {
        Vehicle vehicle = findOwnedVehicle(requesterId, vehicleId);

        // 기본은 감소 금지. force 를 실어야만 정정 경로로 간다
        if (request.forced()) {
            vehicle.correctOdometer(request.odometer());
        } else {
            vehicle.updateOdometer(request.odometer());
        }

        return vehicle;
    }

    @Transactional
    public void delete(Long requesterId, Long vehicleId) {
        Vehicle vehicle = findOwnedVehicle(requesterId, vehicleId);
        // 자식 먼저, 차량 나중 — 바꾸면 FK 제약 위반
        maintenanceRecordRepository.deleteByVehicleId(vehicle.getId());
        fuelRecordRepository.deleteByVehicleId(vehicle.getId());
        vehicleRepository.delete(vehicle);
    }

    /**
     * 한 사용자의 차량을 이력까지 일괄 삭제. 회원 탈퇴 전용
     * delete() 반복 대신 — 차량마다 findById 와 소유권 검사가 반복됨 (이미 ownerId 로 조회한 것)
     */
    @Transactional
    public void deleteAllOwnedBy(Long ownerId) {
        List<Vehicle> vehicles = vehicleRepository.findAllByOwnerId(ownerId);

        // 차량을 먼저 지우면 이력이 붙잡고 있어 FK 제약 위반
        for (Vehicle vehicle : vehicles) {
            maintenanceRecordRepository.deleteByVehicleId(vehicle.getId());
            fuelRecordRepository.deleteByVehicleId(vehicle.getId());
        }
        vehicleRepository.deleteAll(vehicles);
    }

    /**
     * 남의 차량도 "없다"고 답한다
     *
     * 403 은 "권한이 없다"는 뜻이지만 동시에 **"있긴 하다"** 는 뜻을 나른다.
     * 차량 id 가 1,2,3… 으로 이어지므로 403 과 404 가 갈리면 훑어서
     * 어느 번호가 쓰이고 있는지 셀 수 있다
     *
     * 정비·주유는 findByIdAndVehicleId 라 처음부터 404 하나였다 — 차량만 혼자 달랐다
     *
     * 상태 코드만 맞추고 문구를 달리하면 소용없다. 그래서 두 경우가 **같은 예외를 만들어 쓴다**
     */
    public Vehicle findOwnedVehicle(Long requesterId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> notFound(vehicleId));

        if (!vehicle.getOwner().getId().equals(requesterId)) {
            throw notFound(vehicleId);
        }

        return vehicle;
    }

    private ResourceNotFoundException notFound(Long vehicleId) {
        return new ResourceNotFoundException("존재하지 않는 차량입니다: " + vehicleId);
    }
}
