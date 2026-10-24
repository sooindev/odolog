package com.odolog.app.fuel.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * 평균 연비 공식. 차량 상세와 홈 요약이 공유
 * 구간 단위 합산. 불가능한 구간·기록 누락 구간을 골라내기 위함
 *
 * @param distance          계산에 쓴 구간 거리 합(km). 계산 불가면 null
 * @param average           km/L, 소수 2자리. 계산 불가면 null
 * @param excludedSegments  불가능한 값이라 뺀 구간 수
 * @param missingSegments   기록 누락으로 보여 뺀 구간 수
 */
public record FuelEfficiency(Integer distance, BigDecimal average,
                             int excludedSegments, int missingSegments) {

    private static final FuelEfficiency NONE = new FuelEfficiency(null, null, 0, 0);

    /** @param all 주행거리 오름차순 정렬된 한 차량의 주유 기록 */
    public static FuelEfficiency of(List<FuelRecord> all) {
        List<FuelSegment> segments = sinceResetPoint(all);

        // 셀 구간이 없음
        if (segments.isEmpty()) {
            return NONE;
        }

        // 기준은 전체 이력에서 산출. 초기화와 무관한 그 차의 평소
        FuelAnomaly.Baseline baseline = FuelAnomaly.baselineOf(all);

        int distance = 0;
        BigDecimal liters = BigDecimal.ZERO;
        int excluded = 0;
        int missing = 0;

        // 주유량 없는 기록은 자기 구간만 제외
        for (FuelSegment segment : segments) {
            // 불가능한 구간은 평균에서 제외. 목록에서는 값 유지 + 확인 필요 표시
            if (FuelAnomaly.isImpossible(segment.efficiency())) {
                excluded++;
                continue;
            }

            // 기록 누락 구간도 제외. 거리만 늘고 주유량은 한 번치
            if (baseline.suspectsMissingRecord(segment.distance(), segment.efficiency())) {
                missing++;
                continue;
            }

            distance += segment.distance();
            liters = liters.add(segment.liters());
        }

        if (distance <= 0 || liters.compareTo(BigDecimal.ZERO) <= 0) {
            return new FuelEfficiency(null, null, excluded, missing);
        }

        return new FuelEfficiency(distance, FuelSegment.divide(distance, liters), excluded, missing);
    }

    /**
     * 구간 연비의 시간순 추이
     * 평균에서 빼는 구간은 여기서도 제외
     *
     * @param all   주행거리 오름차순 정렬된 한 차량의 주유 기록
     * @param limit 최근 몇 개까지
     */
    public static List<Point> trend(List<FuelRecord> all, int limit) {
        FuelAnomaly.Baseline baseline = FuelAnomaly.baselineOf(all);

        List<Point> points = new ArrayList<>();
        for (FuelSegment segment : sinceResetPoint(all)) {
            if (FuelAnomaly.isImpossible(segment.efficiency())
                    || baseline.suspectsMissingRecord(segment.distance(), segment.efficiency())) {
                continue;
            }

            points.add(new Point(segment.to().getFueledAt(), segment.efficiency()));
        }

        // 최근 limit 개
        return points.size() <= limit ? points : points.subList(points.size() - limit, points.size());
    }

    /** 추이 한 점 */
    public record Point(LocalDate fueledAt, BigDecimal efficiency) {
    }

    /**
     * 최근 기준점 뒤에서 닫힌 구간만. 기준점 없으면 전체
     * 순회는 이력 전체로. 잘라서 돌면 기준점과 같은 주행거리에 먼저 적은 가득 주유를 못 봐 목록 행과 갈림
     */
    private static List<FuelSegment> sinceResetPoint(List<FuelRecord> records) {
        List<FuelSegment> segments = FuelSegment.between(records);
        for (int i = records.size() - 1; i >= 0; i--) {
            if (records.get(i).isResetPoint()) {
                // 저장 전 기록은 id 가 없어 같은 객체인지로 셈
                Set<FuelRecord> after = Collections.newSetFromMap(new IdentityHashMap<>());
                after.addAll(records.subList(i + 1, records.size()));
                return segments.stream().filter(segment -> after.contains(segment.to())).toList();
            }
        }
        return segments;
    }
}
