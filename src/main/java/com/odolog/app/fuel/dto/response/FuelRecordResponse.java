package com.odolog.app.fuel.dto.response;

import com.odolog.app.fuel.domain.FuelAnomaly;
import com.odolog.app.fuel.domain.FuelRecord;
import com.odolog.app.fuel.domain.FuelSegment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 저장값 + 계산값. 계산값은 조회 시 산출(직전 기록 변경에 자동 반영)
 * distance·efficiency null: 첫 기록, 기준점, 구간 미성립
 */
public record FuelRecordResponse(
        /** 공개 id(12자) */
        String id,
        LocalDate fueledAt,
        int odometer,
        BigDecimal liters,
        Integer totalCost,
        String currency,
        String memo,
        /** 연비 재계산 기준점 여부 */
        boolean resetPoint,
        /** 가득 채웠는지. 덜 채웠으면 다음 가득 주유의 구간에 합쳐 계산 */
        boolean fullTank,
        /** 이 주유량이 들어간 구간. 덜 채움·같은 주행거리 행의 안내가 계산과 같게 */
        FuelSegment.LitersUse litersUse,

        /** 리터당 단가(통화의 최소 단위). 총액 ÷ 리터 반올림. 둘 중 하나라도 없으면 null */
        Long pricePerLiter,
        /** 연비 구간의 거리(km). 구간이 없으면 직전 주유 이후 거리, 직전 없으면 null */
        Integer distance,
        /** 연비(km/L), 소수 2자리. 구간 미성립이면 null */
        BigDecimal efficiency,
        /** 물리적으로 불가능한 연비(50 초과·2 미만) = 입력 오류 */
        boolean efficiencySuspicious,

        /** 주유 기록 누락 의심 구간. 평균에서도 제외된 구간 */
        boolean missingRecordSuspected,

        /** 주행거리는 더 큰데 날짜가 직전 기록보다 이름. 둘 중 하나가 잘못 적힌 것 */
        boolean dateBeforePrevious
) {

    /**
     * @param segment 이 기록이 닫는 연비 구간(FuelSegment). 평균과 같은 계산. 없으면 연비 없음
     * @param litersUse 이 기록의 주유량이 들어간 구간. 같은 순회의 결과
     */
    public static FuelRecordResponse of(FuelRecord record, FuelRecord previous, FuelSegment segment,
                                          FuelSegment.LitersUse litersUse, FuelAnomaly.Baseline baseline) {
        Integer distance = null;
        BigDecimal efficiency = null;

        if (segment != null) {
            // 덜 채운 주유를 지나 왔으면 직전 가득부터의 거리
            distance = segment.distance();
            efficiency = segment.efficiency();
        } else if (!record.isResetPoint() && previous != null
                && record.getOdometer() > previous.getOdometer()) {
            // 연비는 없어도 거리는 표시. 기준점은 직전과 연결 끊김
            distance = record.getOdometer() - previous.getOdometer();
        }

        return new FuelRecordResponse(
                record.getPublicId(),
                record.getFueledAt(),
                record.getOdometer(),
                record.getLiters(),
                record.getTotalCost(),
                record.getCurrency(),
                record.getMemo(),
                record.isResetPoint(),
                record.isFullTank(),
                litersUse,
                pricePerLiter(record),
                distance,
                efficiency,
                FuelAnomaly.isImpossible(efficiency),
                // 불가능한 값 표시 우선. 한 행에 표시 하나
                !FuelAnomaly.isImpossible(efficiency)
                        && distance != null
                        && baseline.suspectsMissingRecord(distance, efficiency),
                previous != null && record.getFueledAt().isBefore(previous.getFueledAt())
        );
    }

    /** 총액과 주유량이 모두 있을 때만. long 인 이유: 금액 상한 ÷ 0.01L 이 int 를 넘음 */
    private static Long pricePerLiter(FuelRecord record) {
        if (record.getTotalCost() == null || record.getLiters() == null
                || record.getLiters().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        return BigDecimal.valueOf(record.getTotalCost())
                .divide(record.getLiters(), 0, RoundingMode.HALF_UP)
                .longValue();
    }
}
