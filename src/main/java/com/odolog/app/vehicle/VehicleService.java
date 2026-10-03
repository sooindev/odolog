package com.odolog.app.vehicle;

import com.odolog.app.common.exception.ErrorCode;
import com.odolog.app.common.exception.type.ConflictException;
import com.odolog.app.common.InputText;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.vehicle.dto.request.UpdateOdometerRequest;
import com.odolog.app.vehicle.dto.request.VehicleRegisterRequest;
import com.odolog.app.vehicle.dto.request.VehicleUpdateRequest;
import com.odolog.app.common.exception.type.ResourceNotFoundException;
import com.odolog.app.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        Vehicle vehicle = findOwnedVehicle(requesterId, vehicleId);

        // 번호판 먼저 처리. 다른 필드 변경 후 exists 전 자동 flush 로 자기 중복 판정 방지
        String plateNumber = InputText.required(request.plateNumber(), "plateNumber");
        if (plateNumber != null && !plateNumber.equals(vehicle.getPlateNumber())) {
            // 다른 번호판으로 바꿀 때만 검사. DB 와 같은 기준(앞뒤 공백·대소문자 무시)
            if (!plateNumber.equalsIgnoreCase(vehicle.getPlateNumber().strip())
                    && vehicleRepository.existsByOwnerIdAndPlateNumber(requesterId, plateNumber)) {
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
        Vehicle vehicle = findOwnedVehicle(requesterId, vehicleId);

        // 기본은 감소 금지. force 일 때만 정정
        if (request.forced()) {
            vehicle.correctOdometer(request.odometer());
        } else {
            vehicle.updateOdometer(request.odometer());
        }

        return vehicle;
    }

    /** 소유 차량 전부. 회원 탈퇴 전용, 페이지를 나눌 수 없음 */
    public List<Vehicle> findAllOwnedBy(Long ownerId) {
        return vehicleRepository.findAllByOwnerId(ownerId);
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
        Vehicle vehicle = vehicleRepository.findByPublicId(vehicleId)
                .orElseThrow(() -> notFound(vehicleId));

        if (!vehicle.getOwner().getId().equals(requesterId)) {
            throw notFound(vehicleId);
        }

        return vehicle;
    }

    private ResourceNotFoundException notFound(String vehicleId) {
        return new ResourceNotFoundException(ErrorCode.VEHICLE_NOT_FOUND, "존재하지 않는 차량입니다: " + vehicleId);
    }
}
