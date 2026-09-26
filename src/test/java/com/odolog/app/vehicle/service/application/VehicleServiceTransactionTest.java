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
 * 트랜잭션 경계 검증. 실제 컨테이너를 띄우는 유일한 서비스 테스트
 * Mockito 는 @Transactional 미적용이라 readOnly 오설정을 못 잡음
 * 이 클래스에 @Transactional 금지. 서비스 설정이 무시됨
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
    // 서비스는 공개 id 로 호출, 확인은 숫자 PK 로 재조회
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
        // 롤백이 없어 직접 삭제
        maintenanceRecordRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("updateOdometer 는 save() 호출 없이 dirty checking 으로 DB 까지 반영된다")
    void updateOdometerIsFlushedToDatabase() {
        vehicleService.updateOdometer(ownerId, vehiclePublicId, new UpdateOdometerRequest(45000, null));

        // 트랜잭션 종료 후 재조회. readOnly 였다면 0 유지
        assertThat(vehicleRepository.findById(vehicleId).orElseThrow().getOdometer())
                .isEqualTo(45000);
    }
}
