package com.odolog.app.fuel.service.application;

import com.odolog.app.common.exception.type.ResourceNotFoundException;
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

    /**
     * 목록 정렬을 주행거리 내림차순으로 <b>고정</b>한다. 클라이언트의 sort 파라미터를 무시한다.
     *
     * <p>연비가 "바로 앞 행과의 주행거리 차이"로 계산되기 때문이다. 총액 순으로 정렬해 버리면
     * 옆 행이 직전 주유가 아니게 되어 연비가 조용히 엉뚱한 값이 된다.
     * 차량·정비 목록이 sort 를 허용하는 것과 다른 이유가 여기 있다 — 거기서는 정렬이 표시 순서일
     * 뿐이지만, 여기서는 정렬이 곧 계산의 전제다.
     */
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

        // 주유할 때 계기판을 보고 적는 값이라, 차량의 현재 주행거리보다 크면 그쪽이 더 최신이다.
        vehicle.liftOdometerTo(request.odometer());

        return FuelRecordResponse.of(record, findPrevious(vehicleId, record.getOdometer()));
    }

    /**
     * 페이지 하나를 채우는 데 쿼리 2번만 쓴다.
     *
     * <p>각 행의 연비는 바로 앞 행(더 오래된 주유)이 있어야 나온다. 페이지 안쪽 행들은 서로가
     * 서로의 짝이라 추가 조회가 필요 없지만, <b>페이지의 마지막 행만은 짝이 다음 페이지에 있다.</b>
     * 그래서 그 한 건만 따로 가져온다. 행마다 직전을 조회하면 N+1 이 된다.
     */
    public Page<FuelRecordResponse> findByVehicle(Long requesterId, Long vehicleId, Pageable pageable) {
        vehicleService.findOwnedVehicle(requesterId, vehicleId);

        Page<FuelRecord> page = fuelRecordRepository.findByVehicleId(vehicleId,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), FIXED_SORT));

        List<FuelRecord> items = page.getContent();
        if (items.isEmpty()) {
            return new PageImpl<>(List.of(), page.getPageable(), page.getTotalElements());
        }

        // 페이지 맨 끝이 가장 오래된 기록이다(내림차순이므로). 그것의 직전 한 건.
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
            // 등록과 같은 규칙을 적용한다. 전에는 등록에만 있어서, 주행거리를 10000 으로 잘못
            // 넣고 100000 으로 고치면 기록만 고쳐지고 차량은 틀린 채로 남았다.
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

        // 건수·비용·주유량은 **전체**를 센다. 초기화는 연비를 다시 세는 것이지 지출을
        // 없던 일로 만드는 게 아니다.
        int totalCost = 0;
        BigDecimal totalLiters = BigDecimal.ZERO;
        for (FuelRecord record : records) {
            totalCost += record.getTotalCost();
            totalLiters = totalLiters.add(record.getLiters());
        }

        FuelEfficiency efficiency = FuelEfficiency.of(records);

        // 목록은 주행거리 오름차순이라 마지막이 가장 최근이다.
        Long latestId = records.isEmpty() ? null : records.get(records.size() - 1).getId();

        // 기준점이 여럿이면 가장 최근 것이 적용된다 — 뒤에서부터 찾는다.
        Long resetPointId = null;
        for (int i = records.size() - 1; i >= 0; i--) {
            if (records.get(i).isResetPoint()) {
                resetPointId = records.get(i).getId();
                break;
            }
        }

        return new FuelSummaryResponse(records.size(), totalCost, totalLiters,
                efficiency.distance(), efficiency.average(), latestId, resetPointId);
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
