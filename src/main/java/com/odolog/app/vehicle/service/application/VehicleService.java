package com.odolog.app.vehicle.service.application;

import com.odolog.app.common.exception.type.ConflictException;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.vehicle.dto.request.odometer.UpdateOdometerRequest;
import com.odolog.app.vehicle.dto.request.register.VehicleRegisterRequest;
import com.odolog.app.vehicle.dto.request.update.VehicleUpdateRequest;
import com.odolog.app.common.exception.type.ForbiddenAccessException;
import com.odolog.app.common.exception.type.ResourceNotFoundException;
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

    public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository,
                           MaintenanceRecordRepository maintenanceRecordRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
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

        // 번호판을 가장 먼저 처리한다. 다른 필드를 먼저 바꿔 두면 엔티티가 더러워진(dirty) 상태가
        // 되고, 아래 exists 쿼리 직전에 Hibernate 가 그걸 자동으로 flush 해 버릴 수 있다.
        // 그러면 방금 쓴 값을 내가 다시 조회해서 "중복"이라고 판정하는 일이 생긴다.
        if (request.plateNumber() != null && !request.plateNumber().equals(vehicle.getPlateNumber())) {
            // 값이 실제로 바뀔 때만 검사한다. 이 조건이 없으면 번호판을 그대로 두고 제조사만
            // 고쳐도 자기 자신이 검색되어 409 가 난다 — 등록 때 쓰던 검사를 그대로 가져오면
            // 반드시 밟는 함정이다. id 로 자기를 빼는 쿼리(...AndIdNot)를 새로 만드는 방법도
            // 있지만, "안 바뀌었으면 검사할 것도 없다"가 더 단순하고 리포지토리도 안 늘어난다.
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

        // save() 를 부르지 않는다. 영속 상태라 dirty checking 이 UPDATE 를 만들어 준다.
        return vehicle;
    }

    @Transactional
    public Vehicle updateOdometer(Long requesterId, Long vehicleId, UpdateOdometerRequest request) {
        Vehicle vehicle = findOwnedVehicle(requesterId, vehicleId);
        vehicle.updateOdometer(request.odometer());

        return vehicle;
    }

    @Transactional
    public void delete(Long requesterId, Long vehicleId) {
        Vehicle vehicle = findOwnedVehicle(requesterId, vehicleId);
        maintenanceRecordRepository.deleteByVehicleId(vehicle.getId());
        vehicleRepository.delete(vehicle);
    }

    /**
     * 한 사용자의 차량을 이력까지 전부 지운다. 회원 탈퇴에서만 쓴다.
     *
     * delete() 를 차량 수만큼 부르지 않는 이유: 그러면 차량마다 findById 가 한 번씩 더 나가고
     * 소유권 검사도 매번 반복된다. 여기서는 ownerId 로 조회한 것이라 이미 전부 내 차량이다.
     */
    @Transactional
    public void deleteAllOwnedBy(Long ownerId) {
        List<Vehicle> vehicles = vehicleRepository.findAllByOwnerId(ownerId);

        // 순서가 중요하다. 차량을 먼저 지우면 이력이 붙잡고 있어 FK 제약에 걸린다.
        for (Vehicle vehicle : vehicles) {
            maintenanceRecordRepository.deleteByVehicleId(vehicle.getId());
        }
        vehicleRepository.deleteAll(vehicles);
    }

    public Vehicle findOwnedVehicle(Long requesterId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 차량입니다: " + vehicleId));

        if (!vehicle.getOwner().getId().equals(requesterId)) {
            throw new ForbiddenAccessException("본인 소유의 차량만 접근할 수 있습니다.");
        }

        return vehicle;
    }
}
