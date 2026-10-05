package com.odolog.app.garage;

import com.odolog.app.common.exception.type.ResourceNotFoundException;
import com.odolog.app.fuel.FuelRecordRepository;
import com.odolog.app.fuel.FuelRecordService;
import com.odolog.app.fuel.dto.request.FuelRecordRegisterRequest;
import com.odolog.app.maintenance.MaintenanceRecordRepository;
import com.odolog.app.maintenance.ServiceIntervalRepository;
import com.odolog.app.maintenance.domain.MaintenanceRecord;
import com.odolog.app.maintenance.domain.ServiceType;
import com.odolog.app.user.UserRepository;
import com.odolog.app.user.domain.User;
import com.odolog.app.vehicle.VehicleRepository;
import com.odolog.app.vehicle.domain.Vehicle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 차량을 지우는 동안 그 차에 기록을 넣기. 실제 DB 로
 * 잠금이 없으면 삭제가 읽은 뒤 들어온 기록이 남아 마지막 차량 DELETE 가 FK 로 실패(500)
 * 이 클래스에 @Transactional 금지. 두 트랜잭션이 따로 돌아야 함
 */
@SpringBootTest
class VehicleRemovalServiceTransactionTest {

    /** 삭제가 오래 걸리게 해서 그 사이에 기록이 끼어들 틈을 만듦 */
    private static final int EXISTING_RECORDS = 2000;

    @Autowired
    private VehicleRemovalService vehicleRemovalService;

    @Autowired
    private FuelRecordService fuelRecordService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Autowired
    private ServiceIntervalRepository serviceIntervalRepository;

    @Autowired
    private FuelRecordRepository fuelRecordRepository;

    @AfterEach
    void tearDown() {
        // 실패했을 때의 잔여분. 자식 먼저
        fuelRecordRepository.deleteAll();
        serviceIntervalRepository.deleteAll();
        maintenanceRecordRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("차량 삭제와 기록 추가가 겹쳐도 FK 로 실패하지 않는다 — 추가는 404 이거나, 삭제가 그 기록까지 지운다")
    void deleteAndInsertDoNotCollide() throws Exception {
        User user = userRepository.save(new User("remove@odolog.com", "encoded-pw", "삭제"));
        Vehicle vehicle = vehicleRepository.save(new Vehicle(user, "12가3456", "기아", "카니발", 2020));
        vehicle.liftOdometerTo(100_000);
        vehicleRepository.save(vehicle);
        List<MaintenanceRecord> records = new ArrayList<>();
        for (int i = 0; i < EXISTING_RECORDS; i++) {
            records.add(new MaintenanceRecord(vehicle, ServiceType.OTHER, null, null, "KRW", null,
                    LocalDate.of(2025, 1, 1)));
        }
        maintenanceRecordRepository.saveAll(records);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> removal = pool.submit(() -> vehicleRemovalService.delete(user.getId(), vehicle.getPublicId()));
            // 삭제가 자식 기록을 지우는 중에 도착
            Thread.sleep(30);
            Future<?> insert = pool.submit(() -> fuelRecordService.register(user.getId(), vehicle.getPublicId(),
                    new FuelRecordRegisterRequest(LocalDate.of(2025, 2, 1), 50, new BigDecimal("40.00"), 70000, null)));

            removal.get(60, TimeUnit.SECONDS);
            try {
                insert.get(60, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                // 삭제가 먼저 끝났으면 그 차는 없음
                assertThat(e.getCause()).isInstanceOf(ResourceNotFoundException.class);
            }

            // 어느 순서든 남는 것이 없음
            assertThat(vehicleRepository.count()).isZero();
            assertThat(fuelRecordRepository.count()).isZero();
            assertThat(maintenanceRecordRepository.count()).isZero();
        } finally {
            pool.shutdownNow();
        }
    }
}
