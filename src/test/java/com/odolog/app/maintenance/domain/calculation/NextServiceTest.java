package com.odolog.app.maintenance.domain.calculation;

import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.entity.ServiceInterval;
import com.odolog.app.maintenance.domain.type.ServiceType;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 다음 정비 시점과 지남 판정. 차량 상세·홈 요약 공용 */
class NextServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);

    private final Vehicle vehicle = new Vehicle(null, "12가3456", "현대", "아반떼", 2023);

    /** ENGINE_OIL 기본 5,000km / 6개월 */
    private MaintenanceRecord oil(int odometer, LocalDate date) {
        return record(ServiceType.ENGINE_OIL, odometer, date, 1L);
    }

    private MaintenanceRecord record(ServiceType type, int odometer, LocalDate date, Long id) {
        MaintenanceRecord record = new MaintenanceRecord(vehicle, type, null, 80000, odometer, date);
        ReflectionTestUtils.setField(record, "id", id);
        return record;
    }

    private List<NextService> compute(List<MaintenanceRecord> records, int odometer) {
        return NextService.of(records, List.of(), odometer, TODAY);
    }

    @Test
    @DisplayName("주행거리를 넘겼으면 지난 것이다")
    void overdueByOdometer() {
        // 20,000km 정비 → 다음 25,000km, 현재 26,000km
        assertThat(compute(List.of(oil(20000, LocalDate.of(2026, 9, 1))), 26000))
                .singleElement()
                .satisfies(next -> {
                    assertThat(next.nextOdometer()).isEqualTo(25000);
                    assertThat(next.overdue()).isTrue();
                });
    }

    @Test
    @DisplayName("날짜를 넘겼으면 주행거리가 남아 있어도 지난 것이다")
    void overdueByDate() {
        // km·개월 중 먼저 오는 쪽 기준. 하나만 넘어도 지남
        assertThat(compute(List.of(oil(20000, LocalDate.of(2026, 1, 1))), 20100))
                .singleElement()
                .satisfies(next -> {
                    assertThat(next.nextDate()).isEqualTo(LocalDate.of(2026, 7, 1));
                    assertThat(next.overdue()).isTrue();
                });
    }

    @Test
    @DisplayName("둘 다 안 넘겼으면 아직이다")
    void notOverdue() {
        assertThat(compute(List.of(oil(20000, LocalDate.of(2026, 9, 1))), 21000))
                .singleElement()
                .satisfies(next -> assertThat(next.overdue()).isFalse());
    }

    @Test
    @DisplayName("딱 그 값·그 날이면 지난 것으로 본다 — '오늘까지' 가 아니라 '오늘이 그 날' 이다")
    void boundaryIsOverdue() {
        assertThat(compute(List.of(oil(20000, LocalDate.of(2026, 9, 1))), 25000)
                .get(0).overdue()).isTrue();

        assertThat(compute(List.of(oil(20000, LocalDate.of(2026, 3, 1))), 20100)
                .get(0).overdue()).isTrue();
    }

    @Test
    @DisplayName("주기가 없는 종류(OTHER)는 지날 수가 없다")
    void otherNeverOverdue() {
        List<MaintenanceRecord> records =
                List.of(record(ServiceType.OTHER, 10000, LocalDate.of(2020, 1, 1), 1L));

        assertThat(compute(records, 999999)).singleElement().satisfies(next -> {
            assertThat(next.nextOdometer()).isNull();
            assertThat(next.nextDate()).isNull();
            assertThat(next.overdue()).isFalse();
        });
    }

    @Test
    @DisplayName("지난 것이 먼저 온다 — 이 목록은 '뭘 해야 하나' 를 보는 자리다")
    void overdueComesFirst() {
        List<MaintenanceRecord> records = List.of(
                // WIPER 는 개월(12)만, 아직 남음
                record(ServiceType.WIPER, 20000, LocalDate.of(2026, 9, 1), 1L),
                // ENGINE_OIL 은 선언 순서가 앞이지만 지남
                record(ServiceType.ENGINE_OIL, 10000, LocalDate.of(2020, 1, 1), 2L),
                record(ServiceType.BRAKE_PAD, 20000, LocalDate.of(2026, 9, 1), 3L));

        List<NextService> results = compute(records, 21000);

        assertThat(results.get(0).type()).isEqualTo(ServiceType.ENGINE_OIL);
        assertThat(results.get(0).overdue()).isTrue();
        // 지나지 않은 것끼리는 선언 순서 유지(안정 정렬)
        assertThat(results).extracting(NextService::type)
                .containsExactly(ServiceType.ENGINE_OIL, ServiceType.BRAKE_PAD, ServiceType.WIPER);
    }

    @Test
    @DisplayName("같은 종류가 여럿이면 최신 하나만 본다")
    void latestPerType() {
        List<MaintenanceRecord> records = List.of(
                oil(10000, LocalDate.of(2020, 1, 1)),
                record(ServiceType.ENGINE_OIL, 30000, LocalDate.of(2026, 9, 1), 2L));

        assertThat(compute(records, 31000)).singleElement().satisfies(next -> {
            assertThat(next.lastOdometer()).isEqualTo(30000);
            // 최신 기록 기준 35,000km 라 아직. 옛 기록 기준이면 지남
            assertThat(next.overdue()).isFalse();
        });
    }

    @Test
    @DisplayName("이력이 없으면 빈 목록 — 15종을 '기록 없음' 으로 늘어놓지 않는다")
    void emptyWhenNoRecords() {
        assertThat(compute(List.of(), 10000)).isEmpty();
    }

    @Test
    @DisplayName("차량별 주기가 있으면 그것을 쓴다 — 합성유는 5,000km 가 아니다")
    void vehicleIntervalWins() {
        // 기본값(5,000km)이면 25,000km 에서 지남
        // 10,000km 설정 시 30,000km 까지 아직
        List<MaintenanceRecord> records = List.of(oil(20000, LocalDate.of(2026, 9, 1)));
        List<ServiceInterval> overrides =
                List.of(new ServiceInterval(vehicle, ServiceType.ENGINE_OIL, 10000, null));

        assertThat(NextService.of(records, overrides, 26000, TODAY))
                .singleElement()
                .satisfies(next -> {
                    assertThat(next.nextOdometer()).isEqualTo(30000);
                    assertThat(next.intervalKm()).isEqualTo(10000);
                    assertThat(next.overdue()).isFalse();
                    assertThat(next.customized()).isTrue();
                });
    }

    @Test
    @DisplayName("한쪽만 덮어쓰면 나머지는 기본값이 남는다")
    void partialOverrideKeepsDefault() {
        // 거리만 늘리고 기간은 기본값
        List<ServiceInterval> overrides =
                List.of(new ServiceInterval(vehicle, ServiceType.ENGINE_OIL, 10000, null));

        assertThat(NextService.of(List.of(oil(20000, LocalDate.of(2026, 1, 1))), overrides, 21000, TODAY))
                .singleElement()
                .satisfies(next -> {
                    assertThat(next.intervalMonths()).isEqualTo(6);
                    // 거리는 남았지만 6개월 경과 → 지남
                    assertThat(next.overdue()).isTrue();
                });
    }

    @Test
    @DisplayName("다른 종류의 설정은 섞이지 않는다")
    void overrideAppliesToItsTypeOnly() {
        List<MaintenanceRecord> records = List.of(
                oil(20000, LocalDate.of(2026, 9, 1)),
                record(ServiceType.BRAKE_PAD, 20000, LocalDate.of(2026, 9, 1), 2L));
        List<ServiceInterval> overrides =
                List.of(new ServiceInterval(vehicle, ServiceType.ENGINE_OIL, 10000, null));

        assertThat(NextService.of(records, overrides, 21000, TODAY))
                .filteredOn(next -> next.type() == ServiceType.BRAKE_PAD)
                .singleElement()
                .satisfies(next -> {
                    // BRAKE_PAD 기본 20,000km
                    assertThat(next.nextOdometer()).isEqualTo(40000);
                    assertThat(next.customized()).isFalse();
                });
    }
}
