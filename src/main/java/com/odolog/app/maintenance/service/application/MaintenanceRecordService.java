package com.odolog.app.maintenance.service.application;

import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.type.ServiceType;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.maintenance.dto.request.register.MaintenanceRecordRegisterRequest;
import com.odolog.app.maintenance.dto.request.update.MaintenanceRecordUpdateRequest;
import com.odolog.app.maintenance.dto.response.schedule.NextServiceResponse;
import com.odolog.app.maintenance.repository.jpa.MaintenanceRecordRepository;
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

        MaintenanceRecord record = new MaintenanceRecord(vehicle, request.type(), request.description(),
                request.cost(), request.serviceOdometer(), request.serviceDate());

        return maintenanceRecordRepository.save(record);
    }

    public Page<MaintenanceRecord> findByVehicle(Long requesterId, Long vehicleId, Pageable pageable) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);
        return maintenanceRecordRepository.findByVehicleId(vehicleId, pageable);
    }

    /**
     * 모든 종류의 다음 정비 시점을 한 번에 돌려준다.
     *
     * <p>화면이 종류마다 요청을 보내면 종류 수만큼 왕복이 생긴다. 종류가 5개일 때는 견뎠지만
     * 15개가 되면서 못 견디게 됐다 — 기능이 늘면서 원래 알던 비용이 임계를 넘은 경우다.
     *
     * <p>이력이 하나도 없는 종류는 <b>빼고</b> 준다. "다음 정비 시점"은 마지막 정비가 있어야
     * 나오는 값이고, 15줄 중 13줄이 "기록 없음"이면 화면이 빈칸 목록이 된다.
     */
    public List<NextServiceResponse> calculateAllNextServices(Long requesterId, Long vehicleId) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);

        // 정렬된 목록에서 종류별로 처음 만나는 것이 그 종류의 최신 이력이다.
        Map<ServiceType, MaintenanceRecord> latest = new EnumMap<>(ServiceType.class);
        for (MaintenanceRecord record : maintenanceRecordRepository
                .findByVehicleIdOrderByServiceDateDescIdDesc(vehicleId)) {
            latest.putIfAbsent(record.getType(), record);
        }

        // enum 선언 순서대로 담는다. 화면의 순서를 서버가 정해 주는 편이
        // 클라이언트마다 다르게 정렬하는 것보다 어긋날 여지가 적다.
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

    public NextServiceResponse calculateNextService(Long requesterId, Long vehicleId, ServiceType type) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);

        return maintenanceRecordRepository.findTopByVehicleIdAndTypeOrderByServiceDateDescIdDesc(vehicleId, type)
                .map(record -> toNextService(type, record))
                .orElse(new NextServiceResponse(type, null, null, null, null));
    }

    public MaintenanceRecord findOne(Long requesterId, Long vehicleId, Long recordId) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);
        return findRecordInVehicle(vehicleId, recordId);
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
            record.changeDescription(request.description());
        }
        if (request.cost() != null) {
            record.changeCost(request.cost());
        }
        if (request.serviceOdometer() != null) {
            record.changeServiceOdometer(request.serviceOdometer());
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

    private MaintenanceRecord findRecordInVehicle(Long vehicleId, Long recordId) {
        return maintenanceRecordRepository.findByIdAndVehicleId(recordId, vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 정비 이력입니다: " + recordId));
    }
}
