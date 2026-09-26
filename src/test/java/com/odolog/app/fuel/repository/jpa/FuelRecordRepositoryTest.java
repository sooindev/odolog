package com.odolog.app.fuel.repository.jpa;

import com.odolog.app.common.config.jpa.JpaAuditingConfig;
import com.odolog.app.fuel.domain.entity.FuelRecord;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FuelRecordRepositoryTest {

    @Autowired
    private FuelRecordRepository fuelRecordRepository;

    @Autowired
    private TestEntityManager em;

    private Vehicle vehicle;
    private Vehicle otherVehicle;

    @BeforeEach
    void setUp() {
        User owner = new User("owner@odolog.com", "encoded-pw", "차주", "010-1111-2222");
        em.persist(owner);

        vehicle = new Vehicle(owner, "12가3456", "현대", "아반떼", 2020);
        em.persist(vehicle);

        otherVehicle = new Vehicle(owner, "34나5678", "기아", "K5", 2022);
        em.persist(otherVehicle);
    }

    private FuelRecord save(Vehicle target, int odometer, String liters) {
        FuelRecord record = new FuelRecord(target, LocalDate.of(2026, 9, 1), odometer,
                new BigDecimal(liters), 60000, null);
        em.persist(record);
        return record;
    }

    @Test
    @DisplayName("직전 주유는 주행거리가 더 작은 것 중 가장 큰 것이다")
    void findPrevious() {
        save(vehicle, 10000, "30.00");
        FuelRecord middle = save(vehicle, 10500, "31.00");
        FuelRecord latest = save(vehicle, 11000, "32.00");

        Optional<FuelRecord> previous = fuelRecordRepository
                .findPrevious(vehicle.getId(), 11000, latest.getId());

        assertThat(previous).isPresent();
        assertThat(previous.get().getId()).isEqualTo(middle.getId());
    }

    @Test
    @DisplayName("가장 오래된 기록에는 직전이 없다")
    void findPreviousOfOldest() {
        FuelRecord oldest = save(vehicle, 10000, "30.00");

        assertThat(fuelRecordRepository.findPrevious(vehicle.getId(), 10000, oldest.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("주행거리가 같으면 id 가 작은 쪽이 직전이다 — 목록 정렬과 같은 기준")
    void findPreviousBreaksTieById() {
        save(vehicle, 9500, "30.00");
        FuelRecord first = save(vehicle, 10000, "30.00");
        FuelRecord second = save(vehicle, 10000, "25.00");

        // 둘이 페이지 경계로 갈려도 second 의 짝은 first 다. 전에는 9500 을 잡아 같은 구간이 두 번 보였다
        assertThat(fuelRecordRepository.findPrevious(vehicle.getId(), 10000, second.getId()))
                .get().extracting(FuelRecord::getId).isEqualTo(first.getId());
    }

    @Test
    @DisplayName("직전 주유를 찾을 때 다른 차량의 기록은 보지 않는다")
    void findPreviousIgnoresOtherVehicle() {
        save(otherVehicle, 10900, "40.00");
        FuelRecord mine = save(vehicle, 10000, "30.00");

        Optional<FuelRecord> previous = fuelRecordRepository
                .findPrevious(vehicle.getId(), 11000, Long.MAX_VALUE);

        assertThat(previous).isPresent();
        assertThat(previous.get().getId()).isEqualTo(mine.getId());
    }

    @Test
    @DisplayName("소수점이 있는 주유량이 그대로 저장된다")
    void litersKeepsScale() {
        FuelRecord saved = save(vehicle, 10000, "32.45");
        em.flush();
        em.clear();

        FuelRecord found = fuelRecordRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getLiters()).isEqualByComparingTo("32.45");
    }

    @Test
    @DisplayName("공개 id 로 찾는다. 숫자 id 문자열로는 못 찾는다")
    void findByPublicId() {
        FuelRecord mine = save(vehicle, 10000, "30.00");

        assertThat(fuelRecordRepository.findByPublicIdAndVehicleId(mine.getPublicId(), vehicle.getId()))
                .isPresent();
        assertThat(fuelRecordRepository.findByPublicIdAndVehicleId(String.valueOf(mine.getId()), vehicle.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("findByPublicIdAndVehicleId 는 다른 차량 소속 기록을 찾지 못한다")
    void findByPublicIdAndVehicleIdBlocksOtherVehicle() {
        FuelRecord other = save(otherVehicle, 10000, "30.00");

        assertThat(fuelRecordRepository.findByPublicIdAndVehicleId(other.getPublicId(), vehicle.getId())).isEmpty();
        assertThat(fuelRecordRepository.findByPublicIdAndVehicleId(other.getPublicId(), otherVehicle.getId())).isPresent();
    }

    @Test
    @DisplayName("요약용 전체 조회는 주행거리 오름차순이다")
    void findAllAscending() {
        save(vehicle, 11000, "32.00");
        save(vehicle, 10000, "30.00");
        save(vehicle, 10500, "31.00");

        assertThat(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(vehicle.getId()))
                .extracting(FuelRecord::getOdometer)
                .containsExactly(10000, 10500, 11000);
    }

    @Test
    @DisplayName("deleteByVehicleId 는 그 차량의 주유 기록만 지운다")
    void deleteByVehicleId() {
        save(vehicle, 10000, "30.00");
        save(vehicle, 10500, "31.00");
        save(otherVehicle, 20000, "40.00");

        fuelRecordRepository.deleteByVehicleId(vehicle.getId());
        em.flush();

        assertThat(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(vehicle.getId())).isEmpty();
        assertThat(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(otherVehicle.getId())).hasSize(1);
    }
}
