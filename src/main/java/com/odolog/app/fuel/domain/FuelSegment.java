package com.odolog.app.fuel.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 직전 기록 → 이번 기록 구간. 평균 연비·추이·평소 구간 기준이 같은 규칙으로 순회
 * i 번째 주유량 = i-1 → i 구간 연료. 첫 기록의 주유량은 어느 구간에도 안 들어감
 * 같은 주행거리에 이어 넣은 주유는 그 묶음의 첫 기록 구간에 더함
 *
 * @param to         구간이 끝나는 기록
 * @param distance   구간 거리(km), 양수
 * @param liters     구간 연료(L), 양수
 * @param efficiency km/L, 소수 2자리
 */
public record FuelSegment(FuelRecord to, int distance, BigDecimal liters, BigDecimal efficiency) {

    /**
     * 연비를 계산할 수 있는 구간만. 거리 0 이하·주유량 없음 제외, 기준점은 직전과 연결 끊김
     *
     * @param records 주행거리 오름차순 정렬된 한 차량의 주유 기록
     */
    static List<FuelSegment> between(List<FuelRecord> records) {
        List<FuelSegment> segments = new ArrayList<>();

        for (int i = 1; i < records.size(); i++) {
            FuelRecord to = records.get(i);
            if (to.isResetPoint()) {
                continue;
            }

            int distance = to.getOdometer() - records.get(i - 1).getOdometer();
            if (distance <= 0) {
                continue;
            }

            BigDecimal used = litersAt(records, i);
            if (used == null || used.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            segments.add(new FuelSegment(to, distance, used, divide(distance, used)));
        }

        return segments;
    }

    /**
     * 뒤이어 같은 주행거리에 넣은 기록이 있는 기록의 id → 묶음 주유량 합(하나라도 비면 null)
     * 묶음이 아니면 키가 없음. 목록 행이 구간 계산과 같은 양으로 나누게
     *
     * @param records 주행거리 오름차순 정렬된 한 차량의 주유 기록
     */
    public static Map<Long, BigDecimal> splitFills(List<FuelRecord> records) {
        Map<Long, BigDecimal> totals = new HashMap<>();

        for (int i = 0; i + 1 < records.size(); i++) {
            FuelRecord next = records.get(i + 1);
            if (next.getOdometer() == records.get(i).getOdometer() && !next.isResetPoint()) {
                totals.put(records.get(i).getId(), litersAt(records, i));
            }
        }

        return totals;
    }

    /**
     * i 번째 기록과 뒤이어 같은 주행거리에 넣은 기록의 주유량 합. 나눠 넣은 주유는 한 번으로
     * 하나라도 비었으면 null. 모르는 양을 빼고 나누면 연비가 부풀려짐
     */
    private static BigDecimal litersAt(List<FuelRecord> records, int i) {
        FuelRecord first = records.get(i);
        BigDecimal sum = first.getLiters();

        for (int j = i + 1; j < records.size() && sum != null; j++) {
            FuelRecord next = records.get(j);
            // 기준점은 거기서부터 새로 셈
            if (next.getOdometer() != first.getOdometer() || next.isResetPoint()) {
                break;
            }
            sum = next.getLiters() == null ? null : sum.add(next.getLiters());
        }

        return sum;
    }

    static BigDecimal divide(int distance, BigDecimal liters) {
        return BigDecimal.valueOf(distance).divide(liters, 2, RoundingMode.HALF_UP);
    }
}
