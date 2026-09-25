package com.odolog.app.maintenance.domain.calculation;

import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.type.ServiceType;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 다음 정비 시점과 "지남" 판정
 * 차량 상세와 홈 요약이 이것을 공유하므로, 여기가 틀리면 두 화면이 함께 틀린다
 */
class NextServiceTest {

    private final Vehicle vehicle = new Vehicle(null, "12가3456", "현대", "아반떼", 2023);

    /** ENGINE_OIL 은 5,000km / 6개월 */
    private MaintenanceRecord oil(int odometer, LocalDate date) {
        return record(ServiceType.ENGINE_OIL, odometer, date, 1L);
    }

    private MaintenanceRecord record(ServiceType type, int odometer, LocalDate date, Long id) {
        MaintenanceRecord record = new MaintenanceRecord(vehicle, type, null, 80000, odometer, date);
        ReflectionTestUtils.setField(record, "id", id);
        return record;
    }

    @Test
    @DisplayName("주행거리를 넘겼으면 지난 것이다")
    void overdueByOdometer() {
        // 20,000km 에 갈았으니 다음은 25,000km. 지금 26,000km 다
        List<NextService> results = NextService.of(
                List.of(oil(20000, LocalDate.of(2026, 9, 1))), 26000, LocalDate.of(2026, 9, 25));

        assertThat(results).singleElement().satisfies(next -> {
            assertThat(next.nextOdometer()).isEqualTo(25000);
            assertThat(next.overdue()).isTrue();
        });
    }

    @Test
    @DisplayName("날짜를 넘겼으면 주행거리가 남아 있어도 지난 것이다")
    void overdueByDate() {
        /*
         * 권장 주기는 "km 또는 개월 중 먼저 오는 것" 이라 하나만 넘겨도 지난 것이다.
         * 차를 거의 안 타면 주행거리는 한참 남는데 오일은 그래도 굳는다
         */
        List<NextService> results = NextService.of(
                List.of(oil(20000, LocalDate.of(2026, 1, 1))), 20100, LocalDate.of(2026, 9, 25));

        assertThat(results).singleElement().satisfies(next -> {
            assertThat(next.nextDate()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(next.overdue()).isTrue();
        });
    }

    @Test
    @DisplayName("둘 다 안 넘겼으면 아직이다")
    void notOverdue() {
        List<NextService> results = NextService.of(
                List.of(oil(20000, LocalDate.of(2026, 9, 1))), 21000, LocalDate.of(2026, 9, 25));

        assertThat(results).singleElement()
                .satisfies(next -> assertThat(next.overdue()).isFalse());
    }

    @Test
    @DisplayName("딱 그 값·그 날이면 지난 것으로 본다 — '오늘까지' 가 아니라 '오늘이 그 날' 이다")
    void boundaryIsOverdue() {
        assertThat(NextService.of(List.of(oil(20000, LocalDate.of(2026, 9, 1))),
                25000, LocalDate.of(2026, 9, 25)).get(0).overdue()).isTrue();

        assertThat(NextService.of(List.of(oil(20000, LocalDate.of(2026, 3, 1))),
                20100, LocalDate.of(2026, 9, 1)).get(0).overdue()).isTrue();
    }

    @Test
    @DisplayName("주기가 없는 종류(OTHER)는 지날 수가 없다")
    void otherNeverOverdue() {
        List<NextService> results = NextService.of(
                List.of(record(ServiceType.OTHER, 10000, LocalDate.of(2020, 1, 1), 1L)),
                999999, LocalDate.of(2026, 9, 25));

        assertThat(results).singleElement().satisfies(next -> {
            assertThat(next.nextOdometer()).isNull();
            assertThat(next.nextDate()).isNull();
            assertThat(next.overdue()).isFalse();
        });
    }

    @Test
    @DisplayName("지난 것이 먼저 온다 — 이 목록은 '뭘 해야 하나' 를 보는 자리다")
    void overdueComesFirst() {
        List<NextService> results = NextService.of(List.of(
                // WIPER 는 개월만(12) — 아직 멀었다
                record(ServiceType.WIPER, 20000, LocalDate.of(2026, 9, 1), 1L),
                // ENGINE_OIL 은 선언 순서가 더 앞이지만 지났다
                record(ServiceType.ENGINE_OIL, 10000, LocalDate.of(2020, 1, 1), 2L),
                // BRAKE_PAD 도 아직
                record(ServiceType.BRAKE_PAD, 20000, LocalDate.of(2026, 9, 1), 3L)),
                21000, LocalDate.of(2026, 9, 25));

        assertThat(results.get(0).type()).isEqualTo(ServiceType.ENGINE_OIL);
        assertThat(results.get(0).overdue()).isTrue();
        // 지나지 않은 것끼리는 enum 선언 순서 그대로 — 안정 정렬이라야 화면이 안 흔들린다
        assertThat(results).extracting(NextService::type)
                .containsExactly(ServiceType.ENGINE_OIL, ServiceType.BRAKE_PAD, ServiceType.WIPER);
    }

    @Test
    @DisplayName("같은 종류가 여럿이면 최신 하나만 본다")
    void latestPerType() {
        List<NextService> results = NextService.of(List.of(
                oil(10000, LocalDate.of(2020, 1, 1)),
                record(ServiceType.ENGINE_OIL, 30000, LocalDate.of(2026, 9, 1), 2L)),
                31000, LocalDate.of(2026, 9, 25));

        assertThat(results).singleElement().satisfies(next -> {
            assertThat(next.lastOdometer()).isEqualTo(30000);
            // 최신 기록 기준이면 35,000km 라 아직이다. 옛 기록을 봤다면 지났다고 했을 것이다
            assertThat(next.overdue()).isFalse();
        });
    }

    @Test
    @DisplayName("이력이 없으면 빈 목록 — 15종을 '기록 없음' 으로 늘어놓지 않는다")
    void emptyWhenNoRecords() {
        assertThat(NextService.of(List.of(), 10000, LocalDate.of(2026, 9, 25))).isEmpty();
    }
}
