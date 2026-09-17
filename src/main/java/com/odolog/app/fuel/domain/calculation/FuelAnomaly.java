package com.odolog.app.fuel.domain.calculation;

import com.odolog.app.fuel.domain.entity.FuelRecord;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 빠진 주유 기록 탐지. 기록을 한 번 빼먹으면 그 구간 연비가 정확히 두 배
 * 절대 임계값으로는 못 잡아(25 는 불가능한 값이 아님) 그 차량의 평소 구간과 비교
 * 기준은 평균이 아니라 중앙값 — 평균은 잡으려는 이상값 자체에 끌려 올라감
 */
public final class FuelAnomaly {

    /** 이 위는 내연기관에서 안 나옴. 입력 오류 */
    private static final BigDecimal MAX_REALISTIC = BigDecimal.valueOf(50);
    /** 이 아래도 마찬가지 */
    private static final BigDecimal MIN_REALISTIC = BigDecimal.valueOf(2);

    /** 의심 배수. 1.8 = 한 번 빼먹음(2배)은 잡고 계절 편차(1.5배쯤)는 통과 */
    private static final double LONG_SEGMENT_RATIO = 1.8;

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
     * 평소보다 긴 구간의 개수 = 기록이 빠졌을 가능성
     *
     * @param records 주행거리 오름차순 정렬된 한 차량의 주유 기록
     */
    public static int longSegmentCount(List<FuelRecord> records) {
        List<Integer> distances = segmentDistances(records);
        if (distances.size() < MIN_SEGMENTS_FOR_MEDIAN) {
            // 구간이 두엇뿐이면 "평소"가 없음. 판단 보류
            return 0;
        }

        double threshold = median(distances) * LONG_SEGMENT_RATIO;
        return (int) distances.stream().filter(distance -> distance > threshold).count();
    }

    private static List<Integer> segmentDistances(List<FuelRecord> records) {
        List<Integer> distances = new ArrayList<>();

        for (int i = 1; i < records.size(); i++) {
            // 기준점은 직전과의 연결을 끊음 — 연비 계산과 같은 규칙
            if (records.get(i).isResetPoint()) {
                continue;
            }
            int distance = records.get(i).getOdometer() - records.get(i - 1).getOdometer();
            if (distance > 0) {
                distances.add(distance);
            }
        }

        return distances;
    }

    private static double median(List<Integer> values) {
        List<Integer> sorted = new ArrayList<>(values);
        Collections.sort(sorted);

        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1
                ? sorted.get(middle)
                : (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }
}
