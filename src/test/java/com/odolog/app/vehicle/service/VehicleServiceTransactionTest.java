package com.odolog.app.vehicle.service;

import com.odolog.app.maintenance.repository.MaintenanceRecordRepository;
import com.odolog.app.user.domain.User;
import com.odolog.app.user.repository.UserRepository;
import com.odolog.app.vehicle.dto.request.UpdateOdometerRequest;
import com.odolog.app.vehicle.dto.request.VehicleRegisterRequest;
import com.odolog.app.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 트랜잭션 경계가 실제로 동작하는지 확인한다.
 *
 * 다른 서비스 테스트는 Mockito 라 스프링 프록시를 거치지 않아 @Transactional 이 아예 적용되지
 * 않는다. 그래서 readOnly = true 를 잘못 붙여도 초록불이 뜬다. 여기만 진짜 컨테이너를 띄운다.
 *
 * 테스트 클래스에 @Transactional 을 붙이면 안 된다. 붙이는 순간 서비스가 테스트의 트랜잭션에
 * 참여해 버려서, 서비스 자신의 readOnly 설정이 무시되고 이 테스트가 아무것도 못 잡는다.
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

    @BeforeEach
    void setUp() {
        ownerId = userRepository.save(
                new User("tx@odolog.com", "encoded-pw", "차주", "010-1111-2222")).getId();
        vehicleId = vehicleService.register(ownerId,
                new VehicleRegisterRequest("99하9999", "현대", "아반떼", 2020)).getId();
    }

    @AfterEach
    void tearDown() {
        // 트랜잭션 롤백에 기댈 수 없으므로(@Transactional 을 못 쓴다) 직접 지운다.
        maintenanceRecordRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("updateOdometer 는 save() 호출 없이 dirty checking 으로 DB 까지 반영된다")
    void updateOdometerIsFlushedToDatabase() {
        vehicleService.updateOdometer(ownerId, vehicleId, new UpdateOdometerRequest(45000));

        // 서비스의 트랜잭션이 끝난 뒤 새로 읽는다.
        // readOnly = true 였다면 UPDATE 가 나가지 않아 등록 직후 값인 0 이 남아 있다.
        assertThat(vehicleRepository.findById(vehicleId).orElseThrow().getOdometer())
                .isEqualTo(45000);
    }
}
