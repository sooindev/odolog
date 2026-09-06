package com.odolog.app.maintenance.repository;

import com.odolog.app.maintenance.domain.MaintenanceRecord;
import com.odolog.app.maintenance.domain.ServiceType;
import com.odolog.app.user.domain.User;
import com.odolog.app.vehicle.domain.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MaintenanceRecordRepositoryTest {

    @Autowired
    private MaintenanceRecordRepository maintenanceRecordRepository;

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

    @Test
    @DisplayName("같은 날짜에 같은 종류를 두 번 정비했으면 나중에 등록한 것이 최신이다")
    void findTop_sameDate() {
        LocalDate sameDay = LocalDate.of(2026, 3, 1);

        // 먼저 등록한 것(잘못 입력한 값)
        em.persist(new MaintenanceRecord(vehicle, ServiceType.ENGINE_OIL, "오타", 50000, 10000, sameDay));
        // 나중에 등록한 것(고쳐서 다시 넣은 값)
        em.persist(new MaintenanceRecord(vehicle, ServiceType.ENGINE_OIL, "정정", 50000, 20000, sameDay));
        em.flush();
        em.clear();

        MaintenanceRecord latest = maintenanceRecordRepository
                .findTopByVehicleIdAndTypeOrderByServiceDateDescIdDesc(vehicle.getId(), ServiceType.ENGINE_OIL)
                .orElseThrow();

        assertThat(latest.getServiceOdometer()).isEqualTo(20000);
    }

    @Test
    @DisplayName("findByVehicleId 는 그 차량의 이력만 페이지 단위로 가져온다")
    void findByVehicleId() {
        em.persist(new MaintenanceRecord(vehicle, ServiceType.ENGINE_OIL, "엔진오일", 50000, 10000,
                LocalDate.of(2026, 1, 1)));
        em.persist(new MaintenanceRecord(vehicle, ServiceType.TIRE, "타이어", 300000, 12000,
                LocalDate.of(2026, 2, 1)));
        em.persist(new MaintenanceRecord(otherVehicle, ServiceType.TIRE, "남의 차", 300000, 5000,
                LocalDate.of(2026, 2, 1)));
        em.flush();
        em.clear();

        Page<MaintenanceRecord> firstPage = maintenanceRecordRepository.findByVehicleId(vehicle.getId(),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "serviceDate")));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(1);
        assertThat(firstPage.getContent().get(0).getType()).isEqualTo(ServiceType.TIRE);
        assertThat(firstPage.hasNext()).isTrue();
    }

    @Test
    @DisplayName("findByIdAndVehicleId 는 다른 차량 소속 이력을 찾지 못한다")
    void findByIdAndVehicleId() {
        MaintenanceRecord otherRecord = new MaintenanceRecord(otherVehicle, ServiceType.TIRE, "남의 차",
                300000, 5000, LocalDate.of(2026, 2, 1));
        em.persist(otherRecord);
        em.flush();
        em.clear();

        assertThat(maintenanceRecordRepository.findByIdAndVehicleId(otherRecord.getId(), otherVehicle.getId()))
                .isPresent();
        assertThat(maintenanceRecordRepository.findByIdAndVehicleId(otherRecord.getId(), vehicle.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("deleteByVehicleId 는 그 차량의 이력만 지운다")
    void deleteByVehicleId() {
        em.persist(new MaintenanceRecord(vehicle, ServiceType.ENGINE_OIL, "엔진오일", 50000, 10000,
                LocalDate.of(2026, 1, 1)));
        em.persist(new MaintenanceRecord(otherVehicle, ServiceType.TIRE, "남의 차", 300000, 5000,
                LocalDate.of(2026, 2, 1)));
        em.flush();

        maintenanceRecordRepository.deleteByVehicleId(vehicle.getId());
        em.flush();
        em.clear();

        Pageable all = PageRequest.of(0, 10, Sort.by("serviceDate"));
        assertThat(maintenanceRecordRepository.findByVehicleId(vehicle.getId(), all)).isEmpty();
        assertThat(maintenanceRecordRepository.findByVehicleId(otherVehicle.getId(), all)).hasSize(1);
    }
}
