package com.odolog.app.fuel.domain.calculation;

import com.odolog.app.fuel.domain.entity.FuelRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 기록이 빠진 구간 탐지. 기록을 한 번 빼먹으면 그 구간 연비가 정확히 두 배
 * 절대 임계값으로는 못 잡아(25 는 불가능한 값이 아님) 그 차량의 평소 구간과 비교
 * 기준은 평균이 아니라 중앙값 — 평균은 잡으려는 이상값 자체에 끌려 올라감
 *
 * 기록을 지우는 것도 같은 사건이다 — 거리는 앞 기록까지 늘어나는데 지워진 기록의
 * 주유량은 사라지므로, 깜빡하고 안 적은 것과 남는 데이터가 똑같다
 */
public final class FuelAnomaly {

    /** 이 위는 내연기관에서 안 나옴. 입력 오류 */
    private static final BigDecimal MAX_REALISTIC = BigDecimal.valueOf(50);
    /** 이 아래도 마찬가지 */
    private static final BigDecimal MIN_REALISTIC = BigDecimal.valueOf(2);

    /** 의심 배수. 1.8 = 한 번 빼먹음(2배)은 잡고 계절 편차(1.5배쯤)는 통과 */
    private static final BigDecimal SUSPICION_RATIO = BigDecimal.valueOf(1.8);

    /** 중앙값이 뜻을 가지는 최소 구간 수 */
    private static final int MIN_SEGMENTS_FOR_MEDIAN = 3;

    private FuelAnomaly() {
    }

    /** 물리적으로 불가능한 연비인지. 미계산이면 false */
    public static boolean isImpossible(BigDecimal kmPerLiter) {
        if (kmPerLiter == null) {
            return false;
        }
        return kmPerLiter.compareTo(MAX_REALISTIC) > 0 || kmPerLiter.compareTo(MIN_REALISTIC) < 0;
    }

    /**
     * 그 차량의 '평소 구간'. 임계값을 한 번 계산해 두고 구간마다 물어본다
     *
     * @param records 주행거리 오름차순 정렬된 한 차량의 주유 기록
     */
    public static Baseline baselineOf(List<FuelRecord> records) {
        List<Segment> segments = segments(records);
        if (segments.size() < MIN_SEGMENTS_FOR_MEDIAN) {
            // 구간이 두엇뿐이면 '평소'가 없음. 판단 보류
            return Baseline.NONE;
        }

        BigDecimal distanceMedian = median(segments.stream().map(s -> BigDecimal.valueOf(s.distance())).toList());
        BigDecimal efficiencyMedian = median(segments.stream().map(Segment::efficiency).toList());

        return new Baseline(
                distanceMedian.multiply(SUSPICION_RATIO),
                efficiencyMedian.multiply(SUSPICION_RATIO));
    }

    /**
     * 평소 구간과 견주는 기준. NONE 이면 아무것도 의심하지 않는다
     *
     * @param distanceThreshold    이 거리를 넘으면 '평소보다 긴 구간'
     * @param efficiencyThreshold  이 연비를 넘으면 '평소보다 잘 나온 구간'
     */
    public record Baseline(BigDecimal distanceThreshold, BigDecimal efficiencyThreshold) {

        public static final Baseline NONE = new Baseline(null, null);

        /**
         * 이 구간에 주유 기록이 빠졌다고 볼 수 있는가
         *
         * 거리와 연비가 둘 다 평소를 넘어야 한다. 거리만 보면 장거리 여행을 잡는다 —
         * 멀리 갔으면 그만큼 넣었으므로 거리는 길어도 연비는 평소와 비슷하다.
         * 기록이 빠진 구간은 거리만 늘고 주유량은 그대로라 연비까지 함께 뛴다
         */
        public boolean suspectsMissingRecord(int distance, BigDecimal efficiency) {
            if (distanceThreshold == null || efficiency == null) {
                return false;
            }

            return BigDecimal.valueOf(distance).compareTo(distanceThreshold) > 0
                    && efficiency.compareTo(efficiencyThreshold) > 0;
        }
    }

    /** 성립하는 구간만. 연비 계산과 같은 규칙이라야 두 숫자가 어긋나지 않는다 */
    private record Segment(int distance, BigDecimal efficiency) {
    }

    private static List<Segment> segments(List<FuelRecord> records) {
        List<Segment> segments = new ArrayList<>();

        for (int i = 1; i < records.size(); i++) {
            // 기준점은 직전과의 연결을 끊음 — 연비 계산과 같은 규칙
            if (records.get(i).isResetPoint()) {
                continue;
            }

            int distance = records.get(i).getOdometer() - records.get(i - 1).getOdometer();
            BigDecimal used = records.get(i).getLiters();
            // 연비 계산과 같은 규칙 — 주유량을 안 적은 구간은 '평소' 를 재는 표본에서도 빠진다
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
