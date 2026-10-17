package com.odolog.app.fuel.domain;

import com.odolog.app.vehicle.domain.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 평균 연비 공식. 차량 상세·홈 요약 공용 */
class FuelEfficiencyTest {

    private final Vehicle vehicle = new Vehicle(null, "12가3456", "현대", "아반떼", 2023);

    private FuelRecord record(int odometer, String liters) {
        return new FuelRecord(vehicle, LocalDate.of(2026, 9, 1), odometer,
                new BigDecimal(liters), 80000, "KRW", null);
    }

    /** 주유량 없는 기록 */
    private FuelRecord withoutLiters(int odometer) {
        return new FuelRecord(vehicle, LocalDate.of(2026, 9, 1), odometer, null, 80000, "KRW", null);
    }

    /** 가득 채우지 않은 주유 */
    private FuelRecord partial(int odometer, String liters) {
        FuelRecord record = new FuelRecord(vehicle, LocalDate.of(2026, 9, 1), odometer,
                liters == null ? null : new BigDecimal(liters), 80000, "KRW", null);
        record.changeFullTank(false);
        return record;
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
        // 1000km ÷ 50L(25+25). 첫 30L 를 빼지 않으면 12.50
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
        // 가운데 기록의 주유량 없음. 다음 구간(10800 → 11200)은 계산 가능
        FuelEfficiency result = FuelEfficiency.of(ascending(
                record(10000, "40.00"),
                record(10400, "40.00"),
                withoutLiters(10800),
                record(11200, "40.00")));

        // 성립 구간 둘. 800km ÷ 80L
        assertThat(result.distance()).isEqualTo(800);
        assertThat(result.average()).isEqualByComparingTo("10.00");
        // 미성립 구간은 뺀 구간에 미포함
        assertThat(result.excludedSegments()).isZero();
        assertThat(result.missingSegments()).isZero();
    }

