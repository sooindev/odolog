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
 * 첫 기록·기준점의 주유량은 어느 구간에도 안 들어감. 시작 주행거리의 기록이 모두 덜 채움이면 그 구간은 낼 수 없음
 *
 * @param to         구간이 끝나는 기록(가득 주유)
 * @param distance   구간 거리(km), 양수
 * @param liters     구간 연료(L), 양수
 * @param efficiency km/L, 소수 2자리
 */
public record FuelSegment(FuelRecord to, int distance, BigDecimal liters, BigDecimal efficiency) {

    /** 기록의 주유량이 어느 구간에 들어갔는지. 목록 행의 안내가 계산과 같은 말을 하게 */
    public enum LitersUse {
        /** 이 기록이 닫는 구간 */
        CLOSING,
        /** 같은 주행거리에서 앞서 닫힌 구간(나눠 넣은 주유) */
        PREVIOUS,
        /** 뒤의 가득 주유가 닫는 구간. 아직 가득 주유가 없어도 이어 갈 수 있으면 여기 */
        NEXT,
        /** 어느 구간에도 안 들어감. 첫 기록·기준점·출발 전의 양·끊긴 구간 */
        NONE
    }

    /** 구간 목록과 기록마다의 주유량 쓰임. 한 번 순회로 둘 다 */
    record Walk(List<FuelSegment> segments, Map<Long, LitersUse> uses) {
    }

    /**
     * 연비를 계산할 수 있는 구간만. 주유량을 모름·출발할 때 가득이 아니었으면 제외
     * 기준점은 직전과 연결을 끊고 거기서 새로 셈
     *
     * @param records 주행거리 오름차순 정렬된 한 차량의 주유 기록
     */
    static List<FuelSegment> between(List<FuelRecord> records) {
        return walk(records).segments();
    }

    static Walk walk(List<FuelRecord> records) {
        List<FuelSegment> segments = new ArrayList<>();
        Map<Long, LitersUse> uses = new HashMap<>();

        // 구간의 시작 기록, 시작 주행거리에서 탱크가 가득이었는지, 그 뒤 아직 구간에 넣지 않은 덜 채운 주유량(하나라도 모르면 null)
        int start = 0;
        boolean startFull = records.isEmpty() || records.get(0).isFullTank();
        BigDecimal partial = BigDecimal.ZERO;
        // partial 에 들어간 기록. 구간이 닫히면 NEXT, 끊기면 NONE
        List<FuelRecord> pending = new ArrayList<>();

        for (int i = 1; i < records.size(); i++) {
            FuelRecord record = records.get(i);

            if (record.isResetPoint()) {
                start = i;
                // 같은 주행거리에 먼저 적은 가득 주유도 출발 상태. id 순서와 무관하게
                startFull = record.isFullTank() || fullBefore(records, i);
                partial = BigDecimal.ZERO;
                pending.clear();
                continue;
            }

            FuelRecord from = records.get(start);
            int distance = record.getOdometer() - from.getOdometer();
            if (distance <= 0) {
                // 시작과 같은 주행거리로 이어 넣은 주유. 출발 전의 양이라 구간에 안 넣음
                // 그중 하나라도 가득이면 출발할 때 탱크는 가득
                startFull = startFull || record.isFullTank();
                continue;
            }

            if (!record.isFullTank()) {
                partial = partial == null || record.getLiters() == null ? null : partial.add(record.getLiters());
                pending.add(record);
                continue;
            }

            // 같은 주행거리로 이어 넣은 기록까지. 그 양은 이번 구간에 들어감
            int last = i;
            while (last + 1 < records.size()
                    && records.get(last + 1).getOdometer() == record.getOdometer()
                    && !records.get(last + 1).isResetPoint()) {
                last++;
            }

            BigDecimal closing = litersAt(records, i);
            BigDecimal used = partial == null || closing == null ? null : partial.add(closing);
            // 시작이 가득이어야 그 사이 넣은 양이 곧 쓴 양
            if (startFull && used != null && used.compareTo(BigDecimal.ZERO) > 0) {
                segments.add(new FuelSegment(record, distance, used, divide(distance, used)));
                uses.put(record.getId(), LitersUse.CLOSING);
                for (int j = i + 1; j <= last; j++) {
                    uses.put(records.get(j).getId(), LitersUse.PREVIOUS);
                }
                for (FuelRecord counted : pending) {
                    uses.put(counted.getId(), LitersUse.NEXT);
                }
            }

            // 묶음이 가득 주유로 닫혔으니 다음 구간은 가득에서 출발. 묶음 끝 기록이 덜 채움이어도 같음
            start = last;
            startFull = true;
            i = last;
            partial = BigDecimal.ZERO;
            pending.clear();
        }

        // 아직 닫히지 않은 덜 채운 주유. 다음 가득 주유에서 셀 수 있을 때만 NEXT
        if (startFull && partial != null) {
            for (FuelRecord counted : pending) {
                uses.put(counted.getId(), LitersUse.NEXT);
            }
        }

        return new Walk(segments, uses);
    }

    /** 기록마다 주유량의 쓰임. 없는 id 는 NONE */
    public static Map<Long, LitersUse> litersUses(List<FuelRecord> records) {
        return walk(records).uses();
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

    /** i 번째 기록보다 앞서 같은 주행거리에 적은 가득 주유가 있는지 */
    private static boolean fullBefore(List<FuelRecord> records, int i) {
        int odometer = records.get(i).getOdometer();
        for (int j = i - 1; j >= 0 && records.get(j).getOdometer() == odometer; j--) {
            if (records.get(j).isFullTank()) {
                return true;
            }
        }
        return false;
    }

    static BigDecimal divide(int distance, BigDecimal liters) {
        return BigDecimal.valueOf(distance).divide(liters, 2, RoundingMode.HALF_UP);
    }
}
