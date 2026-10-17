package com.odolog.app.fuel.domain;

import com.odolog.app.fuel.domain.FuelSegment.LitersUse;
import com.odolog.app.vehicle.domain.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 기록마다 주유량이 들어간 구간. 목록 행 안내의 근거 */
class FuelSegmentTest {

    private final Vehicle vehicle = new Vehicle(null, "12가3456", "현대", "아반떼", 2023);
    private long nextId = 1;

    /** id 는 넣은 순서. 같은 주행거리에서는 id 순서가 곧 기록 순서 */
    private FuelRecord full(int odometer, String liters) {
        FuelRecord record = new FuelRecord(vehicle, LocalDate.of(2026, 9, 1), odometer,
                liters == null ? null : new BigDecimal(liters), 80000, "KRW", null);
        ReflectionTestUtils.setField(record, "id", nextId++);
        return record;
    }

    private FuelRecord partial(int odometer, String liters) {
        FuelRecord record = full(odometer, liters);
        record.changeFullTank(false);
        return record;
    }

    private LitersUse useOf(List<FuelRecord> records, FuelRecord record) {
        Map<Long, LitersUse> uses = FuelSegment.litersUses(records);
        return uses.getOrDefault(record.getId(), LitersUse.NONE);
    }

    @Test
    @DisplayName("가득과 다음 가득 사이의 덜 채운 주유는 다음 구간에 들어간다")
    void partialBetweenFullsGoesToNext() {
        FuelRecord start = full(10000, "40.00");
        FuelRecord between = partial(10300, "10.00");
        FuelRecord closing = full(10600, "30.00");
        List<FuelRecord> records = List.of(start, between, closing);

        assertThat(useOf(records, start)).isEqualTo(LitersUse.NONE);
        assertThat(useOf(records, between)).isEqualTo(LitersUse.NEXT);
        assertThat(useOf(records, closing)).isEqualTo(LitersUse.CLOSING);
    }

    @Test
    @DisplayName("출발 주행거리에서 덜 채운 양은 어느 구간에도 안 들어간다")
    void partialAtStartOdometerIsNotCounted() {
        FuelRecord start = full(10000, "40.00");
        FuelRecord topUp = partial(10000, "5.00");
        FuelRecord closing = full(10500, "30.00");
        List<FuelRecord> records = List.of(start, topUp, closing);

        assertThat(useOf(records, topUp)).isEqualTo(LitersUse.NONE);
        assertThat(FuelSegment.between(records)).singleElement()
                .satisfies(segment -> assertThat(segment.liters()).isEqualByComparingTo("30.00"));
    }

    @Test
    @DisplayName("첫 기록 하나만 덜 채움이면 그 양은 어느 구간에도 안 들어간다")
    void firstPartialIsNotCounted() {
        FuelRecord first = partial(10000, "20.00");
        FuelRecord next = full(10500, "30.00");
        List<FuelRecord> records = List.of(first, next);

        assertThat(useOf(records, first)).isEqualTo(LitersUse.NONE);
        assertThat(useOf(records, next)).isEqualTo(LitersUse.NONE);
    }

    @Test
    @DisplayName("가득으로 닫은 뒤 같은 주행거리의 덜 채움은 방금 닫힌 구간에 들어간다")
    void partialAfterClosingFullGoesToPrevious() {
        FuelRecord start = full(10000, "40.00");
        FuelRecord closing = full(10500, "25.00");
        FuelRecord topUp = partial(10500, "5.00");
        List<FuelRecord> records = List.of(start, closing, topUp, full(11000, "25.00"));

        assertThat(useOf(records, closing)).isEqualTo(LitersUse.CLOSING);
        assertThat(useOf(records, topUp)).isEqualTo(LitersUse.PREVIOUS);
    }

    @Test
    @DisplayName("아직 가득 주유가 없는 덜 채움은 이어 갈 수 있으면 다음, 출발이 덜 채움이면 없음")
    void pendingPartial() {
        FuelRecord fromFull = partial(10300, "10.00");
        assertThat(useOf(List.of(full(10000, "40.00"), fromFull), fromFull)).isEqualTo(LitersUse.NEXT);

        FuelRecord fromPartial = partial(10300, "10.00");
        assertThat(useOf(List.of(partial(10000, "40.00"), fromPartial), fromPartial)).isEqualTo(LitersUse.NONE);
    }

    @Test
    @DisplayName("덜 채운 기준점과 같은 주행거리에 가득이 있으면 어느 쪽을 먼저 적었든 다음 구간을 낸다")
    void partialResetPointWithFullAtSameOdometerIgnoresOrder() {
        // 기준점을 나중에 적음
        FuelRecord fullFirst = full(20000, "30.00");
        FuelRecord resetAfter = partial(20000, "5.00");
        resetAfter.changeResetPoint(true);
        List<FuelRecord> fullThenReset = List.of(full(10000, "40.00"), fullFirst, resetAfter, full(20500, "25.00"));

        // 기준점을 먼저 적음
        FuelRecord resetBefore = partial(20000, "5.00");
        resetBefore.changeResetPoint(true);
        List<FuelRecord> resetThenFull = List.of(full(10000, "40.00"), resetBefore, full(20000, "30.00"), full(20500, "25.00"));

        assertThat(FuelSegment.between(fullThenReset)).last()
                .satisfies(segment -> assertThat(segment.efficiency()).isEqualByComparingTo("20.00"));
        assertThat(FuelSegment.between(resetThenFull)).last()
                .satisfies(segment -> assertThat(segment.efficiency()).isEqualByComparingTo("20.00"));
    }
}