    @Test
    @DisplayName("기록이 빠진 구간은 평균에서 빼고 따로 센다 — 기록을 지웠을 때가 이 경우다")
    void missingSegmentExcluded() {
        // 평소 400km/40L(10km/L), 기록 하나 누락으로 800km/40L(20km/L)
        // 불가능 기준(50) 미만이라 누락 판정으로만 제외 가능
        FuelEfficiency result = FuelEfficiency.of(ascending(
                record(10000, "40.00"),
                record(10400, "40.00"),
                record(10800, "40.00"),
                record(11200, "40.00"),
                record(12000, "40.00")));

        assertThat(result.missingSegments()).isEqualTo(1);
        assertThat(result.excludedSegments()).isZero();
        // 정상 구간 셋(1200km ÷ 120L)만. 제외하지 않으면 12.50
        assertThat(result.distance()).isEqualTo(1200);
        assertThat(result.average()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("장거리 여행은 빼지 않는다 — 거리는 길어도 그만큼 넣었으면 연비는 평소와 같다")
    void longTripKept() {
        // 800km 에 80L. 거리만 보고 제외하면 정상 구간 손실
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
        // 자리수 오타 기록 하나. 제외하지 않으면 평균 16만 km/L
        FuelEfficiency result = FuelEfficiency.of(ascending(
                record(10000, "30.00"),
                record(10500, "25.00"),
                record(5000000, "30.00")));

        assertThat(result.excludedSegments()).isEqualTo(1);
        // 남은 구간 500km ÷ 25L
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
        // 같은 주행거리에 두 번 주유. 리터만 더하면 평균 하락
        FuelEfficiency result = FuelEfficiency.of(ascending(
                record(10000, "30.00"),
                record(10000, "10.00"),
                record(10500, "25.00")));

        assertThat(result.average()).isEqualByComparingTo("20.00");
        assertThat(result.distance()).isEqualTo(500);
    }

    @Test
    @DisplayName("중간에 같은 주행거리로 나눠 넣은 주유는 앞 구간에 합쳐 계산한다")
    void splitFillMergedIntoSegment() {
        // 40,500km 에서 30L + 15L. 둘째를 빼면 500÷30 = 16.67, 평균 14.29
        FuelEfficiency result = FuelEfficiency.of(ascending(
                record(40000, "40.00"),
                record(40500, "30.00"),
                record(40500, "15.00"),
                record(41000, "40.00")));

        // 1000km ÷ 85L(45 + 40)
        assertThat(result.distance()).isEqualTo(1000);
        assertThat(result.average()).isEqualByComparingTo("11.76");
    }

    @Test
    @DisplayName("나눠 넣은 주유 중 하나라도 주유량이 비면 그 구간은 계산하지 않는다")
    void splitFillWithUnknownLitersSkipped() {
        // 둘째 양을 모름. 30L 만으로 나누면 부풀려짐
        FuelEfficiency result = FuelEfficiency.of(ascending(
                record(40000, "40.00"),
                record(40500, "30.00"),
                withoutLiters(40500),
                record(41000, "40.00")));

        // 남는 구간은 40,500 → 41,000 하나
        assertThat(result.distance()).isEqualTo(500);
        assertThat(result.average()).isEqualByComparingTo("12.50");
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

        // 기준점 이전 구간과 기준점 자신의 주유량 제외
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

    @Test
    @DisplayName("덜 채운 주유는 다음 가득 주유의 구간에 합쳐 한 구간으로 계산한다")
    void partialFillMergesIntoNextFullSegment() {
        // 가득 → 300km 뒤 10L 만 → 300km 뒤 가득 30L. 따로 나누면 30.00 과 10.00 두 점
        List<FuelRecord> records = ascending(
                record(10000, "40.00"),
                partial(10300, "10.00"),
                record(10600, "30.00"));

        FuelEfficiency result = FuelEfficiency.of(records);
        assertThat(result.distance()).isEqualTo(600);
        assertThat(result.average()).isEqualByComparingTo("15.00");
        assertThat(FuelEfficiency.trend(records, 12)).singleElement()
                .extracting(FuelEfficiency.Point::efficiency).satisfies(value -> assertThat(value).isEqualByComparingTo("15.00"));
    }

    @Test
    @DisplayName("시작이 덜 채운 주유면 다음 가득까지의 구간은 낼 수 없다 — 그때 탱크에 얼마 있었는지 모른다")
    void segmentStartingFromPartialFillIsSkipped() {
        FuelEfficiency result = FuelEfficiency.of(ascending(
                partial(10000, "20.00"),
                record(10500, "30.00"),
                record(11000, "25.00")));

        // 10,500 → 11,000 하나만
        assertThat(result.distance()).isEqualTo(500);
        assertThat(result.average()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("가득 뒤 같은 주행거리로 덜 채워 더 넣어도 탱크는 가득 — 다음 구간을 낸다")
    void partialAfterFullAtSameOdometerKeepsTankFull() {
        FuelEfficiency result = FuelEfficiency.of(ascending(
                record(10000, "40.00"),
                record(10500, "25.00"),
                partial(10500, "5.00"),
                record(11000, "25.00")));

        // 500÷30(25+5) 와 500÷25. 묶음 끝의 덜 채움만 보면 뒤 구간이 빠짐
        assertThat(result.distance()).isEqualTo(1000);
        assertThat(result.average()).isEqualByComparingTo("18.18");
    }

    @Test
    @DisplayName("첫 기록을 나눠 넣으며 뒤에서 가득 채웠으면 거기서 구간을 시작한다")
    void fullAfterPartialAtFirstOdometerStartsSegment() {
        FuelEfficiency result = FuelEfficiency.of(ascending(
                partial(10000, "20.00"),
                record(10000, "10.00"),
                record(10500, "30.00")));

        // 같은 주행거리에서 가득 채운 뒤 500km. 시작 전에 넣은 30L 는 셈에 안 들어감
        assertThat(result.distance()).isEqualTo(500);
        assertThat(result.average()).isEqualByComparingTo("16.67");
    }

    @Test
    @DisplayName("사이의 덜 채운 주유량을 모르면 그 구간은 계산하지 않는다")
    void unknownPartialLitersSkipsSegment() {
        FuelEfficiency result = FuelEfficiency.of(ascending(
                record(10000, "40.00"),
                partial(10300, null),
                record(10600, "30.00"),
                record(11000, "20.00")));

        // 10,600 → 11,000 하나만
        assertThat(result.distance()).isEqualTo(400);
        assertThat(result.average()).isEqualByComparingTo("20.00");
    }
}
