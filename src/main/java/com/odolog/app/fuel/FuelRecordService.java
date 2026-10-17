package com.odolog.app.fuel;

import com.odolog.app.common.exception.ErrorCode;
import com.odolog.app.common.exception.type.ResourceNotFoundException;
import com.odolog.app.common.validation.InputText;
import com.odolog.app.fuel.domain.FuelAnomaly;
import com.odolog.app.fuel.domain.FuelEfficiency;
import com.odolog.app.fuel.domain.FuelRecord;
import com.odolog.app.fuel.domain.FuelSegment;
import com.odolog.app.fuel.dto.request.FuelRecordRegisterRequest;
import com.odolog.app.fuel.dto.request.FuelRecordUpdateRequest;
import com.odolog.app.fuel.dto.response.FuelRecordResponse;
import com.odolog.app.fuel.dto.response.FuelSummaryResponse;
import com.odolog.app.user.UserToday;
import com.odolog.app.vehicle.VehicleService;
import com.odolog.app.vehicle.domain.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        userToday.rejectOutOfRange(requesterId, request.fueledAt(), "fueledAt");

        // 통화는 기록 시점의 사용자 설정
        FuelRecord record = fuelRecordRepository.save(new FuelRecord(
                vehicle, request.fueledAt(), request.odometer(),
                request.liters(), request.totalCost(), vehicle.getOwner().getCurrency(),
                InputText.optional(request.memo())));

        // 덜 채웠다고 밝힌 경우만. 안 보내면 가득
        record.changeFullTank(!Boolean.FALSE.equals(request.fullTank()));

        // 계기판 값이 더 크면 차량 주행거리도 갱신
        vehicle.liftOdometerTo(request.odometer());

        return rowsOf(id).of(record, findPrevious(id, record));
    }

    /**
     * 페이지당 쿼리 5번(차량·페이지·개수·직전 1건·이력 전체). 마지막 행의 짝만 추가 조회
     * 평소 구간·나눠 넣은 묶음은 이력 전체 기준. 페이지마다 판정이 달라지는 문제 방지
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

        Rows rows = rowsOf(id);

        List<FuelRecordResponse> responses = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            FuelRecord previous = (i == items.size() - 1) ? beforePage : items.get(i + 1);
            responses.add(rows.of(items.get(i), previous));
        }

        return new PageImpl<>(responses, page.getPageable(), page.getTotalElements());
    }

    @Transactional
    public FuelRecordResponse update(Long requesterId, String vehicleId, String recordId,
                                     FuelRecordUpdateRequest request) {
        FuelRecord record = findRecordInVehicle(requesterId, vehicleId, recordId);
        userToday.rejectOutOfRange(requesterId, request.fueledAt(), "fueledAt");

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
        if (request.memo() != null) record.changeMemo(InputText.optional(request.memo()));
        if (request.fullTank() != null) record.changeFullTank(request.fullTank());
        Long id = record.getVehicle().getId();
        if (request.resetPoint() != null) {
            changeResetPoint(id, record, request.resetPoint());
        }

        return rowsOf(id).of(record, findPrevious(id, record));
    }

    /**
     * 기준점은 차량당 하나. 설정·해제 모두 다른 기준점을 지움
     * 남겨 두면 최신 것을 해제하거나 지웠을 때 옛 기준점이 말없이 되살아남
     * 기준점이 아닌 기록의 해제는 할 일 없음. 다른 기준점까지 지우면 연비가 말없이 전체 이력 기준으로 돌아감
     */
    private void changeResetPoint(Long vehicleId, FuelRecord target, boolean resetPoint) {
        if (!resetPoint && !target.isResetPoint()) {
            return;
        }
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
        // 하나도 안 적었으면 null. 0 은 "0원·0L 을 넣었다"는 다른 사실
        Long totalCost = null;
        int otherCurrency = 0;
        BigDecimal totalLiters = null;
        for (FuelRecord record : records) {
            if (record.getTotalCost() != null) {
                if (currency.equals(record.getCurrency())) {
                    totalCost = (totalCost == null ? 0L : totalCost) + record.getTotalCost();
                } else {
                    // 금액이 비어 있으면 뺀 것이 없어 세지 않음
                    otherCurrency++;
                }
            }
            if (record.getLiters() != null) {
                totalLiters = totalLiters == null ? record.getLiters() : totalLiters.add(record.getLiters());
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

    /** 이력 전체를 한 번 읽어 행 계산 기준을 만듦. 목록·등록·수정 공통 */
    private Rows rowsOf(Long vehicleId) {
        List<FuelRecord> all = fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(vehicleId);

        // 최근 기준점보다 앞선 기록. 평균 밖이라 "기록 빠짐?(평균에서 뺐다)" 을 붙이지 않음
        Set<Long> beforeResetPoint = new HashSet<>();
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i).isResetPoint()) {
                for (int j = 0; j < i; j++) {
                    beforeResetPoint.add(all.get(j).getId());
                }
                break;
            }
        }

        return new Rows(FuelAnomaly.baselineOf(all), FuelSegment.byClosingRecord(all),
                FuelSegment.litersUses(all), beforeResetPoint);
    }

    /**
     * 행 계산의 이력 전체 기준. 평소 구간·기록마다 닫는 연비 구간·기준점 앞 기록
     * 페이지만 보고 정하면 덜 채운 주유·나눠 넣은 주유가 쪽 경계에 걸릴 때 행마다 판정이 달라짐
     */
    private record Rows(FuelAnomaly.Baseline baseline, Map<Long, FuelSegment> segments,
                        Map<Long, FuelSegment.LitersUse> litersUses, Set<Long> beforeResetPoint) {

        FuelRecordResponse of(FuelRecord record, FuelRecord previous) {
            // 기준점 앞은 누락 판정 안 함. "확인 필요"(불가능한 값)는 입력 오류라 그대로
            FuelAnomaly.Baseline rowBaseline = beforeResetPoint.contains(record.getId())
                    ? FuelAnomaly.Baseline.NONE : baseline;
            return FuelRecordResponse.of(record, previous, segments.get(record.getId()),
                    litersUses.getOrDefault(record.getId(), FuelSegment.LitersUse.NONE), rowBaseline);
        }
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
