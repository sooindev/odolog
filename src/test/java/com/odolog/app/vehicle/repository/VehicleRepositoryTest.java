package com.odolog.app.vehicle.repository;

import com.odolog.app.common.exception.ConflictException;
import com.odolog.app.user.domain.User;
import com.odolog.app.vehicle.domain.Vehicle;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    @DisplayName("findByOwnerId 는 owner 의 id 로 페이지 단위 조회를 한다")
    void findByOwnerId() {
        em.persist(new Vehicle(owner, "12가3456", "현대", "아반떼", 2020));
        em.persist(new Vehicle(owner, "34나5678", "기아", "K5", 2022));
        em.flush();
        em.clear();

        Page<Vehicle> firstPage = vehicleRepository.findByOwnerId(owner.getId(),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(firstPage.getContent()).hasSize(1);
        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.hasNext()).isTrue();
    }

    @Test
    @DisplayName("번호판 중복은 소유자 안에서만 따진다 — 다른 사람이 같은 번호판을 써도 막지 않는다")
    void existsByOwnerIdAndPlateNumber() {
        User another = new User("another@odolog.com", "encoded-pw", "다른차주", "010-3333-4444");
        em.persist(another);
        em.persist(new Vehicle(owner, "12가3456", "현대", "아반떼", 2020));
        em.flush();
        em.clear();

        assertThat(vehicleRepository.existsByOwnerIdAndPlateNumber(owner.getId(), "12가3456")).isTrue();
        assertThat(vehicleRepository.existsByOwnerIdAndPlateNumber(owner.getId(), "99하9999")).isFalse();
        // 같은 번호판이라도 주인이 다르면 중복이 아니다 (중고차 이전 / 가족 공유 차량)
        assertThat(vehicleRepository.existsByOwnerIdAndPlateNumber(another.getId(), "12가3456")).isFalse();
    }

    @Test
    @DisplayName("다른 사용자는 같은 번호판을 등록할 수 있다 (복합 유니크 제약 확인)")
    void samePlateNumberForDifferentOwners() {
        User another = new User("another@odolog.com", "encoded-pw", "다른차주", "010-3333-4444");
        em.persist(another);

        em.persist(new Vehicle(owner, "12가3456", "현대", "아반떼", 2020));
        em.persist(new Vehicle(another, "12가3456", "현대", "아반떼", 2020));

        // 전역 유니크였다면 여기서 제약 위반으로 터진다
        em.flush();

        assertThat(vehicleRepository.count()).isEqualTo(2);
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
                .isInstanceOf(ConflictException.class);
    }
}
