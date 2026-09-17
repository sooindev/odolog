package com.odolog.app.fuel.domain.calculation;

import com.odolog.app.fuel.domain.entity.FuelRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 평균 연비 공식. 차량 상세와 홈 요약이 **같은 것을 쓴다.**
 *
 * <p>전에는 두 서비스가 각자 계산했다. 공식이 두 벌이면 한쪽만 고쳤을 때 화면마다 다른 연비가
 * 뜨는데, 그건 사용자가 어느 쪽을 믿어야 할지 알 수 없는 상태다.
 * 엔티티도 서비스도 아닌 **값 계산**이라 domain 아래 따로 뒀다.
 *
 * @param distance 연비를 잰 구간의 거리(km). 계산할 수 없으면 null.
 * @param average  km/L, 소수 둘째 자리까지. 계산할 수 없으면 null.
 */
public record FuelEfficiency(Integer distance, BigDecimal average) {

    private static final FuelEfficiency NONE = new FuelEfficiency(null, null);

    /**
     * @param all 주행거리 오름차순으로 정렬된 한 차량의 주유 기록
     */
    public static FuelEfficiency of(List<FuelRecord> all) {
        List<FuelRecord> records = sinceResetPoint(all);

        // 두 건은 있어야 "그 사이를 달린 거리"가 생긴다.
        if (records.size() < 2) {
            return NONE;
        }

        FuelRecord first = records.get(0);
        int distance = records.get(records.size() - 1).getOdometer() - first.getOdometer();

        BigDecimal liters = records.stream()
                .map(FuelRecord::getLiters)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                // 첫 주유량은 그 이전 구간을 달린 연료라 우리가 아는 거리와 짝이 맞지 않는다.
                // 안 빼면 연비가 실제보다 낮게 나온다.
                .subtract(first.getLiters());

        if (distance <= 0 || liters.compareTo(BigDecimal.ZERO) <= 0) {
            return NONE;
        }

        return new FuelEfficiency(distance,
                BigDecimal.valueOf(distance).divide(liters, 2, RoundingMode.HALF_UP));
    }

    /**
     * 가장 최근 기준점부터의 구간만 남긴다. 기준점이 없으면 전부 그대로다.
     *
     * <p>오름차순 목록이라 <b>뒤에서부터</b> 찾는다 — 기준점이 여럿이면 마지막 것이 이긴다.
     * 기준점으로 찍힌 기록은 새로운 "첫 기록"이 되어 그 주유량이 계산에서 빠진다.
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
