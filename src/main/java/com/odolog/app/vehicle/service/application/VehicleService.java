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
    // 서비스가 아니라 리포지토리 주입 — 서비스끼리면 진짜 순환 참조
    // (FuelRecordService 가 VehicleService 를 이미 씀)
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

    /** 화면이 쓰는 것만 정렬 대상. 연관 엔티티를 타고 들어가는 정렬을 막는다 */
    private static final Set<String> SORTABLE = Set.of(
            "createdAt", "plateNumber", "manufacturer", "modelName", "modelYear", "odometer");

    /**
     * 목록만 DTO 를 돌려준다. 지남 수가 엔티티에 없는 계산값이기 때문 —
     * FuelRecordService.findByVehicle 이 같은 이유로 DTO 를 돌려준다
     *
     * 쿼리 3번(페이지 + 정비 이력 + 주기). 이력과 주기는 소유자 단위로 한 번에 읽고 나눈다 —
     * 차량마다 조회하면 페이지 크기만큼 늘어난다
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

    /** 홈 요약과 같은 계산(NextService)을 쓴다 — 두 화면이 다른 수를 말하면 안 된다 */
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

        // 번호판 먼저. 다른 필드를 먼저 바꾸면 dirty 상태가 되고 exists 직전에 자동 flush —
        // 방금 쓴 값을 다시 조회해 자기를 중복으로 판정
        String plateNumber = InputText.strip(request.plateNumber());
        if (plateNumber != null && !plateNumber.equals(vehicle.getPlateNumber())) {
            // 다른 번호판으로 바꿀 때만 검사. 판단은 DB 와 같은 기준(앞뒤 공백·대소문자 무시) —
            // 자바 equals 로만 보면 "12가3456 " → "12가3456" 이 자기 자신과 중복으로 잡혀 409
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

        // save() 불필요. 영속 상태라 dirty checking 이 UPDATE 생성
        return vehicle;
    }

    @Transactional
    public Vehicle updateOdometer(Long requesterId, String vehicleId, UpdateOdometerRequest request) {
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
    public void delete(Long requesterId, String vehicleId) {
        Vehicle vehicle = findOwnedVehicle(requesterId, vehicleId);
        // 자식 먼저, 차량 나중 — 바꾸면 FK 제약 위반
        maintenanceRecordRepository.deleteByVehicleId(vehicle.getId());
        serviceIntervalRepository.deleteByVehicleId(vehicle.getId());
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
            serviceIntervalRepository.deleteByVehicleId(vehicle.getId());
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
     * 정비·주유는 findByPublicIdAndVehicleId 라 처음부터 404 하나였다 — 차량만 혼자 달랐다
     *
     * 상태 코드만 맞추고 문구를 달리하면 소용없다. 그래서 두 경우가 **같은 예외를 만들어 쓴다**
     */
    public Vehicle findOwnedVehicle(Long requesterId, String vehicleId) {
        // 공개 id 로 찾는다. 예전 숫자 주소(/vehicles/1)는 그냥 없는 차량이다
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
