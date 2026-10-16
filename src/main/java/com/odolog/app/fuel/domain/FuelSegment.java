package com.odolog.app.fuel.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 직전 가득 주유 → 이번 가득 주유 구간. 평균 연비·추이·평소 구간·목록 행이 같은 규칙으로 순회
 * 구간 연료 = 그 사이 덜 채운 주유량 + 이번 주유량(같은 주행거리로 이어 넣은 양 포함)
 * 첫 기록·기준점의 주유량은 어느 구간에도 안 들어감. 시작이 덜 채운 주유면 그 구간은 낼 수 없음
 *
 * @param to         구간이 끝나는 기록(가득 주유)
 * @param distance   구간 거리(km), 양수
 * @param liters     구간 연료(L), 양수
 * @param efficiency km/L, 소수 2자리
 */
public record FuelSegment(FuelRecord to, int distance, BigDecimal liters, BigDecimal efficiency) {

    /**
     * 연비를 계산할 수 있는 구간만. 거리 0 이하·주유량을 모름·시작이 덜 채운 주유면 제외
     * 기준점은 직전과 연결을 끊고 거기서 새로 셈
     *
     * @param records 주행거리 오름차순 정렬된 한 차량의 주유 기록
     */
    static List<FuelSegment> between(List<FuelRecord> records) {
        List<FuelSegment> segments = new ArrayList<>();

        // 구간의 시작 기록과, 그 뒤 아직 구간에 넣지 않은 덜 채운 주유량(하나라도 모르면 null)
        int start = 0;
        BigDecimal partial = BigDecimal.ZERO;

        for (int i = 1; i < records.size(); i++) {
            FuelRecord record = records.get(i);

            if (record.isResetPoint()) {
                start = i;
                partial = BigDecimal.ZERO;
                continue;
            }
            if (!record.isFullTank()) {
                partial = partial == null || record.getLiters() == null ? null : partial.add(record.getLiters());
                continue;
            }

            FuelRecord from = records.get(start);
            int distance = record.getOdometer() - from.getOdometer();
            if (distance <= 0) {
                // 같은 주행거리로 이어 넣은 주유. 앞 구간을 닫을 때 litersAt 이 이미 셈
                continue;
            }

            BigDecimal closing = litersAt(records, i);
            BigDecimal used = partial == null || closing == null ? null : partial.add(closing);
            // 시작이 가득이어야 그 사이 넣은 양이 곧 쓴 양
            if (from.isFullTank() && used != null && used.compareTo(BigDecimal.ZERO) > 0) {
                segments.add(new FuelSegment(record, distance, used, divide(distance, used)));
            }

            // 같은 주행거리로 이어 넣은 기록까지 넘김. 그 양은 방금 구간에 들어감
            int last = i;
            while (last + 1 < records.size()
                    && records.get(last + 1).getOdometer() == record.getOdometer()
                    && !records.get(last + 1).isResetPoint()) {
                last++;
            }
            start = last;
            i = last;
            partial = BigDecimal.ZERO;
        }

        return segments;
    }

    /** 구간을 닫는 기록의 id → 구간. 목록 행이 평균과 같은 구간으로 연비를 보이게 */
    public static Map<Long, FuelSegment> byClosingRecord(List<FuelRecord> records) {
        Map<Long, FuelSegment> segments = new HashMap<>();
        for (FuelSegment segment : between(records)) {
            segments.put(segment.to().getId(), segment);
        }
        return segments;
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
