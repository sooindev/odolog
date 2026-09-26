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
     * 차량별 권장 주기 설정. 둘 다 null 이면 행 삭제(기본값 복귀)
     * 빈 행이 남으면 customized 가 거짓으로 true
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

    /** 화면이 쓰는 속성만 정렬 허용 */
    private static final Set<String> SORTABLE =
            Set.of("serviceDate", "id", "cost", "serviceOdometer", "type");

    /** type 이 null 이면 전체. 소유권 검사·정렬 가드 한 곳 유지 */
    public Page<MaintenanceRecord> findByVehicle(Long requesterId, String vehicleId,
                                                 ServiceType type, Pageable pageable) {
        Long id = vehicleService.findOwnedVehicle(requesterId, vehicleId).getId();
        SortGuard.allowOnly(pageable, SORTABLE);

        return type == null
                ? maintenanceRecordRepository.findByVehicleId(id, pageable)
                : maintenanceRecordRepository.findByVehicleIdAndType(id, type, pageable);
    }

    /**
     * 전체 종류의 다음 정비 시점. 이력 없는 종류 제외
     * 계산·지남 판정은 NextService(홈 요약과 공용)
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
            // 수정에도 같은 규칙
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

    /** 빈 문자열은 null */
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private MaintenanceRecord findRecordInVehicle(Long vehicleId, String recordId) {
        return maintenanceRecordRepository.findByPublicIdAndVehicleId(recordId, vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 정비 이력입니다: " + recordId));
    }
}
