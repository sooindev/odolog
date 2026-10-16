package com.odolog.app.maintenance;

import com.odolog.app.common.config.JpaAuditingConfig;
import com.odolog.app.maintenance.domain.ServiceInterval;
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
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
// @DataJpaTest 는 Auditing 설정 미포함. 없으면 created_at NOT NULL 위반
@Import(JpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ServiceIntervalRepositoryTest {

    @Autowired
    private ServiceIntervalRepository serviceIntervalRepository;

    @Autowired
    private TestEntityManager em;

    private User owner;
    private Vehicle vehicle;
    private Vehicle otherVehicle;

    @BeforeEach
    void setUp() {
        owner = new User("owner@odolog.com", "encoded-pw", "차주");
        em.persist(owner);
        vehicle = new Vehicle(owner, "12가3456", "현대", "아반떼", 2020);
        em.persist(vehicle);
        otherVehicle = new Vehicle(owner, "34나5678", "기아", "K5", 2022);
        em.persist(otherVehicle);
    }

    @Test
    @DisplayName("차량·종류로 찾고, 다른 차량의 같은 종류는 섞이지 않는다")
    void findsByVehicleAndType() {
        serviceIntervalRepository.save(new ServiceInterval(vehicle, ServiceType.ENGINE_OIL, 10000, 12));
        serviceIntervalRepository.save(new ServiceInterval(otherVehicle, ServiceType.ENGINE_OIL, 7000, null));
        em.flush();
        em.clear();

        assertThat(serviceIntervalRepository.findByVehicleIdAndType(vehicle.getId(), ServiceType.ENGINE_OIL))
                .get().extracting(ServiceInterval::getIntervalKm).isEqualTo(10000);
        assertThat(serviceIntervalRepository.findByVehicleIdAndType(vehicle.getId(), ServiceType.TIRE)).isEmpty();
        assertThat(serviceIntervalRepository.findByVehicleId(otherVehicle.getId())).hasSize(1);
        assertThat(serviceIntervalRepository.findByVehicle_Owner_Id(owner.getId())).hasSize(2);
    }

    @Test
    @DisplayName("한 차량의 같은 종류는 하나뿐 — 유니크 제약")
    void oneIntervalPerVehicleAndType() {
        serviceIntervalRepository.save(new ServiceInterval(vehicle, ServiceType.ENGINE_OIL, 10000, 12));

        assertThatThrownBy(() -> serviceIntervalRepository.saveAndFlush(
                new ServiceInterval(vehicle, ServiceType.ENGINE_OIL, 8000, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("차량 하나의 주기만 한 문장으로 지운다")
    void deletesOnlyThatVehicle() {
        serviceIntervalRepository.save(new ServiceInterval(vehicle, ServiceType.ENGINE_OIL, 10000, 12));
        serviceIntervalRepository.save(new ServiceInterval(vehicle, ServiceType.TIRE, 40000, null));
        serviceIntervalRepository.save(new ServiceInterval(otherVehicle, ServiceType.ENGINE_OIL, 7000, null));

        int deleted = serviceIntervalRepository.deleteByVehicleId(vehicle.getId());
        em.clear();

        assertThat(deleted).isEqualTo(2);
        assertThat(serviceIntervalRepository.findByVehicleId(vehicle.getId())).isEmpty();
        assertThat(serviceIntervalRepository.findByVehicleId(otherVehicle.getId())).hasSize(1);
    }
}
