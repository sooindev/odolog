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

    /** 추이에 그릴 점 개수. 월별 차트 12칸과 같은 눈금 */
    private static final int TREND_POINTS = 12;

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
                request.liters(), request.totalCost(), blankToNull(request.memo())));

        // 계기판 값이 더 최신이면 차량 쪽도 갱신
        vehicle.liftOdometerTo(request.odometer());

        return FuelRecordResponse.of(record, findPrevious(vehicleId, record),
                baselineOf(vehicleId));
    }

    /**
     * 페이지당 쿼리 3번
     * 페이지 안쪽 행은 서로가 짝, 마지막 행의 짝만 다음 페이지에 있어 한 건 추가 조회 (N+1 방지)
     *
     * 세 번째는 '평소 구간'을 구하는 전체 조회다. 페이지 안에서만 중앙값을 내면
     * 같은 기록이 1페이지와 2페이지에서 다르게 판정된다 — 기준은 이력 전체라야 한 벌이다
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
        FuelRecord beforePage = findPrevious(vehicleId, items.get(items.size() - 1));

        FuelAnomaly.Baseline baseline = baselineOf(vehicleId);

        List<FuelRecordResponse> responses = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            FuelRecord previous = (i == items.size() - 1) ? beforePage : items.get(i + 1);
            responses.add(FuelRecordResponse.of(items.get(i), previous, baseline));
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
        // 비우기가 값보다 먼저. 둘 다 왔다면 비우려는 뜻으로 읽는다 (DTO 주석 참고)
        if (Boolean.TRUE.equals(request.clearLiters())) {
            record.changeLiters(null);
        } else if (request.liters() != null) {
            record.changeLiters(request.liters());
        }
        if (Boolean.TRUE.equals(request.clearTotalCost())) {
            record.changeTotalCost(null);
        } else if (request.totalCost() != null) {
            record.changeTotalCost(request.totalCost());
        }
        if (request.memo() != null) record.changeMemo(blankToNull(request.memo()));
        if (request.resetPoint() != null) record.changeResetPoint(request.resetPoint());

        return FuelRecordResponse.of(record, findPrevious(vehicleId, record),
                baselineOf(vehicleId));
    }

    @Transactional
    public void delete(Long requesterId, Long vehicleId, Long recordId) {
        fuelRecordRepository.delete(findRecordInVehicle(requesterId, vehicleId, recordId));
    }

    public FuelSummaryResponse summary(Long requesterId, Long vehicleId) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);

        List<FuelRecord> records = fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(vehicleId);

        // 건수·비용·주유량은 전체 기준. 초기화 대상은 연비뿐
        long totalCost = 0;
        BigDecimal totalLiters = BigDecimal.ZERO;
        for (FuelRecord record : records) {
            totalCost += record.totalCostOrZero();
            // 주유량은 BigDecimal 이라 더하는 모양이 달라 여기서 직접 거른다
            if (record.getLiters() != null) {
                totalLiters = totalLiters.add(record.getLiters());
            }
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

        // 빠진 구간 수를 여기서 따로 세지 않는다 — 평균에서 뺀 바로 그 개수라야
        // "N곳을 뺐습니다" 가 참이 된다
        return new FuelSummaryResponse(records.size(), totalCost, totalLiters,
                efficiency.distance(), efficiency.average(), latestId, resetPointId,
                efficiency.missingSegments(), efficiency.excludedSegments(),
                // 이미 읽어 둔 records 로 만든다 — 추가 쿼리 없음
                FuelEfficiency.trend(records, TREND_POINTS).stream()
                        .map(point -> new FuelSummaryResponse.TrendPoint(
                                point.fueledAt(), point.efficiency()))
                        .toList());
    }



    /**
     * 빈 문자열은 "없음" 으로 저장한다
     * 그대로 두면 "없음" 이 null 과 '' 두 모양이 되고, 내보낸 JSON 에도 그대로 나간다.
     * UserService 가 phone 에 쓰는 규칙과 같다
     */
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    /** 그 차량의 평소 구간. 목록·등록·수정이 같은 기준을 써야 같은 행이 같은 말을 한다 */
    private FuelAnomaly.Baseline baselineOf(Long vehicleId) {
        return FuelAnomaly.baselineOf(
                fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(vehicleId));
    }

    private FuelRecord findPrevious(Long vehicleId, FuelRecord record) {
        return fuelRecordRepository
                .findPrevious(vehicleId, record.getOdometer(), record.getId())
                .orElse(null);
    }

    private FuelRecord findRecordInVehicle(Long requesterId, Long vehicleId, Long recordId) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);

        return fuelRecordRepository.findByIdAndVehicleId(recordId, vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 주유 기록입니다: " + recordId));
    }
}
