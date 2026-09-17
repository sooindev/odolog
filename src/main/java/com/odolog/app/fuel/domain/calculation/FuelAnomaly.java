package com.odolog.app.fuel.domain.calculation;

import com.odolog.app.fuel.domain.entity.FuelRecord;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * "이 기록, 뭔가 이상한데요?" 를 판단한다.
 *
 * <p>가장 흔한 사고는 <b>주유를 기록하는 걸 깜빡하는 것</b>이다. 한 번 빼먹으면 그 구간의
 * 연비가 정확히 두 배로 나온다 — 달린 거리는 그대로인데 넣은 기름만 절반으로 세기 때문이다.
 *
 * <p><b>절대 임계값만으로는 못 잡는다.</b> 12.5 가 25 가 되는데 25 는 "불가능한 값"이 아니다.
 * 그래서 그 차량의 <b>평소 구간</b>과 견준다 — 늘 400km 쯤 달리고 넣던 차가 갑자기
 * 800km 를 달렸다면 중간에 기록하지 않은 주유가 있을 가능성이 높다.
 *
 * <p>중앙값을 기준으로 삼는 이유: 평균은 이상한 구간 자체에 끌려 올라가서, 잡으려는 것이
 * 기준을 오염시킨다. 중앙값은 튀는 값 하나에 거의 움직이지 않는다.
 */
public final class FuelAnomaly {

    /** 어떤 내연기관차도 이 위는 나오지 않는다. 입력 자체가 틀린 경우다. */
    private static final BigDecimal MAX_REALISTIC = BigDecimal.valueOf(50);
    /** 아무리 나쁜 조건이라도 이 아래는 기록이 잘못된 것이다. */
    private static final BigDecimal MIN_REALISTIC = BigDecimal.valueOf(2);

    /** 평소 구간의 몇 배부터 의심할지. 1.8 은 "한 번 빼먹음"(2배)을 잡고 계절 편차는 넘긴다. */
    private static final double LONG_SEGMENT_RATIO = 1.8;

    /** 중앙값이 뜻을 가지려면 구간이 최소 이만큼은 있어야 한다. */
    private static final int MIN_SEGMENTS_FOR_MEDIAN = 3;

    private FuelAnomaly() {
    }

    /** 물리적으로 말이 안 되는 연비인지. 계산되지 않았으면 false. */
    public static boolean isImpossible(BigDecimal kmPerLiter) {
        if (kmPerLiter == null) {
            return false;
        }
        return kmPerLiter.compareTo(MAX_REALISTIC) > 0 || kmPerLiter.compareTo(MIN_REALISTIC) < 0;
    }

    /**
     * 평소보다 눈에 띄게 긴 구간의 개수. 기록이 빠졌을 가능성을 세는 것이다.
     *
     * @param records 주행거리 오름차순으로 정렬된 한 차량의 주유 기록
     */
    public static int longSegmentCount(List<FuelRecord> records) {
        List<Integer> distances = segmentDistances(records);
        if (distances.size() < MIN_SEGMENTS_FOR_MEDIAN) {
            // 구간이 두엇뿐이면 "평소"라는 게 없다. 섣불리 의심하지 않는다.
            return 0;
        }

        double threshold = median(distances) * LONG_SEGMENT_RATIO;
        return (int) distances.stream().filter(distance -> distance > threshold).count();
    }

    private static List<Integer> segmentDistances(List<FuelRecord> records) {
        List<Integer> distances = new ArrayList<>();

        for (int i = 1; i < records.size(); i++) {
            // 기준점은 직전과의 연결을 끊는다 — 연비 계산과 같은 규칙이라 구간도 세지 않는다.
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
