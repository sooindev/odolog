package com.odolog.app.fuel.domain.calculation;

import com.odolog.app.fuel.domain.calculation.FuelAnomaly.Baseline;
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

    private FuelRecord at(int odometer, String liters) {
        return new FuelRecord(vehicle, LocalDate.of(2026, 9, 1), odometer,
                new BigDecimal(liters), 80000, null);
    }

    /** 주행거리 목록 → 기록 목록. 주유량 전부 40L, 구간 연비가 거리에 비례 */
    private List<FuelRecord> records(int... odometers) {
        List<FuelRecord> list = new ArrayList<>();
        for (int odometer : odometers) {
            list.add(at(odometer, "40.00"));
        }
        return list;
    }

    /** 마지막 구간의 누락 의심 여부 */
    private boolean suspectsLastSegment(List<FuelRecord> records) {
        Baseline baseline = FuelAnomaly.baselineOf(records);

        FuelRecord last = records.get(records.size() - 1);
        int distance = last.getOdometer() - records.get(records.size() - 2).getOdometer();
        BigDecimal efficiency = BigDecimal.valueOf(distance)
                .divide(last.getLiters(), 2, java.math.RoundingMode.HALF_UP);

        return baseline.suspectsMissingRecord(distance, efficiency);
    }

    @Test
    @DisplayName("50km/L 초과와 2km/L 미만은 물리적으로 말이 안 된다")
    void impossibleRange() {
        assertThat(FuelAnomaly.isImpossible(new BigDecimal("55.00"))).isTrue();
        assertThat(FuelAnomaly.isImpossible(new BigDecimal("1.50"))).isTrue();

        assertThat(FuelAnomaly.isImpossible(new BigDecimal("12.50"))).isFalse();
        assertThat(FuelAnomaly.isImpossible(new BigDecimal("25.00"))).isFalse();
        // 미계산은 이상값 아님
        assertThat(FuelAnomaly.isImpossible(null)).isFalse();
    }

    @Test
    @DisplayName("주유를 한 번 빼먹으면 그 구간이 두 배가 되고, 그걸 잡아낸다")
    void catchesMissedRecord() {
        // 평소 400km(10km/L), 마지막만 800km(20km/L)
        assertThat(suspectsLastSegment(records(10000, 10400, 10800, 11200, 12000))).isTrue();
    }

    @Test
    @DisplayName("기록을 지운 것도 같은 모양으로 잡힌다 — 빼먹은 것과 데이터가 같다")
    void catchesDeletedRecord() {
        List<FuelRecord> kept = records(10000, 10400, 10800, 11200, 11600, 12000);
        // 11,600 기록 삭제 시 마지막 구간 400 → 800
        kept.remove(4);

        assertThat(suspectsLastSegment(kept)).isTrue();
    }

    @Test
    @DisplayName("⚠️ 장거리 여행은 잡지 않는다 — 거리는 길어도 연비는 평소와 같다")
    void ignoresLongTrip() {
        List<FuelRecord> trip = records(10000, 10400, 10800, 11200);
        // 800km 에 80L. 거리 두 배, 연비 그대로
        trip.add(at(12000, "80.00"));

        assertThat(suspectsLastSegment(trip)).isFalse();
    }

    @Test
    @DisplayName("구간 길이가 고르면 아무것도 의심하지 않는다")
    void noWarningWhenEven() {
        assertThat(suspectsLastSegment(records(10000, 10400, 10800, 11200, 11600))).isFalse();
    }

    @Test
    @DisplayName("계절 편차 정도(1.5배)는 넘긴다 — 아무 때나 경고하면 아무도 안 본다")
    void toleratesNormalVariation() {
        // 400·400·400·600. 1.5배는 계절 편차 수준
        assertThat(suspectsLastSegment(records(10000, 10400, 10800, 11200, 11800))).isFalse();
    }

    @Test
    @DisplayName("구간이 셋 미만이면 '평소'라는 게 없어 의심하지 않는다")
    void needsEnoughSegments() {
        // 400·1200. 기준이 될 평소 구간 부족
        assertThat(suspectsLastSegment(records(10000, 10400, 11600))).isFalse();
        assertThat(FuelAnomaly.baselineOf(records(10000))).isEqualTo(Baseline.NONE);
        assertThat(FuelAnomaly.baselineOf(List.of())).isEqualTo(Baseline.NONE);
    }

    @Test
    @DisplayName("연비 기준점에서는 구간을 세지 않는다 — 연비 계산과 같은 규칙")
    void skipsResetPoint() {
        List<FuelRecord> records = records(10000, 10400, 10800, 11200);
        // 기준점 앞은 이어지지 않아 구간 둘뿐 → 판단 보류
        records.get(1).changeResetPoint(true);

        assertThat(FuelAnomaly.baselineOf(records)).isEqualTo(Baseline.NONE);
    }

    @Test
    @DisplayName("연비를 모르는 구간은 의심 대상이 아니다")
    void ignoresUncalculatedSegment() {
        Baseline baseline = FuelAnomaly.baselineOf(records(10000, 10400, 10800, 11200, 11600));

        assertThat(baseline.suspectsMissingRecord(9999, null)).isFalse();
    }
}
