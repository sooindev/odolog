package com.odolog.app.vehicle.service.application;

import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.maintenance.repository.jpa.MaintenanceRecordRepository;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.repository.jpa.UserRepository;
import com.odolog.app.vehicle.dto.request.odometer.UpdateOdometerRequest;
import com.odolog.app.vehicle.dto.request.register.VehicleRegisterRequest;
import com.odolog.app.vehicle.repository.jpa.VehicleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 트랜잭션 경계 검증. 유일하게 진짜 컨테이너를 띄우는 테스트
 * Mockito 는 스프링 프록시를 안 거쳐 Transactional 이 적용되지 않음 — readOnly 오설정도 초록불
 * 이 클래스에 Transactional 금지 — 서비스가 테스트 트랜잭션에 참여해 자기 설정이 무시됨
 */
@SpringBootTest
class VehicleServiceTransactionTest {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MaintenanceRecordRepository maintenanceRecordRepository;

    private Long ownerId;
    private Long vehicleId;
    // 서비스는 공개 id 로 부르고, 결과 확인은 숫자 PK 로 다시 읽는다
    private String vehiclePublicId;

    @BeforeEach
    void setUp() {
        ownerId = userRepository.save(
                new User("tx@odolog.com", "encoded-pw", "차주", "010-1111-2222")).getId();
        Vehicle registered = vehicleService.register(ownerId,
                new VehicleRegisterRequest("99하9999", "현대", "아반떼", 2020));
        vehicleId = registered.getId();
        vehiclePublicId = registered.getPublicId();
    }

    @AfterEach
    void tearDown() {
        // 롤백에 기댈 수 없어 직접 삭제
        maintenanceRecordRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("updateOdometer 는 save() 호출 없이 dirty checking 으로 DB 까지 반영된다")
    void updateOdometerIsFlushedToDatabase() {
        vehicleService.updateOdometer(ownerId, vehiclePublicId, new UpdateOdometerRequest(45000, null));

        // 서비스 트랜잭션이 끝난 뒤 새로 읽기. readOnly 였다면 UPDATE 가 안 나가 0 이 남음
        assertThat(vehicleRepository.findById(vehicleId).orElseThrow().getOdometer())
                .isEqualTo(45000);
    }
}
