package com.odolog.app.fuel.dto.response.record;

import com.odolog.app.fuel.domain.entity.FuelRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 저장된 값(위 5개)과 계산된 값(아래 3개)이 한 응답에 섞여 있다.
 *
 * <p>계산 값을 DB 에 저장하지 않는 이유: 직전 기록이 수정되거나 삭제되면 연비가 달라지는데,
 * 저장해 두면 그때마다 뒤따르는 기록을 전부 다시 써야 한다. 읽을 때 계산하면 언제나 맞다.
 *
 * <p>distance / efficiency 가 null 인 경우는 셋이다: 첫 기록이라 직전이 없을 때,
 * 주행거리가 직전보다 작거나 같아 구간이 성립하지 않을 때(잘못 입력한 데이터),
 * 그리고 그 둘의 결과로 거리를 못 구했을 때.
 */
public record FuelRecordResponse(
        Long id,
        LocalDate fueledAt,
        int odometer,
        BigDecimal liters,
        int totalCost,
        String memo,

        /** 리터당 단가(원). 총액 ÷ 리터를 반올림한 표시용 값이다. */
        int pricePerLiter,
        /** 직전 주유 이후 달린 거리(km). 직전 기록이 없으면 null. */
        Integer distance,
        /** 연비(km/L), 소수 둘째 자리까지. 구간이 성립하지 않으면 null. */
        BigDecimal efficiency
) {

    public static FuelRecordResponse of(FuelRecord record, FuelRecord previous) {
        Integer distance = null;
        BigDecimal efficiency = null;

        if (previous != null && record.getOdometer() > previous.getOdometer()) {
            distance = record.getOdometer() - previous.getOdometer();
            // 단순법: 이번에 넣은 양으로 이번 구간을 나눈다.
            // 가득 채우지 않은 주유가 섞이면 그 구간만 실제보다 높게 나온다.
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
                pricePerLiter(record),
                distance,
                efficiency
        );
    }

    private static int pricePerLiter(FuelRecord record) {
        return BigDecimal.valueOf(record.getTotalCost())
                .divide(record.getLiters(), 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
