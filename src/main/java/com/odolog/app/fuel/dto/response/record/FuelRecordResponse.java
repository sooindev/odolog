package com.odolog.app.fuel.dto.response.record;

import com.odolog.app.fuel.domain.calculation.FuelAnomaly;
import com.odolog.app.fuel.domain.entity.FuelRecord;

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
        String memo,
        /** 연비 재계산 기준점 여부 */
        boolean resetPoint,

        /** 리터당 단가(원). 총액 ÷ 리터 반올림. 둘 중 하나라도 없으면 null */
        Integer pricePerLiter,
        /** 직전 주유 이후 거리(km). 직전 없으면 null */
        Integer distance,
        /** 연비(km/L), 소수 2자리. 구간 미성립이면 null */
        BigDecimal efficiency,
        /** 물리적으로 불가능한 연비(50 초과·2 미만) = 입력 오류 */
        boolean efficiencySuspicious,

        /** 주유 기록 누락 의심 구간. 평균에서도 제외된 구간 */
        boolean missingRecordSuspected
) {

    public static FuelRecordResponse of(FuelRecord record, FuelRecord previous,
                                          FuelAnomaly.Baseline baseline) {
        Integer distance = null;
        BigDecimal efficiency = null;

        // 기준점은 직전과 연결 끊김
        if (!record.isResetPoint() && previous != null
                && record.getOdometer() > previous.getOdometer()) {
            distance = record.getOdometer() - previous.getOdometer();

            // 거리와 연비는 조건 분리. 주유량이 없어도 거리는 표시
            if (record.getLiters() != null) {
                // 단순법. 가득 채우지 않은 주유가 섞이면 그 구간만 높게 나옴
                efficiency = BigDecimal.valueOf(distance)
                        .divide(record.getLiters(), 2, RoundingMode.HALF_UP);
            }
        }

        return new FuelRecordResponse(
                record.getPublicId(),
                record.getFueledAt(),
                record.getOdometer(),
                record.getLiters(),
                record.getTotalCost(),
                record.getMemo(),
                record.isResetPoint(),
                pricePerLiter(record),
                distance,
                efficiency,
                FuelAnomaly.isImpossible(efficiency),
                // 불가능한 값 표시 우선. 한 행에 표시 하나
                !FuelAnomaly.isImpossible(efficiency)
                        && distance != null
                        && baseline.suspectsMissingRecord(distance, efficiency)
        );
    }

    /** 총액과 주유량이 모두 있을 때만 */
    private static Integer pricePerLiter(FuelRecord record) {
        if (record.getTotalCost() == null || record.getLiters() == null
                || record.getLiters().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        return BigDecimal.valueOf(record.getTotalCost())
                .divide(record.getLiters(), 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
