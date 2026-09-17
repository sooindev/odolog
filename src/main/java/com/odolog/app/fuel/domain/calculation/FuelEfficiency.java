package com.odolog.app.fuel.domain.calculation;

import com.odolog.app.fuel.domain.entity.FuelRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 평균 연비 공식. 차량 상세와 홈 요약이 공유
 * 공식이 두 벌이면 화면마다 다른 연비가 뜸
 *
 * @param distance 구간 거리(km). 계산 불가면 null
 * @param average  km/L, 소수 2자리. 계산 불가면 null
 */
public record FuelEfficiency(Integer distance, BigDecimal average) {

    private static final FuelEfficiency NONE = new FuelEfficiency(null, null);

    /**
     * @param all 주행거리 오름차순 정렬된 한 차량의 주유 기록
     */
    public static FuelEfficiency of(List<FuelRecord> all) {
        List<FuelRecord> records = sinceResetPoint(all);

        // 두 건은 있어야 사이 거리가 생김
        if (records.size() < 2) {
            return NONE;
        }

        FuelRecord first = records.get(0);
        int distance = records.get(records.size() - 1).getOdometer() - first.getOdometer();

        BigDecimal liters = records.stream()
                .map(FuelRecord::getLiters)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                // 첫 주유량 제외 — 그건 첫 기록 이전 구간의 연료라 거리와 짝이 안 맞음
                .subtract(first.getLiters());

        if (distance <= 0 || liters.compareTo(BigDecimal.ZERO) <= 0) {
            return NONE;
        }

        return new FuelEfficiency(distance,
                BigDecimal.valueOf(distance).divide(liters, 2, RoundingMode.HALF_UP));
    }

    /**
     * 최근 기준점 이후만 남기기. 기준점 없으면 전부
     * 오름차순이라 뒤에서부터 — 여럿이면 마지막 것 우선
     */
    private static List<FuelRecord> sinceResetPoint(List<FuelRecord> records) {
        for (int i = records.size() - 1; i >= 0; i--) {
            if (records.get(i).isResetPoint()) {
                return records.subList(i, records.size());
            }
        }
        return records;
    }
}
