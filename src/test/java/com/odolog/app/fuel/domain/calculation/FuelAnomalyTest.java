package com.odolog.app.fuel.domain.calculation;

import com.odolog.app.fuel.domain.entity.FuelRecord;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FuelAnomalyTest {

    private final Vehicle vehicle = new Vehicle(null, "12가3456", "현대", "아반떼", 2023);

    private FuelRecord at(int odometer) {
        return new FuelRecord(vehicle, LocalDate.of(2026, 9, 1), odometer,
                new BigDecimal("40.00"), 80000, null);
    }

    /** 주행거리 목록 → 기록 목록 */
    private List<FuelRecord> records(int... odometers) {
        List<FuelRecord> list = new ArrayList<>();
        for (int odometer : odometers) {
            list.add(at(odometer));
        }
        return list;
    }

    @Test
    @DisplayName("50km/L 초과와 2km/L 미만은 물리적으로 말이 안 된다")
    void impossibleRange() {
        assertThat(FuelAnomaly.isImpossible(new BigDecimal("55.00"))).isTrue();
        assertThat(FuelAnomaly.isImpossible(new BigDecimal("1.50"))).isTrue();

        assertThat(FuelAnomaly.isImpossible(new BigDecimal("12.50"))).isFalse();
        assertThat(FuelAnomaly.isImpossible(new BigDecimal("25.00"))).isFalse();
        // 미계산은 이상값이 아님
        assertThat(FuelAnomaly.isImpossible(null)).isFalse();
    }

    @Test
    @DisplayName("주유를 한 번 빼먹으면 그 구간이 두 배가 되고, 그걸 잡아낸다")
    void catchesMissedRecord() {
        // 평소 400km 구간. 가운데 한 번을 안 적어 800km 구간 발생
        List<FuelRecord> records = records(10000, 10400, 10800, 11600, 12000, 12400);

        assertThat(FuelAnomaly.longSegmentCount(records)).isEqualTo(1);
    }

    @Test
    @DisplayName("구간 길이가 고르면 아무것도 의심하지 않는다")
    void noWarningWhenEven() {
        assertThat(FuelAnomaly.longSegmentCount(records(10000, 10400, 10800, 11200, 11600)))
                .isZero();
    }

    @Test
    @DisplayName("계절 편차 정도(1.5배)는 넘긴다 — 아무 때나 경고하면 아무도 안 본다")
    void toleratesNormalVariation() {
        // 400 · 400 · 600 · 400 — 1.5배는 계절 편차 수준
        assertThat(FuelAnomaly.longSegmentCount(records(10000, 10400, 10800, 11400, 11800)))
                .isZero();
    }

    @Test
    @DisplayName("구간이 셋 미만이면 '평소'라는 게 없어 의심하지 않는다")
    void needsEnoughSegments() {
        // 400 · 1200 — 세 배지만 기준 삼을 '평소'가 없음
        assertThat(FuelAnomaly.longSegmentCount(records(10000, 10400, 11600))).isZero();
        assertThat(FuelAnomaly.longSegmentCount(records(10000))).isZero();
        assertThat(FuelAnomaly.longSegmentCount(List.of())).isZero();
    }

    @Test
    @DisplayName("연비 기준점에서는 구간을 세지 않는다 — 연비 계산과 같은 규칙")
    void skipsResetPoint() {
        List<FuelRecord> records = records(10000, 10400, 10800, 11200, 11600);
        // 기준점 앞 구간은 애초에 이어지지 않음
        records.get(2).changeResetPoint(true);

        assertThat(FuelAnomaly.longSegmentCount(records)).isZero();
    }
}
