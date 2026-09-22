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

/**
 * 평균 연비 공식. 차량 상세와 홈 요약이 같은 것을 쓰므로 여기가 틀리면 두 화면이 함께 틀린다
 */
class FuelEfficiencyTest {

    private final Vehicle vehicle = new Vehicle(null, "12가3456", "현대", "아반떼", 2023);

    private FuelRecord record(int odometer, String liters) {
        return new FuelRecord(vehicle, LocalDate.of(2026, 9, 1), odometer,
                new BigDecimal(liters), 80000, null);
    }

    /** 주유량을 안 적고 저장한 기록 */
    private FuelRecord withoutLiters(int odometer) {
        return new FuelRecord(vehicle, LocalDate.of(2026, 9, 1), odometer, null, 80000, null);
    }

    private List<FuelRecord> ascending(FuelRecord... records) {
        return new ArrayList<>(List.of(records));
    }

    @Test
    @DisplayName("기록이 2건 미만이면 계산하지 않는다")
    void needsTwoRecords() {
        assertThat(FuelEfficiency.of(List.of()).average()).isNull();
        assertThat(FuelEfficiency.of(ascending(record(10000, "30.00"))).average()).isNull();
    }

    @Test
    @DisplayName("첫 주유량은 평균에서 빠진다")
    void firstLitersExcluded() {
        // 1000km 를 50L(=25+25)로 달렸다. 첫 30L 를 빼지 않으면 12.50 이 나온다
        FuelEfficiency result = FuelEfficiency.of(ascending(
                record(10000, "30.00"),
                record(10500, "25.00"),
                record(11000, "25.00")));

        assertThat(result.distance()).isEqualTo(1000);
        assertThat(result.average()).isEqualByComparingTo("20.00");
        assertThat(result.excludedSegments()).isZero();
    }

    @Test
    @DisplayName("주유량을 안 적은 기록은 자기 구간만 빠지고 다음 구간은 멀쩡하다")
    void recordWithoutLitersSkipsOnlyItsOwnSegment() {
        /*
         * 가운데 기록에 주유량이 없다. 모르는 것은 "그때 얼마나 넣었나" 뿐이고
         * 계기판은 그대로 이어지므로, 그 다음 구간(10800 → 11200)은 계산할 수 있어야 한다.
         */
        FuelEfficiency result = FuelEfficiency.of(ascending(
                record(10000, "40.00"),
                record(10400, "40.00"),
                withoutLiters(10800),
                record(11200, "40.00")));

        // 성립하는 구간은 10000→10400 과 10800→11200 둘. 800km / 80L
        assertThat(result.distance()).isEqualTo(800);
        assertThat(result.average()).isEqualByComparingTo("10.00");
        // 못 센 것은 '뺀 구간' 이 아니다 — 애초에 구간이 성립하지 않는다
        assertThat(result.excludedSegments()).isZero();
        assertThat(result.missingSegments()).isZero();
    }

    @Test
    @DisplayName("기록이 빠진 구간은 평균에서 빼고 따로 센다 — 기록을 지웠을 때가 이 경우다")
    void missingSegmentExcluded() {
        /*
         * 평소 400km 를 40L 로 달리는 차(10km/L). 가운데 기록 하나가 없어져
         * 한 구간만 800km 가 됐다 — 거리는 두 배인데 주유량은 한 번치뿐이라 20km/L 로 뜬다.
         * 50 을 넘지 않아 "불가능"에는 안 걸리므로, 빼지 않으면 조용히 평균을 끌어올린다.
         */
        FuelEfficiency result = FuelEfficiency.of(ascending(
                record(10000, "40.00"),
                record(10400, "40.00"),
                record(10800, "40.00"),
                record(11200, "40.00"),
                record(12000, "40.00")));

        assertThat(result.missingSegments()).isEqualTo(1);
        assertThat(result.excludedSegments()).isZero();
        // 멀쩡한 구간 셋(1200km / 120L)만 남는다. 빼지 않으면 12.50 이다
        assertThat(result.distance()).isEqualTo(1200);
        assertThat(result.average()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("장거리 여행은 빼지 않는다 — 거리는 길어도 그만큼 넣었으면 연비는 평소와 같다")
    void longTripKept() {
        // 마지막 구간이 800km 지만 80L 를 넣었다. 거리만 보고 빼면 멀쩡한 구간을 버린다
        FuelEfficiency result = FuelEfficiency.of(ascending(
                record(10000, "40.00"),
                record(10400, "40.00"),
                record(10800, "40.00"),
                record(11200, "40.00"),
                record(12000, "80.00")));

        assertThat(result.missingSegments()).isZero();
        assertThat(result.distance()).isEqualTo(2000);
        assertThat(result.average()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("물리적으로 불가능한 구간은 평균에서 빼고 뺀 개수를 알려준다")
    void impossibleSegmentExcluded() {
        /*
         * 자리수를 한 자리 더 넣은 기록 하나가 섞인 경우.
         * 빼지 않으면 평균이 16만 km/L 가 되어 차량 상세와 홈이 동시에 거짓말을 한다.
         */
        FuelEfficiency result = FuelEfficiency.of(ascending(
                record(10000, "30.00"),
                record(10500, "25.00"),
                record(5000000, "30.00")));

        assertThat(result.excludedSegments()).isEqualTo(1);
        // 남은 구간은 500km ÷ 25L
        assertThat(result.average()).isEqualByComparingTo("20.00");
        assertThat(result.distance()).isEqualTo(500);
    }

    @Test
    @DisplayName("쓸 수 있는 구간이 하나도 없으면 평균은 없고 뺀 개수만 남는다")
    void allSegmentsExcluded() {
        FuelEfficiency result = FuelEfficiency.of(ascending(
                record(10000, "30.00"),
                record(5000000, "30.00")));

        assertThat(result.average()).isNull();
        assertThat(result.distance()).isNull();
        assertThat(result.excludedSegments()).isEqualTo(1);
    }

    @Test
    @DisplayName("주행거리가 같은 구간은 거리도 주유량도 계산에 넣지 않는다")
    void zeroDistanceSegmentSkipped() {
        // 같은 주행거리에 두 번 넣은 경우. 리터만 더하면 평균이 실제보다 낮아진다
        FuelEfficiency result = FuelEfficiency.of(ascending(
                record(10000, "30.00"),
                record(10000, "10.00"),
                record(10500, "25.00")));

        assertThat(result.average()).isEqualByComparingTo("20.00");
        assertThat(result.distance()).isEqualTo(500);
    }

    @Test
    @DisplayName("기준점 이후 구간만 평균에 들어간다")
    void sinceResetPoint() {
        FuelRecord resetPoint = record(20000, "30.00");
        resetPoint.changeResetPoint(true);

        FuelEfficiency result = FuelEfficiency.of(ascending(
                record(10000, "30.00"),
                record(10500, "40.00"),
                resetPoint,
                record(20500, "25.00")));

        // 기준점 이전(10000~10500)은 통째로 빠지고, 기준점 자신의 주유량도 "첫 기록"이라 빠진다
        assertThat(result.distance()).isEqualTo(500);
        assertThat(result.average()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("기준점이 마지막 기록이면 평균이 없다")
    void resetPointAtLastRecord() {
        FuelRecord resetPoint = record(20000, "30.00");
        resetPoint.changeResetPoint(true);

        FuelEfficiency result = FuelEfficiency.of(ascending(
                record(10000, "30.00"),
                resetPoint));

        assertThat(result.average()).isNull();
    }
}
