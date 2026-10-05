package com.odolog.app.vehicle;

import com.odolog.app.common.exception.ErrorCode;
import com.odolog.app.common.exception.type.ConflictException;
import com.odolog.app.common.exception.type.ResourceNotFoundException;
import com.odolog.app.common.validation.InputText;
import com.odolog.app.user.UserRepository;
import com.odolog.app.user.domain.User;
import com.odolog.app.vehicle.domain.Vehicle;
import com.odolog.app.vehicle.dto.request.UpdateOdometerRequest;
import com.odolog.app.vehicle.dto.request.VehicleRegisterRequest;
import com.odolog.app.vehicle.dto.request.VehicleUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Vehicle register(Long ownerId, VehicleRegisterRequest request) {
        String plateNumber = InputText.required(request.plateNumber(), "plateNumber");
        if (vehicleRepository.existsByOwnerIdAndPlateNumber(ownerId, plateNumber)) {
            throw new ConflictException(ErrorCode.PLATE_DUPLICATE, "이미 등록하신 차량 번호입니다: " + plateNumber);
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 사용자입니다: " + ownerId));

        Vehicle vehicle = new Vehicle(owner, plateNumber,
                InputText.required(request.manufacturer(), "manufacturer"),
                InputText.required(request.modelName(), "modelName"), request.modelYear());
        // 0 에서 시작하므로 감소 검사에 걸리지 않음
        vehicle.updateOdometer(request.odometer());

        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle update(Long requesterId, String vehicleId, VehicleUpdateRequest request) {
        // 잠금. 기록 쓰기·차량 삭제와 한 줄로 세움. 겹치면 409 대신 앞선 결과(삭제면 404)를 보고 진행
        Vehicle vehicle = findOwnedVehicleForUpdate(requesterId, vehicleId);

        // 번호판 먼저 처리. 다른 필드 변경 후 exists 전 자동 flush 로 자기 중복 판정 방지
        String plateNumber = InputText.required(request.plateNumber(), "plateNumber");
        if (plateNumber != null && !plateNumber.equals(vehicle.getPlateNumber())) {
            // 같은 번호판인지는 DB 가 판단. 자바 비교는 전각 숫자·악센트에서 DB 와 갈려 자기 자신과 409
            if (vehicleRepository.existsByOwnerIdAndPlateNumberAndIdNot(requesterId, plateNumber, vehicle.getId())) {
                throw new ConflictException(ErrorCode.PLATE_DUPLICATE, "이미 등록하신 차량 번호입니다: " + plateNumber);
            }
            vehicle.changePlateNumber(plateNumber);
        }
        if (request.manufacturer() != null) {
            vehicle.changeManufacturer(InputText.required(request.manufacturer(), "manufacturer"));
        }
        if (request.modelName() != null) {
            vehicle.changeModelName(InputText.required(request.modelName(), "modelName"));
        }
        if (request.modelYear() != null) {
            vehicle.changeModelYear(request.modelYear());
        }

        // save() 불필요. dirty checking
        return vehicle;
    }

    @Transactional
    public Vehicle updateOdometer(Long requesterId, String vehicleId, UpdateOdometerRequest request) {
        // 잠금. 주유가 그 사이 올린 값을 보고 판정(감소면 현재 값을 담은 409)
        Vehicle vehicle = findOwnedVehicleForUpdate(requesterId, vehicleId);

        // 기본은 감소 금지. force 일 때만 정정
        if (request.forced()) {
            vehicle.correctOdometer(request.odometer());
        } else {
            vehicle.updateOdometer(request.odometer());
        }

        return vehicle;
    }

    /** 소유 차량 전부, 행 잠금. 회원 탈퇴 전용(페이지를 나눌 수 없음) */
    @Transactional
    public List<Vehicle> findAllOwnedByForUpdate(Long ownerId) {
        return vehicleRepository.findLockedByOwnerId(ownerId);
    }

    /** 차량만 삭제. 자식 기록은 VehicleRemovalService 가 먼저 지움 */
    @Transactional
    public void remove(List<Vehicle> vehicles) {
        vehicleRepository.deleteAll(vehicles);
    }

    /**
     * 남의 차량도 없는 차량과 같은 404·같은 문구
     * 403 은 존재를 드러냄
     */
    public Vehicle findOwnedVehicle(Long requesterId, String vehicleId) {
        // 공개 id 로 조회. 예전 숫자 주소는 없는 차량
        return owned(requesterId, vehicleId, vehicleRepository.findByPublicId(vehicleId));
    }

    /**
     * 위와 같고 행을 잠금. 차량 수정·주행거리, 기록 등록·수정·삭제, 차량 삭제
     * 트랜잭션의 첫 조회여야 함. 대기 뒤에 앞선 쓰기의 결과가 보이게(REPEATABLE READ 스냅숏)
     */
    @Transactional
    public Vehicle findOwnedVehicleForUpdate(Long requesterId, String vehicleId) {
        return owned(requesterId, vehicleId, vehicleRepository.findLockedByPublicId(vehicleId));
    }

    private Vehicle owned(Long requesterId, String vehicleId, Optional<Vehicle> found) {
        Vehicle vehicle = found.orElseThrow(() -> notFound(vehicleId));

        if (!vehicle.getOwner().getId().equals(requesterId)) {
            throw notFound(vehicleId);
        }

        return vehicle;
    }

    private ResourceNotFoundException notFound(String vehicleId) {
        return new ResourceNotFoundException(ErrorCode.VEHICLE_NOT_FOUND, "존재하지 않는 차량입니다: " + vehicleId);
    }
}
