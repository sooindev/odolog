package com.odolog.app.vehicle.service.application;

import com.odolog.app.common.dto.request.page.SortGuard;
import com.odolog.app.common.exception.type.ConflictException;
import com.odolog.app.common.text.InputText;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.maintenance.domain.calculation.NextService;
import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.entity.ServiceInterval;
import com.odolog.app.vehicle.dto.request.odometer.UpdateOdometerRequest;
import com.odolog.app.vehicle.dto.request.register.VehicleRegisterRequest;
import com.odolog.app.vehicle.dto.request.update.VehicleUpdateRequest;
import com.odolog.app.common.exception.type.ResourceNotFoundException;
import com.odolog.app.fuel.repository.jpa.FuelRecordRepository;
import com.odolog.app.maintenance.repository.jpa.MaintenanceRecordRepository;
import com.odolog.app.maintenance.repository.jpa.ServiceIntervalRepository;
import com.odolog.app.user.repository.jpa.UserRepository;
import com.odolog.app.vehicle.dto.response.vehicle.VehicleResponse;
import com.odolog.app.vehicle.repository.jpa.VehicleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final ServiceIntervalRepository serviceIntervalRepository;
    // 서비스 대신 리포지토리 주입. FuelRecordService → VehicleService 순환 방지
    private final FuelRecordRepository fuelRecordRepository;

    public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository,
                           MaintenanceRecordRepository maintenanceRecordRepository,
                           ServiceIntervalRepository serviceIntervalRepository,
                           FuelRecordRepository fuelRecordRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.serviceIntervalRepository = serviceIntervalRepository;
        this.fuelRecordRepository = fuelRecordRepository;
    }

    @Transactional
    public Vehicle register(Long ownerId, VehicleRegisterRequest request) {
        String plateNumber = InputText.strip(request.plateNumber());
        if (vehicleRepository.existsByOwnerIdAndPlateNumber(ownerId, plateNumber)) {
            throw new ConflictException("이미 등록하신 차량 번호입니다: " + plateNumber);
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 사용자입니다: " + ownerId));

        Vehicle vehicle = new Vehicle(owner, plateNumber, InputText.strip(request.manufacturer()),
                InputText.strip(request.modelName()), request.modelYear());

        return vehicleRepository.save(vehicle);
    }

    /** 화면이 쓰는 속성만 정렬 허용 */
    private static final Set<String> SORTABLE = Set.of(
            "createdAt", "plateNumber", "manufacturer", "modelName", "modelYear", "odometer");

    /**
     * 목록은 DTO 반환. 지남 수가 계산값
     * 쿼리 3번. 이력·주기는 소유자 단위로 한 번에 조회
     */
    public Page<VehicleResponse> findMyVehicles(Long ownerId, Pageable pageable) {
        SortGuard.allowOnly(pageable, SORTABLE);

        Page<Vehicle> page = vehicleRepository.findByOwnerId(ownerId, pageable);
        if (page.isEmpty()) {
            return page.map(vehicle -> VehicleResponse.of(vehicle, 0));
        }

        List<MaintenanceRecord> records =
                maintenanceRecordRepository.findByVehicle_Owner_IdOrderByServiceDateDescIdDesc(ownerId);
        List<ServiceInterval> intervals = serviceIntervalRepository.findByVehicle_Owner_Id(ownerId);
        LocalDate today = LocalDate.now();

        return page.map(vehicle -> VehicleResponse.of(vehicle,
                overdueCountOf(vehicle, records, intervals, today)));
    }

    /** 홈 요약과 같은 NextService 계산 */
    private int overdueCountOf(Vehicle vehicle, List<MaintenanceRecord> records,
                               List<ServiceInterval> intervals, LocalDate today) {

        List<MaintenanceRecord> mine = records.stream()
                .filter(record -> record.getVehicle().getId().equals(vehicle.getId()))
                .toList();
        List<ServiceInterval> mineIntervals = intervals.stream()
                .filter(interval -> interval.getVehicle().getId().equals(vehicle.getId()))
                .toList();

        return (int) NextService.of(mine, mineIntervals, vehicle.getOdometer(), today).stream()
                .filter(NextService::overdue)
                .count();
    }

    @Transactional
    public Vehicle update(Long requesterId, String vehicleId, VehicleUpdateRequest request) {
        Vehicle vehicle = findOwnedVehicle(requesterId, vehicleId);

        // 번호판 먼저 처리. 다른 필드 변경 후 exists 전 자동 flush 로 자기 중복 판정 방지
        String plateNumber = InputText.strip(request.plateNumber());
        if (plateNumber != null && !plateNumber.equals(vehicle.getPlateNumber())) {
            // 다른 번호판으로 바꿀 때만 검사. DB 와 같은 기준(앞뒤 공백·대소문자 무시)
            if (!plateNumber.equalsIgnoreCase(vehicle.getPlateNumber().strip())
                    && vehicleRepository.existsByOwnerIdAndPlateNumber(requesterId, plateNumber)) {
                throw new ConflictException("이미 등록하신 차량 번호입니다: " + plateNumber);
            }
            vehicle.changePlateNumber(plateNumber);
        }
        if (request.manufacturer() != null) {
            vehicle.changeManufacturer(InputText.strip(request.manufacturer()));
        }
        if (request.modelName() != null) {
            vehicle.changeModelName(InputText.strip(request.modelName()));
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

    @Transactional
    public void delete(Long requesterId, String vehicleId) {
        Vehicle vehicle = findOwnedVehicle(requesterId, vehicleId);
        // 자식 먼저, 차량 나중. FK 제약
        maintenanceRecordRepository.deleteByVehicleId(vehicle.getId());
        serviceIntervalRepository.deleteByVehicleId(vehicle.getId());
        fuelRecordRepository.deleteByVehicleId(vehicle.getId());
        vehicleRepository.delete(vehicle);
    }

    /** 한 사용자의 차량 일괄 삭제. 회원 탈퇴 전용 */
    @Transactional
    public void deleteAllOwnedBy(Long ownerId) {
        List<Vehicle> vehicles = vehicleRepository.findAllByOwnerId(ownerId);

        // 이력 먼저 삭제. FK 제약
        for (Vehicle vehicle : vehicles) {
            maintenanceRecordRepository.deleteByVehicleId(vehicle.getId());
            serviceIntervalRepository.deleteByVehicleId(vehicle.getId());
            fuelRecordRepository.deleteByVehicleId(vehicle.getId());
        }
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
        return new ResourceNotFoundException("존재하지 않는 차량입니다: " + vehicleId);
    }
}
