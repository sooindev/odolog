package com.odolog.app.fuel.domain.calculation;

import com.odolog.app.fuel.domain.entity.FuelRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 평균 연비 공식. 차량 상세와 홈 요약이 공유
 * 공식이 두 벌이면 화면마다 다른 연비가 뜸
 *
 * <p>구간별로 더한다. 총 거리 ÷ (총 주유량 − 첫 주유량) 과 결과는 같지만,
 * 구간 단위라야 물리적으로 불가능한 구간을 골라낼 수 있다 — 자리수를 한 자리 잘못 넣은
 * 기록 하나가 평균을 16만 km/L 로 만들어 두 화면이 동시에 거짓말을 하던 자리다.
 *
 * @param distance          계산에 쓴 구간 거리의 합(km). 계산 불가면 null
 * @param average           km/L, 소수 2자리. 계산 불가면 null
 * @param excludedSegments  평균에서 뺀 구간 수. 뺐으면 화면이 그 사실을 말해야 한다
 */
public record FuelEfficiency(Integer distance, BigDecimal average, int excludedSegments) {

    private static final FuelEfficiency NONE = new FuelEfficiency(null, null, 0);

    /**
     * @param all 주행거리 오름차순 정렬된 한 차량의 주유 기록
     */
    public static FuelEfficiency of(List<FuelRecord> all) {
        List<FuelRecord> records = sinceResetPoint(all);

        // 두 건은 있어야 사이 거리가 생김
        if (records.size() < 2) {
            return NONE;
        }

        int distance = 0;
        BigDecimal liters = BigDecimal.ZERO;
        int excluded = 0;

        // i 번째 기록의 주유량이 i-1 → i 구간을 달린 연료. 그래서 첫 기록의 주유량은 빠진다
        for (int i = 1; i < records.size(); i++) {
            int segment = records.get(i).getOdometer() - records.get(i - 1).getOdometer();
            BigDecimal used = records.get(i).getLiters();

            if (segment <= 0 || used.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 불가능한 구간은 평균에서 뺀다. 목록은 그 값을 지우지 않고 "확인 필요"로 표시한다 —
            // 무엇을 잘못 적었는지 보려면 값이 남아 있어야 하고, 평균은 맞아야 한다
            if (FuelAnomaly.isImpossible(divide(segment, used))) {
                excluded++;
                continue;
            }

            distance += segment;
            liters = liters.add(used);
        }

        if (distance <= 0 || liters.compareTo(BigDecimal.ZERO) <= 0) {
            return new FuelEfficiency(null, null, excluded);
        }

        return new FuelEfficiency(distance, divide(distance, liters), excluded);
    }

    private static BigDecimal divide(int distance, BigDecimal liters) {
        return BigDecimal.valueOf(distance).divide(liters, 2, RoundingMode.HALF_UP);
    }

    /**
     * 최근 기준점 이후만 남기기. 기준점 없으면 전부
     * 오름차순이라 뒤에서부터 — 여럿이면 마지막 것 우선
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
