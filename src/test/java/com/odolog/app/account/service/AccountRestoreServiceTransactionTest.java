package com.odolog.app.account.service;

import com.odolog.app.account.dto.request.AccountRestoreRequest;
import com.odolog.app.account.dto.response.AccountRestoreResponse;
import com.odolog.app.fuel.FuelRecordRepository;
import com.odolog.app.maintenance.domain.ServiceType;
import com.odolog.app.maintenance.repository.MaintenanceRecordRepository;
import com.odolog.app.maintenance.repository.ServiceIntervalRepository;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.repository.UserRepository;
import com.odolog.app.vehicle.Vehicle;
import com.odolog.app.vehicle.VehicleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 같은 파일을 실제 DB 에 동시에 두 번 가져오기
 * Mockito 로는 잠금이 안 보임. 잠금이 없으면 둘 다 "기록 없음" 을 보고 두 배로 넣거나,
 * 차량 주행거리 갱신에서 데드락이 나 한쪽이 통째로 실패(잠금을 빼고 돌려 확인)
 * 이 클래스에 @Transactional 금지. 두 트랜잭션이 따로 돌아야 함
 */
@SpringBootTest
class AccountRestoreServiceTransactionTest {

    private static final int RECORDS = 200;

    @Autowired
    private AccountRestoreService accountRestoreService;

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
        // 자식 먼저
        fuelRecordRepository.deleteAll();
        serviceIntervalRepository.deleteAll();
        maintenanceRecordRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("같은 파일을 동시에 두 번 가져와도 기록이 두 배가 되지 않는다")
    void concurrentRestoresDoNotDuplicate() throws Exception {
        User user = userRepository.save(new User("restore@odolog.com", "encoded-pw", "가져오기"));
        // 있던 차량에 붙이는 경우. 차량 유니크 제약이 막아 주지 않는 쪽
        vehicleRepository.save(new Vehicle(user, "12가3456", "기아", "카니발", 2020));

        List<AccountRestoreRequest.FuelData> fuels = new ArrayList<>();
        for (int i = 0; i < RECORDS; i++) {
            fuels.add(new AccountRestoreRequest.FuelData(LocalDate.of(2025, 1, 1).plusDays(i),
                    30000 + i * 100, new BigDecimal("40.00"), 70000, "KRW", null, false));
        }
        AccountRestoreRequest request = new AccountRestoreRequest(List.of(new AccountRestoreRequest.VehicleData(
                "12가3456", "기아", "카니발", 2020, 30000,
                List.of(new AccountRestoreRequest.MaintenanceData(ServiceType.ENGINE_OIL, null, 80000, "KRW",
                        30000, LocalDate.of(2025, 1, 1))),
                fuels, List.of())));

        // 두 요청을 같은 순간에 출발
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<AccountRestoreResponse>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return accountRestoreService.restore(user.getId(), request);
                }));
            }
            int added = 0;
            int skipped = 0;
            for (Future<AccountRestoreResponse> result : results) {
                AccountRestoreResponse response = result.get(60, TimeUnit.SECONDS);
                added += response.addedFuelRecords();
                skipped += response.skippedRecords();
            }

            // 한쪽이 넣고 다른 쪽은 전부 건너뜀
            assertThat(fuelRecordRepository.count()).isEqualTo(RECORDS);
            assertThat(maintenanceRecordRepository.count()).isEqualTo(1);
            assertThat(added).isEqualTo(RECORDS);
            assertThat(skipped).isEqualTo(RECORDS + 1);
        } finally {
            pool.shutdownNow();
        }
    }
}
