package com.odolog.app.fuel;

import com.odolog.app.common.exception.ErrorCode;
import com.odolog.app.common.exception.type.ResourceNotFoundException;
import com.odolog.app.fuel.domain.calculation.FuelAnomaly;
import com.odolog.app.fuel.domain.calculation.FuelEfficiency;
import com.odolog.app.fuel.domain.FuelRecord;
import com.odolog.app.fuel.dto.request.FuelRecordRegisterRequest;
import com.odolog.app.fuel.dto.request.FuelRecordUpdateRequest;
import com.odolog.app.fuel.dto.response.FuelRecordResponse;
import com.odolog.app.fuel.dto.response.FuelSummaryResponse;
import com.odolog.app.vehicle.Vehicle;
import com.odolog.app.user.service.UserToday;
import com.odolog.app.vehicle.VehicleService;
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

    /** 추이 점 개수. 월별 차트 12칸과 동일 */
    private static final int TREND_POINTS = 12;

    /** 정렬 고정. sort 파라미터 무시, 연비 계산의 전제 */
    private static final Sort FIXED_SORT = Sort.by(Sort.Direction.DESC, "odometer", "id");

    private final FuelRecordRepository fuelRecordRepository;
    private final VehicleService vehicleService;
    private final UserToday userToday;

    public FuelRecordService(FuelRecordRepository fuelRecordRepository, VehicleService vehicleService,
                             UserToday userToday) {
        this.fuelRecordRepository = fuelRecordRepository;
        this.vehicleService = vehicleService;
        this.userToday = userToday;
    }

    @Transactional
    public FuelRecordResponse register(Long requesterId, String vehicleId,
                                       FuelRecordRegisterRequest request) {
        Vehicle vehicle = vehicleService.findOwnedVehicleForUpdate(requesterId, vehicleId);
        Long id = vehicle.getId();
        userToday.rejectFuture(requesterId, request.fueledAt(), "fueledAt");

        // 통화는 기록 시점의 사용자 설정
        FuelRecord record = fuelRecordRepository.save(new FuelRecord(
                vehicle, request.fueledAt(), request.odometer(),
                request.liters(), request.totalCost(), vehicle.getOwner().getCurrency(),
                blankToNull(request.memo())));

        // 계기판 값이 더 크면 차량 주행거리도 갱신
        vehicle.liftOdometerTo(request.odometer());

        return FuelRecordResponse.of(record, findPrevious(id, record), baselineOf(id));
    }

    /**
     * 페이지당 쿼리 5번(차량·페이지·개수·직전 1건·평소 구간). 마지막 행의 짝만 추가 조회
     * 평소 구간은 이력 전체 기준. 페이지마다 판정이 달라지는 문제 방지
     */
    public Page<FuelRecordResponse> findByVehicle(Long requesterId, String vehicleId, Pageable pageable) {
        Long id = vehicleService.findOwnedVehicle(requesterId, vehicleId).getId();

        Page<FuelRecord> page = fuelRecordRepository.findByVehicleId(id,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), FIXED_SORT));

        List<FuelRecord> items = page.getContent();
        if (items.isEmpty()) {
            return new PageImpl<>(List.of(), page.getPageable(), page.getTotalElements());
        }

        // 내림차순의 마지막 = 가장 오래된 기록. 그 직전 한 건
        FuelRecord beforePage = findPrevious(id, items.get(items.size() - 1));

        FuelAnomaly.Baseline baseline = baselineOf(id);

        List<FuelRecordResponse> responses = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            FuelRecord previous = (i == items.size() - 1) ? beforePage : items.get(i + 1);
            responses.add(FuelRecordResponse.of(items.get(i), previous, baseline));
        }

        return new PageImpl<>(responses, page.getPageable(), page.getTotalElements());
    }


    @Transactional
    public FuelRecordResponse update(Long requesterId, String vehicleId, String recordId,
                                     FuelRecordUpdateRequest request) {
        FuelRecord record = findRecordInVehicle(requesterId, vehicleId, recordId);
        userToday.rejectFuture(requesterId, request.fueledAt(), "fueledAt");

        if (request.fueledAt() != null) record.changeFueledAt(request.fueledAt());
        if (request.odometer() != null) {
            record.changeOdometer(request.odometer());
            // 수정에도 같은 규칙. 자리수 오타 정정 반영
            record.getVehicle().liftOdometerTo(request.odometer());
        }
        // 비움이 값보다 우선
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
        Long id = record.getVehicle().getId();
        if (request.resetPoint() != null) {
            changeResetPoint(id, record, request.resetPoint());
        }

        return FuelRecordResponse.of(record, findPrevious(id, record), baselineOf(id));
    }

    /**
     * 기준점은 차량당 하나. 설정·해제 모두 다른 기준점을 지움
     * 남겨 두면 최신 것을 해제하거나 지웠을 때 옛 기준점이 말없이 되살아남
     */
    private void changeResetPoint(Long vehicleId, FuelRecord target, boolean resetPoint) {
        for (FuelRecord other : fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(vehicleId)) {
            if (other != target && other.isResetPoint()) {
                other.changeResetPoint(false);
            }
        }
        target.changeResetPoint(resetPoint);
    }

    @Transactional
    public void delete(Long requesterId, String vehicleId, String recordId) {
        fuelRecordRepository.delete(findRecordInVehicle(requesterId, vehicleId, recordId));
    }

    /** 한 차량의 주유 기록 전부 삭제. 차량 삭제 조율 전용, 소유 확인은 부르는 쪽 */
    @Transactional
    public void deleteAllOf(Long vehicleId) {
        fuelRecordRepository.deleteByVehicleId(vehicleId);
    }

    public FuelSummaryResponse summary(Long requesterId, String vehicleId) {
        Vehicle vehicle = vehicleService.findOwnedVehicle(requesterId, vehicleId);
        Long id = vehicle.getId();
        String currency = vehicle.getOwner().getCurrency();

        List<FuelRecord> records = fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(id);

        // 건수·주유량은 전체 기준. 초기화 대상은 연비만
        // 비용은 사용자 통화만. 다른 통화는 더할 수 없어 건수만 공개
        long totalCost = 0;
        int otherCurrency = 0;
        BigDecimal totalLiters = BigDecimal.ZERO;
        for (FuelRecord record : records) {
            if (currency.equals(record.getCurrency())) {
                totalCost += record.totalCostOrZero();
            } else if (record.getTotalCost() != null) {
                // 금액이 비어 있으면 뺀 것이 없어 세지 않음
                otherCurrency++;
            }
            // BigDecimal 합산이라 null 직접 제외
            if (record.getLiters() != null) {
                totalLiters = totalLiters.add(record.getLiters());
            }
        }

        FuelEfficiency efficiency = FuelEfficiency.of(records);

        // 주행은 기록·주유량·비용과 같은 전체 기준. 초기화·제외 구간과 무관(B-77)
        Integer totalDistance = records.size() < 2 ? null
                : records.get(records.size() - 1).getOdometer() - records.get(0).getOdometer();

        // 오름차순의 마지막 = 최근
        String latestId = records.isEmpty() ? null : records.get(records.size() - 1).getPublicId();

        // 기준점이 여럿이면 최근 것 우선
        String resetPointId = null;
        for (int i = records.size() - 1; i >= 0; i--) {
            if (records.get(i).isResetPoint()) {
                resetPointId = records.get(i).getPublicId();
                break;
            }
        }

        // 빠진 구간 수는 평균에서 뺀 개수 그대로 사용
        return new FuelSummaryResponse(records.size(), totalCost, currency, otherCurrency, totalLiters,
                totalDistance, efficiency.average(), latestId, resetPointId,
                efficiency.missingSegments(), efficiency.excludedSegments(),
                // 이미 읽은 records 재사용. 추가 쿼리 없음
                FuelEfficiency.trend(records, TREND_POINTS).stream()
                        .map(point -> new FuelSummaryResponse.TrendPoint(
                                point.fueledAt(), point.efficiency()))
                        .toList());
    }



    /** 빈 문자열은 null */
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    /** 그 차량의 평소 구간. 목록·등록·수정 공통 기준 */
    private FuelAnomaly.Baseline baselineOf(Long vehicleId) {
        return FuelAnomaly.baselineOf(
                fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(vehicleId));
    }

    private FuelRecord findPrevious(Long vehicleId, FuelRecord record) {
        return fuelRecordRepository
                .findPrevious(vehicleId, record.getOdometer(), record.getId())
                .orElse(null);
    }

    private FuelRecord findRecordInVehicle(Long requesterId, String vehicleId, String recordId) {
        Long id = vehicleService.findOwnedVehicleForUpdate(requesterId, vehicleId).getId();

        return fuelRecordRepository.findByPublicIdAndVehicleId(recordId, id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.FUEL_RECORD_NOT_FOUND, "존재하지 않는 주유 기록입니다: " + recordId));
    }
}
