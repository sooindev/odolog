package com.odolog.app.fuel.domain.calculation;

import com.odolog.app.fuel.domain.entity.FuelRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
        List<FuelRecord> records = sinceResetPoint(all);

        // 구간 계산에 최소 두 건 필요
        if (records.size() < 2) {
            return NONE;
        }

        // 기준은 전체 이력에서 산출. 초기화와 무관한 그 차의 평소
        FuelAnomaly.Baseline baseline = FuelAnomaly.baselineOf(all);

        int distance = 0;
        BigDecimal liters = BigDecimal.ZERO;
        int excluded = 0;
        int missing = 0;

        // i 번째 주유량 = i-1 → i 구간 연료. 첫 기록의 주유량 제외
        for (int i = 1; i < records.size(); i++) {
            int segment = records.get(i).getOdometer() - records.get(i - 1).getOdometer();
            BigDecimal used = records.get(i).getLiters();

            // 주유량 없는 기록은 자기 구간만 제외
            if (segment <= 0 || used == null || used.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal segmentEfficiency = divide(segment, used);

            // 불가능한 구간은 평균에서 제외. 목록에서는 값 유지 + 확인 필요 표시
            if (FuelAnomaly.isImpossible(segmentEfficiency)) {
                excluded++;
                continue;
            }

            // 기록 누락 구간도 제외. 거리만 늘고 주유량은 한 번치
            if (baseline.suspectsMissingRecord(segment, segmentEfficiency)) {
                missing++;
                continue;
            }

            distance += segment;
            liters = liters.add(used);
        }

        if (distance <= 0 || liters.compareTo(BigDecimal.ZERO) <= 0) {
            return new FuelEfficiency(null, null, excluded, missing);
        }

        return new FuelEfficiency(distance, divide(distance, liters), excluded, missing);
    }

    /**
     * 구간 연비의 시간순 추이
     * 평균에서 빼는 구간은 여기서도 제외
     *
     * @param all   주행거리 오름차순 정렬된 한 차량의 주유 기록
     * @param limit 최근 몇 개까지
     */
    public static List<Point> trend(List<FuelRecord> all, int limit) {
        List<FuelRecord> records = sinceResetPoint(all);
        FuelAnomaly.Baseline baseline = FuelAnomaly.baselineOf(all);

        List<Point> points = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            int segment = records.get(i).getOdometer() - records.get(i - 1).getOdometer();
            BigDecimal used = records.get(i).getLiters();

            if (segment <= 0 || used == null || used.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal efficiency = divide(segment, used);
            if (FuelAnomaly.isImpossible(efficiency)
                    || baseline.suspectsMissingRecord(segment, efficiency)) {
                continue;
            }

            points.add(new Point(records.get(i).getFueledAt(), efficiency));
        }

        // 최근 limit 개
        return points.size() <= limit ? points : points.subList(points.size() - limit, points.size());
    }

    /** 추이 한 점 */
    public record Point(LocalDate fueledAt, BigDecimal efficiency) {
    }

    private static BigDecimal divide(int distance, BigDecimal liters) {
        return BigDecimal.valueOf(distance).divide(liters, 2, RoundingMode.HALF_UP);
    }

    /** 최근 기준점 이후만. 기준점 없으면 전체 */
    private static List<FuelRecord> sinceResetPoint(List<FuelRecord> records) {
        for (int i = records.size() - 1; i >= 0; i--) {
            if (records.get(i).isResetPoint()) {
                return records.subList(i, records.size());
            }
        }
        return records;
    }
}
