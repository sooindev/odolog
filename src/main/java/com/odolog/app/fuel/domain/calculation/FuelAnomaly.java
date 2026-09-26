package com.odolog.app.fuel.domain.calculation;

import com.odolog.app.fuel.domain.entity.FuelRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 빠진 주유 기록 탐지. 기록 하나가 빠지면 그 구간 연비가 두 배
 * 절대 임계값 대신 그 차량의 평소 구간과 비교. 기준은 중앙값(평균은 이상값에 끌려감)
 * 기록 삭제도 같은 경우로 취급
 */
public final class FuelAnomaly {

    /** 내연기관 연비 상한. 초과는 입력 오류 */
    private static final BigDecimal MAX_REALISTIC = BigDecimal.valueOf(50);
    /** 연비 하한 */
    private static final BigDecimal MIN_REALISTIC = BigDecimal.valueOf(2);

    /** 의심 배수. 누락(2배)은 탐지, 계절 편차(1.5배 안팎)는 통과 */
    private static final BigDecimal SUSPICION_RATIO = BigDecimal.valueOf(1.8);

    /** 중앙값을 쓸 최소 구간 수 */
    private static final int MIN_SEGMENTS_FOR_MEDIAN = 3;

    private FuelAnomaly() {
    }

    /** 물리적으로 불가능한 연비 여부. 미계산이면 false */
    public static boolean isImpossible(BigDecimal kmPerLiter) {
        if (kmPerLiter == null) {
            return false;
        }
        return kmPerLiter.compareTo(MAX_REALISTIC) > 0 || kmPerLiter.compareTo(MIN_REALISTIC) < 0;
    }

    /**
     * 그 차량의 평소 구간 기준. 한 번 계산 후 구간마다 판정
     *
     * @param records 주행거리 오름차순 정렬된 한 차량의 주유 기록
     */
    public static Baseline baselineOf(List<FuelRecord> records) {
        List<Segment> segments = segments(records);
        if (segments.size() < MIN_SEGMENTS_FOR_MEDIAN) {
            // 구간이 부족하면 판단 보류
            return Baseline.NONE;
        }

        BigDecimal distanceMedian = median(segments.stream().map(s -> BigDecimal.valueOf(s.distance())).toList());
        BigDecimal efficiencyMedian = median(segments.stream().map(Segment::efficiency).toList());

        return new Baseline(
                distanceMedian.multiply(SUSPICION_RATIO),
                efficiencyMedian.multiply(SUSPICION_RATIO));
    }

    /**
     * 평소 구간 기준. NONE 이면 판정 안 함
     *
     * @param distanceThreshold    초과 시 평소보다 긴 구간
     * @param efficiencyThreshold  초과 시 평소보다 잘 나온 구간
     */
    public record Baseline(BigDecimal distanceThreshold, BigDecimal efficiencyThreshold) {

        public static final Baseline NONE = new Baseline(null, null);

        /**
         * 주유 기록 누락 의심 여부
         * 거리와 연비가 둘 다 평소 초과일 때만. 장거리 여행(거리만 김) 제외
         */
        public boolean suspectsMissingRecord(int distance, BigDecimal efficiency) {
            if (distanceThreshold == null || efficiency == null) {
                return false;
            }

            return BigDecimal.valueOf(distance).compareTo(distanceThreshold) > 0
                    && efficiency.compareTo(efficiencyThreshold) > 0;
        }
    }

    /** 성립하는 구간만. 연비 계산과 같은 규칙 */
    private record Segment(int distance, BigDecimal efficiency) {
    }

    private static List<Segment> segments(List<FuelRecord> records) {
        List<Segment> segments = new ArrayList<>();

        for (int i = 1; i < records.size(); i++) {
            // 기준점은 직전과 연결 끊김
            if (records.get(i).isResetPoint()) {
                continue;
            }

            int distance = records.get(i).getOdometer() - records.get(i - 1).getOdometer();
            BigDecimal used = records.get(i).getLiters();
            // 주유량 없는 구간은 표본에서 제외
            if (distance <= 0 || used == null || used.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            segments.add(new Segment(distance,
                    BigDecimal.valueOf(distance).divide(used, 2, RoundingMode.HALF_UP)));
        }

        return segments;
    }

    private static BigDecimal median(List<BigDecimal> values) {
        List<BigDecimal> sorted = new ArrayList<>(values);
        Collections.sort(sorted);

        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }

        return sorted.get(middle - 1).add(sorted.get(middle))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }
}
