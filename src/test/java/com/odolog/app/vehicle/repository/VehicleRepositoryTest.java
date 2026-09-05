package com.odolog.app.vehicle.repository;

import com.odolog.app.user.domain.User;
import com.odolog.app.vehicle.domain.Vehicle;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VehicleRepositoryTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TestEntityManager em;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User("owner@odolog.com", "encoded-pw", "차주", "010-1111-2222");
        em.persist(owner);
    }

    @Test
    @DisplayName("차량을 저장하면 id / createdAt 이 채워지고 odometer 는 0으로 시작한다")
    void save() {
        Vehicle vehicle = new Vehicle(owner, "12가3456", "현대", "아반떼", 2020);

        Vehicle saved = vehicleRepository.save(vehicle);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(saved.getOdometer()).isZero();
    }

    @Test
    @DisplayName("findByOwner 는 그 사람의 차량만 가져온다")
    void findByOwner() {
        User other = new User("other@odolog.com", "encoded-pw", "다른차주", null);
        em.persist(other);

        em.persist(new Vehicle(owner, "12가3456", "현대", "아반떼", 2020));
        em.persist(new Vehicle(owner, "34나5678", "기아", "K5", 2022));
        em.persist(new Vehicle(other, "56다7890", "쉐보레", "트랙스", 2021));
        em.flush();
        em.clear();

        List<Vehicle> mine = vehicleRepository.findByOwner(owner);

        assertThat(mine).hasSize(2)
                .extracting(Vehicle::getPlateNumber)
                .containsExactlyInAnyOrder("12가3456", "34나5678");
    }

    @Test
    @DisplayName("findByOwnerIdOrderByCreatedAtDesc 는 owner 의 id 로도 조회할 수 있다")
    void findByOwnerId() {
        em.persist(new Vehicle(owner, "12가3456", "현대", "아반떼", 2020));
        em.persist(new Vehicle(owner, "34나5678", "기아", "K5", 2022));
        em.flush();
        em.clear();

        List<Vehicle> found = vehicleRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId());

        assertThat(found).hasSize(2);
    }

    @Test
    @DisplayName("번호판으로 중복 여부를 확인할 수 있다")
    void findByPlateNumber() {
        em.persist(new Vehicle(owner, "12가3456", "현대", "아반떼", 2020));
        em.flush();
        em.clear();

        assertThat(vehicleRepository.existsByPlateNumber("12가3456")).isTrue();
        assertThat(vehicleRepository.existsByPlateNumber("99하9999")).isFalse();
        assertThat(vehicleRepository.findByPlateNumber("12가3456"))
                .isPresent()
                .get()
                .extracting(Vehicle::getModelName)
                .isEqualTo("아반떼");
    }

    @Test
    @DisplayName("owner 는 LAZY 라서 차량만 조회하면 아직 초기화되지 않는다")
    void ownerIsLazy() {
        Long vehicleId = em.persistAndGetId(
                new Vehicle(owner, "12가3456", "현대", "아반떼", 2020), Long.class);
        em.flush();
        em.clear();

        Vehicle found = vehicleRepository.findById(vehicleId).orElseThrow();

        assertThat(Hibernate.isInitialized(found.getOwner())).isFalse();
        assertThat(found.getOwner().getNickname()).isEqualTo("차주");
        assertThat(Hibernate.isInitialized(found.getOwner())).isTrue();
    }

    @Test
    @DisplayName("updateOdometer 는 값을 올려주지만, 줄이려 하면 예외를 던진다")
    void updateOdometer() {
        Vehicle vehicle = new Vehicle(owner, "12가3456", "현대", "아반떼", 2020);
        em.persist(vehicle);
        em.flush();

        vehicle.updateOdometer(15000);
        em.flush();

        assertThat(vehicle.getOdometer()).isEqualTo(15000);

        assertThatThrownBy(() -> vehicle.updateOdometer(14999))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
