package com.odolog.app.account.service.application;

import com.odolog.app.account.dto.request.restore.AccountRestoreRequest;
import com.odolog.app.account.dto.response.restore.AccountRestoreResponse;
import com.odolog.app.fuel.domain.entity.FuelRecord;
import com.odolog.app.fuel.repository.jpa.FuelRecordRepository;
import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.type.ServiceType;
import com.odolog.app.maintenance.repository.jpa.MaintenanceRecordRepository;
import com.odolog.app.maintenance.repository.jpa.ServiceIntervalRepository;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.service.application.UserService;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.vehicle.repository.jpa.VehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 내보내기의 짝 */
@ExtendWith(MockitoExtension.class)
class AccountRestoreServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Mock
    private ServiceIntervalRepository serviceIntervalRepository;

    @Mock
    private FuelRecordRepository fuelRecordRepository;

    @InjectMocks
    private AccountRestoreService accountRestoreService;

    private final User owner = new User("me@odolog.com", "encoded", "나", null);

    private AccountRestoreRequest.VehicleData vehicleData(String plate,
                                                          List<AccountRestoreRequest.MaintenanceData> maintenance,
                                                          List<AccountRestoreRequest.FuelData> fuels) {
        return new AccountRestoreRequest.VehicleData(plate, "기아", "카니발", 2020, 30000,
                maintenance, fuels, List.of());
    }

    private AccountRestoreRequest.MaintenanceData oilData(LocalDate date, int odometer) {
        return new AccountRestoreRequest.MaintenanceData(
                ServiceType.ENGINE_OIL, null, 80000, odometer, date);
    }

    private AccountRestoreRequest.FuelData fuelData(LocalDate date, int odometer) {
        return new AccountRestoreRequest.FuelData(date, odometer, new BigDecimal("50.00"),
                90000, null, false);
    }

    private Vehicle existing(String plate, Long id) {
        Vehicle vehicle = new Vehicle(owner, plate, "기아", "카니발", 2020);
        ReflectionTestUtils.setField(vehicle, "id", id);
        return vehicle;
    }

    @Test
    @DisplayName("빈 계정에 넣으면 차량과 기록이 그대로 들어간다")
    void restoresIntoEmptyAccount() {
        when(userService.findById(1L)).thenReturn(owner);
        when(vehicleRepository.findAllByOwnerId(1L)).thenReturn(List.of());
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(call -> {
            Vehicle saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });

        AccountRestoreResponse result = accountRestoreService.restore(1L,
                new AccountRestoreRequest(List.of(vehicleData("12가1212",
                        List.of(oilData(LocalDate.of(2026, 5, 1), 30000)),
                        List.of(fuelData(LocalDate.of(2026, 5, 2), 30100))))));

        assertThat(result.addedVehicles()).isEqualTo(1);
        assertThat(result.addedMaintenanceRecords()).isEqualTo(1);
        assertThat(result.addedFuelRecords()).isEqualTo(1);
        assertThat(result.skippedRecords()).isZero();
    }

    @Test
    @DisplayName("같은 파일을 두 번 넣어도 두 배가 되지 않는다")
    void doesNotDuplicate() {
        // id 없는 JSON 이라 내용(종류·날짜·주행거리)으로 중복 판정
        Vehicle vehicle = existing("12가1212", 10L);
        when(userService.findById(1L)).thenReturn(owner);
        when(vehicleRepository.findAllByOwnerId(1L)).thenReturn(List.of(vehicle));

        MaintenanceRecord already = new MaintenanceRecord(vehicle, ServiceType.ENGINE_OIL,
                null, 80000, 30000, LocalDate.of(2026, 5, 1));
        FuelRecord alreadyFuel = new FuelRecord(vehicle, LocalDate.of(2026, 5, 2), 30100,
                new BigDecimal("50.00"), 90000, null);
        when(maintenanceRecordRepository.findByVehicleIdOrderByServiceDateDescIdDesc(10L))
                .thenReturn(List.of(already));
        when(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(10L))
                .thenReturn(List.of(alreadyFuel));

        AccountRestoreResponse result = accountRestoreService.restore(1L,
                new AccountRestoreRequest(List.of(vehicleData("12가1212",
                        List.of(oilData(LocalDate.of(2026, 5, 1), 30000)),
                        List.of(fuelData(LocalDate.of(2026, 5, 2), 30100))))));

        assertThat(result.addedVehicles()).isZero();
        assertThat(result.mergedVehicles()).isEqualTo(1);
        assertThat(result.skippedRecords()).isEqualTo(2);
        verify(maintenanceRecordRepository, never()).save(any());
        verify(fuelRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("같은 번호판이 있으면 차량 정보는 건드리지 않고 기록만 붙인다")
    void mergesIntoExistingVehicle() {
        // 옛 파일일 수 있어 기존 차량 정보 유지
        Vehicle vehicle = existing("12가1212", 10L);
        vehicle.changeModelName("카니발 하이리무진");
        when(userService.findById(1L)).thenReturn(owner);
        when(vehicleRepository.findAllByOwnerId(1L)).thenReturn(List.of(vehicle));
        when(maintenanceRecordRepository.findByVehicleIdOrderByServiceDateDescIdDesc(10L))
                .thenReturn(List.of());
        when(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(10L))
                .thenReturn(List.of());

        accountRestoreService.restore(1L, new AccountRestoreRequest(List.of(
                vehicleData("12가1212", List.of(oilData(LocalDate.of(2026, 5, 1), 30000)), List.of()))));

        assertThat(vehicle.getModelName()).isEqualTo("카니발 하이리무진");
        verify(vehicleRepository, never()).save(any(Vehicle.class));
        verify(maintenanceRecordRepository).save(any(MaintenanceRecord.class));
    }

    @Test
    @DisplayName("번호판이 공백만 달라도 같은 차로 본다 — DB 유니크 제약과 같은 기준")
    void matchesPlateIgnoringWhitespace() {
        // DB 는 "12가1212 ", 파일은 "12가1212". 새 차로 저장하면 유니크 위반으로 전체 실패
        Vehicle vehicle = existing("12가1212 ", 10L);
        when(userService.findById(1L)).thenReturn(owner);
        when(vehicleRepository.findAllByOwnerId(1L)).thenReturn(List.of(vehicle));
        when(maintenanceRecordRepository.findByVehicleIdOrderByServiceDateDescIdDesc(10L))
                .thenReturn(List.of());
        when(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(10L))
                .thenReturn(List.of());

        AccountRestoreResponse result = accountRestoreService.restore(1L, new AccountRestoreRequest(List.of(
                vehicleData("12가1212", List.of(oilData(LocalDate.of(2026, 5, 1), 30000)), List.of()))));

        assertThat(result.mergedVehicles()).isEqualTo(1);
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("차량 주행거리는 파일의 값까지 따라 오른다 — 내려가지는 않는다")
    void liftsOdometer() {
        Vehicle vehicle = existing("12가1212", 10L);
        vehicle.updateOdometer(50000);
        when(userService.findById(1L)).thenReturn(owner);
        when(vehicleRepository.findAllByOwnerId(1L)).thenReturn(List.of(vehicle));
        when(maintenanceRecordRepository.findByVehicleIdOrderByServiceDateDescIdDesc(10L))
                .thenReturn(List.of());
        when(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(10L))
                .thenReturn(List.of());

        // 파일 값 30,000 < 현재 50,000. 감소 금지
        accountRestoreService.restore(1L,
                new AccountRestoreRequest(List.of(vehicleData("12가1212", List.of(), List.of()))));

        assertThat(vehicle.getOdometer()).isEqualTo(50000);
    }
}
