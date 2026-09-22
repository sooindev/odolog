package com.odolog.app.maintenance.service.application;

import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.type.ServiceType;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.maintenance.dto.request.register.MaintenanceRecordRegisterRequest;
import com.odolog.app.maintenance.dto.request.update.MaintenanceRecordUpdateRequest;
import com.odolog.app.maintenance.dto.response.schedule.NextServiceResponse;
import com.odolog.app.maintenance.repository.jpa.MaintenanceRecordRepository;
import com.odolog.app.common.dto.request.page.SortGuard;
import com.odolog.app.common.exception.type.ResourceNotFoundException;
import com.odolog.app.vehicle.service.application.VehicleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class MaintenanceRecordService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final VehicleService vehicleService;

    public MaintenanceRecordService(MaintenanceRecordRepository maintenanceRecordRepository,
                                     VehicleService vehicleService) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.vehicleService = vehicleService;
    }

    @Transactional
    public MaintenanceRecord register(Long requesterId, Long vehicleId, MaintenanceRecordRegisterRequest request) {
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

    public Page<MaintenanceRecord> findByVehicle(Long requesterId, Long vehicleId, Pageable pageable) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);
        SortGuard.allowOnly(pageable, SORTABLE);

        return maintenanceRecordRepository.findByVehicleId(vehicleId, pageable);
    }

    /**
     * 전체 종류의 다음 정비 시점을 한 번에. 종류마다 요청하면 15왕복
     * 이력 없는 종류는 제외 — 15줄 중 13줄이 "기록 없음"이면 빈칸 목록이 됨
     */
    public List<NextServiceResponse> calculateAllNextServices(Long requesterId, Long vehicleId) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);

        // 정렬된 목록에서 종류별 첫 줄 = 그 종류의 최신 이력
        Map<ServiceType, MaintenanceRecord> latest = new EnumMap<>(ServiceType.class);
        for (MaintenanceRecord record : maintenanceRecordRepository
                .findByVehicleIdOrderByServiceDateDescIdDesc(vehicleId)) {
            latest.putIfAbsent(record.getType(), record);
        }

        // enum 선언 순서 유지. 화면 순서를 서버가 정하는 편이 어긋날 여지가 적음
        List<NextServiceResponse> responses = new ArrayList<>(latest.size());
        for (ServiceType type : ServiceType.values()) {
            MaintenanceRecord record = latest.get(type);
            if (record != null) {
                responses.add(toNextService(type, record));
            }
        }

        return responses;
    }

    private NextServiceResponse toNextService(ServiceType type, MaintenanceRecord record) {
        Integer intervalKm = type.getRecommendedIntervalKm();
        Integer nextOdometer = (intervalKm == null) ? null : record.getServiceOdometer() + intervalKm;

        Integer intervalMonths = type.getRecommendedIntervalMonths();
        LocalDate nextDate = (intervalMonths == null) ? null
                : record.getServiceDate().plusMonths(intervalMonths);

        return new NextServiceResponse(type, record.getServiceOdometer(), nextOdometer,
                record.getServiceDate(), nextDate);
    }



    @Transactional
    public MaintenanceRecord update(Long requesterId, Long vehicleId, Long recordId,
                                     MaintenanceRecordUpdateRequest request) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);
        MaintenanceRecord record = findRecordInVehicle(vehicleId, recordId);

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
    public void delete(Long requesterId, Long vehicleId, Long recordId) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);
        MaintenanceRecord record = findRecordInVehicle(vehicleId, recordId);

        maintenanceRecordRepository.delete(record);
    }

    /** 빈 문자열은 "없음" 으로. 주유 메모·프로필 전화번호와 같은 규칙 */
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private MaintenanceRecord findRecordInVehicle(Long vehicleId, Long recordId) {
        return maintenanceRecordRepository.findByIdAndVehicleId(recordId, vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 정비 이력입니다: " + recordId));
    }
}
