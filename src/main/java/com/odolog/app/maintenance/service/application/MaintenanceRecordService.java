package com.odolog.app.maintenance.service.application;

import com.odolog.app.maintenance.domain.calculation.NextService;
import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.entity.ServiceInterval;
import com.odolog.app.maintenance.domain.type.ServiceType;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.maintenance.dto.request.register.MaintenanceRecordRegisterRequest;
import com.odolog.app.maintenance.dto.request.update.MaintenanceRecordUpdateRequest;
import com.odolog.app.maintenance.dto.response.schedule.NextServiceResponse;
import com.odolog.app.maintenance.repository.jpa.MaintenanceRecordRepository;
import com.odolog.app.maintenance.repository.jpa.ServiceIntervalRepository;
import com.odolog.app.common.dto.request.page.SortGuard;
import com.odolog.app.common.exception.type.ResourceNotFoundException;
import com.odolog.app.vehicle.service.application.VehicleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class MaintenanceRecordService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final ServiceIntervalRepository serviceIntervalRepository;
    private final VehicleService vehicleService;

    public MaintenanceRecordService(MaintenanceRecordRepository maintenanceRecordRepository,
                                     ServiceIntervalRepository serviceIntervalRepository,
                                     VehicleService vehicleService) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.serviceIntervalRepository = serviceIntervalRepository;
        this.vehicleService = vehicleService;
    }

    /**
     * 이 차량에서 쓸 권장 주기를 정한다. 둘 다 null 이면 기본값으로 되돌린다
     *
     * 되돌릴 때 행을 지우는 이유: 값이 전부 비어 있는 행은 "덮어쓰지 않음" 과 같은 뜻인데,
     * 남겨 두면 customized 가 true 로 남아 화면이 "기본과 다름" 이라고 거짓말한다
     */
    @Transactional
    public void changeInterval(Long requesterId, String vehicleId, ServiceType type,
                               Integer intervalKm, Integer intervalMonths) {

        Vehicle vehicle = vehicleService.findOwnedVehicle(requesterId, vehicleId);

        serviceIntervalRepository.findByVehicleIdAndType(vehicle.getId(), type).ifPresentOrElse(
                existing -> {
                    existing.change(intervalKm, intervalMonths);
                    if (existing.isEmpty()) {
                        serviceIntervalRepository.delete(existing);
                    }
                },
                () -> {
                    if (intervalKm != null || intervalMonths != null) {
                        serviceIntervalRepository.save(
                                new ServiceInterval(vehicle, type, intervalKm, intervalMonths));
                    }
                });
    }

    @Transactional
    public MaintenanceRecord register(Long requesterId, String vehicleId, MaintenanceRecordRegisterRequest request) {
        Vehicle vehicle = vehicleService.findOwnedVehicle(requesterId, vehicleId);

        MaintenanceRecord record = new MaintenanceRecord(vehicle, request.type(),
                blankToNull(request.description()),
                request.cost(), request.serviceOdometer(), request.serviceDate());

        // 정비 시점 주행거리가 더 크면 차량도 갱신. 주유와 같은 규칙
        vehicle.liftOdometerTo(request.serviceOdometer());

        return maintenanceRecordRepository.save(record);
    }

    /** 화면이 쓰는 것만 정렬 대상 (차량 목록과 같은 이유 — SortGuard 주석 참고) */
    private static final Set<String> SORTABLE =
            Set.of("serviceDate", "id", "cost", "serviceOdometer", "type");

    /** type 이 null 이면 전체. 전용 메서드를 하나 더 만들지 않는 이유 — 호출부가 갈리면
     *  소유권 검사와 정렬 가드를 두 곳에서 되풀이해야 한다 */
    public Page<MaintenanceRecord> findByVehicle(Long requesterId, String vehicleId,
                                                 ServiceType type, Pageable pageable) {
        Long id = vehicleService.findOwnedVehicle(requesterId, vehicleId).getId();
        SortGuard.allowOnly(pageable, SORTABLE);

        return type == null
                ? maintenanceRecordRepository.findByVehicleId(id, pageable)
                : maintenanceRecordRepository.findByVehicleIdAndType(id, type, pageable);
    }

    /**
     * 전체 종류의 다음 정비 시점을 한 번에. 종류마다 요청하면 15왕복
     * 이력 없는 종류는 제외 — 15줄 중 13줄이 "기록 없음"이면 빈칸 목록이 됨
     *
     * 계산과 "지남" 판정은 NextService 가 한다 — 홈 요약이 같은 것을 쓴다
     * "오늘"을 밖에서 받지 않고 여기서 만드는 이유: 이 경로는 화면이 바로 부르는 조회라
     * 고정할 이유가 없고, 고정이 필요한 홈 요약 쪽은 자기가 today 를 넘긴다
     */
    public List<NextServiceResponse> calculateAllNextServices(Long requesterId, String vehicleId) {
        Vehicle vehicle = vehicleService.findOwnedVehicle(requesterId, vehicleId);

        return NextService.of(
                        maintenanceRecordRepository.findByVehicleIdOrderByServiceDateDescIdDesc(vehicle.getId()),
                        serviceIntervalRepository.findByVehicleId(vehicle.getId()),
                        vehicle.getOdometer(), LocalDate.now())
                .stream()
                .map(NextServiceResponse::from)
                .toList();
    }



    @Transactional
    public MaintenanceRecord update(Long requesterId, String vehicleId, String recordId,
                                     MaintenanceRecordUpdateRequest request) {
        Vehicle vehicle = vehicleService.findOwnedVehicle(requesterId, vehicleId);
        MaintenanceRecord record = findRecordInVehicle(vehicle.getId(), recordId);

        if (request.type() != null) {
            record.changeType(request.type());
        }
        if (request.description() != null) {
            record.changeDescription(blankToNull(request.description()));
        }
        if (request.cost() != null) {
            record.changeCost(request.cost());
        }
        if (request.serviceOdometer() != null) {
            record.changeServiceOdometer(request.serviceOdometer());
            // 수정에도 같은 규칙. 자리수 오타 정정이 흔함
            record.getVehicle().liftOdometerTo(request.serviceOdometer());
        }
        if (request.serviceDate() != null) {
            record.changeServiceDate(request.serviceDate());
        }

        return record;
    }

    @Transactional
    public void delete(Long requesterId, String vehicleId, String recordId) {
        Vehicle vehicle = vehicleService.findOwnedVehicle(requesterId, vehicleId);
        MaintenanceRecord record = findRecordInVehicle(vehicle.getId(), recordId);

        maintenanceRecordRepository.delete(record);
    }

    /** 빈 문자열은 "없음" 으로. 주유 메모·프로필 전화번호와 같은 규칙 */
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private MaintenanceRecord findRecordInVehicle(Long vehicleId, String recordId) {
        return maintenanceRecordRepository.findByPublicIdAndVehicleId(recordId, vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 정비 이력입니다: " + recordId));
    }
}
