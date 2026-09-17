package com.odolog.app.fuel.dto.response.record;

import com.odolog.app.fuel.domain.calculation.FuelAnomaly;
import com.odolog.app.fuel.domain.entity.FuelRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 저장값 + 계산값
 * 계산값을 저장하지 않는 이유 — 직전 기록이 바뀌면 뒤따르는 기록을 전부 다시 써야 함
 * distance / efficiency 가 null 인 경우: 첫 기록, 기준점, 구간 미성립
 */
public record FuelRecordResponse(
        Long id,
        LocalDate fueledAt,
        int odometer,
        BigDecimal liters,
        int totalCost,
        String memo,
        /** 연비 재계산 기준점인지 */
        boolean resetPoint,

        /** 리터당 단가(원). 총액 ÷ 리터, 표시용 반올림 */
        int pricePerLiter,
        /** 직전 주유 이후 거리(km). 직전 없으면 null */
        Integer distance,
        /** 연비(km/L), 소수 2자리. 구간 미성립이면 null */
        BigDecimal efficiency,
        /**
         * 물리적으로 불가능한 연비인지(50 초과 · 2 미만) = 입력 오류
         * 빠진 기록은 여기서 못 잡음 — 요약의 longSegmentCount 담당
         */
        boolean efficiencySuspicious
) {

    public static FuelRecordResponse of(FuelRecord record, FuelRecord previous) {
        Integer distance = null;
        BigDecimal efficiency = null;

        // 기준점은 직전과의 연결을 끊으므로 자기 구간 연비도 없음 (첫 기록과 같은 처지)
        if (!record.isResetPoint() && previous != null
                && record.getOdometer() > previous.getOdometer()) {
            distance = record.getOdometer() - previous.getOdometer();
            // 단순법. 가득 채우지 않은 주유가 섞이면 그 구간만 높게 나옴
            efficiency = BigDecimal.valueOf(distance)
                    .divide(record.getLiters(), 2, RoundingMode.HALF_UP);
        }

        return new FuelRecordResponse(
                record.getId(),
                record.getFueledAt(),
                record.getOdometer(),
                record.getLiters(),
                record.getTotalCost(),
                record.getMemo(),
                record.isResetPoint(),
                pricePerLiter(record),
                distance,
                efficiency,
                FuelAnomaly.isImpossible(efficiency)
        );
    }

    private static int pricePerLiter(FuelRecord record) {
        return BigDecimal.valueOf(record.getTotalCost())
                .divide(record.getLiters(), 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
