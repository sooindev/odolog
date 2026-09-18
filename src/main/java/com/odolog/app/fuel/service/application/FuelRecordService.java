package com.odolog.app.fuel.service.application;

import com.odolog.app.common.exception.type.ResourceNotFoundException;
import com.odolog.app.fuel.domain.calculation.FuelAnomaly;
import com.odolog.app.fuel.domain.calculation.FuelEfficiency;
import com.odolog.app.fuel.domain.entity.FuelRecord;
import com.odolog.app.fuel.dto.request.register.FuelRecordRegisterRequest;
import com.odolog.app.fuel.dto.request.update.FuelRecordUpdateRequest;
import com.odolog.app.fuel.dto.response.record.FuelRecordResponse;
import com.odolog.app.fuel.dto.response.summary.FuelSummaryResponse;
import com.odolog.app.fuel.repository.jpa.FuelRecordRepository;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.vehicle.service.application.VehicleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class FuelRecordService {

    /** 정렬 고정. sort 파라미터 무시 — 정렬이 곧 연비 계산의 전제 */
    private static final Sort FIXED_SORT = Sort.by(Sort.Direction.DESC, "odometer", "id");

    private final FuelRecordRepository fuelRecordRepository;
    private final VehicleService vehicleService;

    public FuelRecordService(FuelRecordRepository fuelRecordRepository, VehicleService vehicleService) {
        this.fuelRecordRepository = fuelRecordRepository;
        this.vehicleService = vehicleService;
    }

    @Transactional
    public FuelRecordResponse register(Long requesterId, Long vehicleId,
                                       FuelRecordRegisterRequest request) {
        Vehicle vehicle = vehicleService.findOwnedVehicle(requesterId, vehicleId);

        FuelRecord record = fuelRecordRepository.save(new FuelRecord(
                vehicle, request.fueledAt(), request.odometer(),
                request.liters(), request.totalCost(), request.memo()));

        // 계기판 값이 더 최신이면 차량 쪽도 갱신
        vehicle.liftOdometerTo(request.odometer());

        return FuelRecordResponse.of(record, findPrevious(vehicleId, record.getOdometer()));
    }

    /**
     * 페이지당 쿼리 2번
     * 페이지 안쪽 행은 서로가 짝, 마지막 행의 짝만 다음 페이지에 있어 한 건 추가 조회 (N+1 방지)
     */
    public Page<FuelRecordResponse> findByVehicle(Long requesterId, Long vehicleId, Pageable pageable) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);

        Page<FuelRecord> page = fuelRecordRepository.findByVehicleId(vehicleId,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), FIXED_SORT));

        List<FuelRecord> items = page.getContent();
        if (items.isEmpty()) {
            return new PageImpl<>(List.of(), page.getPageable(), page.getTotalElements());
        }

        // 내림차순이라 맨 끝이 가장 오래된 기록. 그것의 직전 한 건
        FuelRecord beforePage = findPrevious(vehicleId, items.get(items.size() - 1).getOdometer());

        List<FuelRecordResponse> responses = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            FuelRecord previous = (i == items.size() - 1) ? beforePage : items.get(i + 1);
            responses.add(FuelRecordResponse.of(items.get(i), previous));
        }

        return new PageImpl<>(responses, page.getPageable(), page.getTotalElements());
    }


    @Transactional
    public FuelRecordResponse update(Long requesterId, Long vehicleId, Long recordId,
                                     FuelRecordUpdateRequest request) {
        FuelRecord record = findRecordInVehicle(requesterId, vehicleId, recordId);

        if (request.fueledAt() != null) record.changeFueledAt(request.fueledAt());
        if (request.odometer() != null) {
            record.changeOdometer(request.odometer());
            // 수정에도 같은 규칙. 없으면 자리수 오타를 고쳐도 차량이 틀린 채로 남음
            record.getVehicle().liftOdometerTo(request.odometer());
        }
        if (request.liters() != null) record.changeLiters(request.liters());
        if (request.totalCost() != null) record.changeTotalCost(request.totalCost());
        if (request.memo() != null) record.changeMemo(request.memo());
        if (request.resetPoint() != null) record.changeResetPoint(request.resetPoint());

        return FuelRecordResponse.of(record, findPrevious(vehicleId, record.getOdometer()));
    }

    @Transactional
    public void delete(Long requesterId, Long vehicleId, Long recordId) {
        fuelRecordRepository.delete(findRecordInVehicle(requesterId, vehicleId, recordId));
    }

    public FuelSummaryResponse summary(Long requesterId, Long vehicleId) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);

        List<FuelRecord> records = fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(vehicleId);

        // 건수·비용·주유량은 전체 기준. 초기화 대상은 연비뿐
        int totalCost = 0;
        BigDecimal totalLiters = BigDecimal.ZERO;
        for (FuelRecord record : records) {
            totalCost += record.getTotalCost();
            totalLiters = totalLiters.add(record.getLiters());
        }

        FuelEfficiency efficiency = FuelEfficiency.of(records);

        // 오름차순이라 마지막이 최근
        Long latestId = records.isEmpty() ? null : records.get(records.size() - 1).getId();

        // 기준점이 여럿이면 최근 것 우선이라 뒤에서부터
        Long resetPointId = null;
        for (int i = records.size() - 1; i >= 0; i--) {
            if (records.get(i).isResetPoint()) {
                resetPointId = records.get(i).getId();
                break;
            }
        }

        return new FuelSummaryResponse(records.size(), totalCost, totalLiters,
                efficiency.distance(), efficiency.average(), latestId, resetPointId,
                FuelAnomaly.longSegmentCount(records), efficiency.excludedSegments());
    }



    private FuelRecord findPrevious(Long vehicleId, int odometer) {
        return fuelRecordRepository
                .findTopByVehicleIdAndOdometerLessThanOrderByOdometerDescIdDesc(vehicleId, odometer)
                .orElse(null);
    }

    private FuelRecord findRecordInVehicle(Long requesterId, Long vehicleId, Long recordId) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);

        return fuelRecordRepository.findByIdAndVehicleId(recordId, vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 주유 기록입니다: " + recordId));
    }
}
