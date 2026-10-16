package com.odolog.app.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odolog.app.account.dto.request.AccountRestoreRequest;
import com.odolog.app.account.dto.response.AccountRestoreResponse;
import com.odolog.app.fuel.FuelRecordRepository;
import com.odolog.app.fuel.domain.FuelRecord;
import com.odolog.app.maintenance.MaintenanceRecordRepository;
import com.odolog.app.maintenance.ServiceIntervalRepository;
import com.odolog.app.maintenance.domain.MaintenanceRecord;
import com.odolog.app.maintenance.domain.ServiceInterval;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 내보낸 JSON 을 그대로 다른 계정에 가져오면 같은 차·기록·주기·기준점이 생기는지
 * 두 쪽 DTO 가 따로 있어 한쪽에만 칸을 더하면 조용히 빠짐. 실제 JSON 을 거쳐 확인
 */
@SpringBootTest
class AccountExportRestoreRoundTripTest {

    @Autowired
    private AccountExportService accountExportService;

    @Autowired
    private AccountRestoreService accountRestoreService;

    @Autowired
    private ObjectMapper objectMapper;

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
    @DisplayName("내보낸 파일을 새 계정에 넣으면 연식 없는 차·정비·주유(덜 채움 포함)·기준점·차량별 주기가 그대로 생긴다")
    void exportThenRestoreRecreatesEverything() throws Exception {
        User source = userRepository.save(new User("source@odolog.com", "encoded-pw", "원본"));
        // 연식 없는 옛 차량. 가져오기가 연식을 필수로 받던 때는 이 파일이 통째로 400
        Vehicle vehicle = new Vehicle(source, "12가3456", "기아", "카니발", null);
        vehicle.updateOdometer(41000);
        vehicleRepository.save(vehicle);
        maintenanceRecordRepository.save(new MaintenanceRecord(vehicle, ServiceType.ENGINE_OIL, "합성유", 90000,
                "KRW", 40000, LocalDate.of(2026, 5, 1)));
        fuelRecordRepository.save(new FuelRecord(vehicle, LocalDate.of(2026, 8, 1), 40500,
                new BigDecimal("40.00"), 70000, "KRW", "첫 주유"));
        FuelRecord partial = new FuelRecord(vehicle, LocalDate.of(2026, 8, 15), 40700,
                new BigDecimal("10.00"), 18000, "KRW", null);
        partial.changeFullTank(false);
        fuelRecordRepository.save(partial);
        FuelRecord reset = new FuelRecord(vehicle, LocalDate.of(2026, 9, 1), 41000,
                new BigDecimal("35.50"), 62000, "KRW", null);
        reset.changeResetPoint(true);
        fuelRecordRepository.save(reset);
        serviceIntervalRepository.save(new ServiceInterval(vehicle, ServiceType.ENGINE_OIL, 10000, 12));

        // 화면이 하는 그대로: 받은 JSON 문자열을 다시 보냄
        String file = objectMapper.writeValueAsString(accountExportService.export(source.getId(), Instant.now()));
        AccountRestoreRequest request = objectMapper.readValue(file, AccountRestoreRequest.class);

        User target = userRepository.save(new User("target@odolog.com", "encoded-pw", "대상"));
        AccountRestoreResponse result = accountRestoreService.restore(target.getId(), request);

        assertThat(result.addedVehicles()).isEqualTo(1);
        assertThat(result.addedMaintenanceRecords()).isEqualTo(1);
        assertThat(result.addedFuelRecords()).isEqualTo(3);
        assertThat(result.addedServiceIntervals()).isEqualTo(1);

        Vehicle restored = vehicleRepository.findAllByOwnerId(target.getId()).get(0);
        assertThat(restored.getModelYear()).isNull();
        assertThat(restored.getOdometer()).isEqualTo(41000);

        List<FuelRecord> fuels = fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(restored.getId());
        assertThat(fuels).extracting(FuelRecord::getOdometer).containsExactly(40500, 40700, 41000);
        assertThat(fuels).extracting(FuelRecord::isResetPoint).containsExactly(false, false, true);
        assertThat(fuels).extracting(FuelRecord::isFullTank).containsExactly(true, false, true);
        assertThat(fuels.get(0).getMemo()).isEqualTo("첫 주유");

        assertThat(serviceIntervalRepository.findByVehicleId(restored.getId()))
                .singleElement()
                .satisfies(interval -> {
                    assertThat(interval.getType()).isEqualTo(ServiceType.ENGINE_OIL);
                    assertThat(interval.getIntervalKm()).isEqualTo(10000);
                    assertThat(interval.getIntervalMonths()).isEqualTo(12);
                });

        // 같은 파일을 한 번 더: 늘지 않고, 붙인 것이 없으니 "더한 차량" 도 0
        AccountRestoreResponse again = accountRestoreService.restore(target.getId(), request);
        assertThat(again.addedFuelRecords() + again.addedMaintenanceRecords() + again.addedServiceIntervals()).isZero();
        assertThat(again.mergedVehicles()).isZero();
        assertThat(again.skippedRecords()).isEqualTo(4);
    }
}
