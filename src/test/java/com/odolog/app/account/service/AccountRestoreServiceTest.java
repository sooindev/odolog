package com.odolog.app.account.service;

import com.odolog.app.account.dto.request.AccountRestoreRequest;
import com.odolog.app.account.dto.response.AccountRestoreResponse;
import com.odolog.app.common.exception.type.InvalidRequestException;
import com.odolog.app.fuel.domain.FuelRecord;
import com.odolog.app.fuel.FuelRecordRepository;
import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.entity.ServiceInterval;
import com.odolog.app.maintenance.domain.ServiceType;
import com.odolog.app.maintenance.repository.MaintenanceRecordRepository;
import com.odolog.app.maintenance.repository.ServiceIntervalRepository;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.service.application.UserService;
import com.odolog.app.user.service.UserToday;
import com.odolog.app.vehicle.Vehicle;
import com.odolog.app.vehicle.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @Mock
    private UserToday userToday;

    @InjectMocks
    private AccountRestoreService accountRestoreService;

    private final User owner = new User("me@odolog.com", "encoded", "나");

    /** 사용자의 오늘. 서비스가 한 번만 계산 */
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 29);

    @BeforeEach
    void setUpToday() {
        lenient().when(userToday.of(owner)).thenReturn(TODAY);
    }

    private AccountRestoreRequest.VehicleData vehicleData(String plate,
                                                          List<AccountRestoreRequest.MaintenanceData> maintenance,
                                                          List<AccountRestoreRequest.FuelData> fuels) {
        return new AccountRestoreRequest.VehicleData(plate, "기아", "카니발", 2020, 30000,
                maintenance, fuels, List.of());
    }

    private AccountRestoreRequest.MaintenanceData oilData(LocalDate date, int odometer) {
        return oilData(date, odometer, null);
    }

    private AccountRestoreRequest.MaintenanceData oilData(LocalDate date, int odometer, String currency) {
        return new AccountRestoreRequest.MaintenanceData(
                ServiceType.ENGINE_OIL, null, 80000, currency, odometer, date);
    }

    private AccountRestoreRequest.FuelData fuelData(LocalDate date, int odometer) {
        return new AccountRestoreRequest.FuelData(date, odometer, new BigDecimal("50.00"),
                90000, null, null, false);
    }

    /** 빈 계정 + 차량 저장 시 id 부여 */
    private void emptyAccount() {
        when(userService.findByIdForUpdate(1L)).thenReturn(owner);
        when(vehicleRepository.findLockedByOwnerIdAndPlateNumber(eq(1L), any())).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(call -> {
            Vehicle saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });
    }

    private MaintenanceRecord restoreOneOil(String currency) {
        emptyAccount();
        accountRestoreService.restore(1L, new AccountRestoreRequest(List.of(vehicleData("12가1212",
                List.of(oilData(LocalDate.of(2026, 5, 1), 30000, currency)), List.of()))));

        ArgumentCaptor<MaintenanceRecord> saved = ArgumentCaptor.forClass(MaintenanceRecord.class);
        verify(maintenanceRecordRepository).save(saved.capture());
        return saved.getValue();
    }

    @Test
    @DisplayName("통화 칸이 없는 옛 파일은 지금 설정이 달러여도 원화로 읽는다")
    void legacyFileIsKrw() {
        // 옛 파일은 전부 원화 시절. 지금 설정을 붙이면 50,000원이 $500.00
        owner.changeCurrency("USD");

        assertThat(restoreOneOil(null).getCurrency()).isEqualTo("KRW");
    }

    @Test
    @DisplayName("파일에 적힌 통화를 그대로 쓴다")
    void usesFileCurrency() {
        assertThat(restoreOneOil("USD").getCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("모르는 통화는 400")
    void rejectsUnknownCurrency() {
        assertThatThrownBy(() -> restoreOneOil("XYZ"))
                .isInstanceOf(InvalidRequestException.class);
    }

    private Vehicle existing(String plate, Long id) {
        Vehicle vehicle = new Vehicle(owner, plate, "기아", "카니발", 2020);
        ReflectionTestUtils.setField(vehicle, "id", id);
        return vehicle;
    }

    @Test
    @DisplayName("기록 하나라도 미래 날짜면 차량까지 아무것도 들어가지 않는다")
    void rejectsWholeFileWithFutureDate() {
        LocalDate tomorrow = TODAY.plusDays(1);
        when(userService.findByIdForUpdate(1L)).thenReturn(owner);

        assertThatThrownBy(() -> accountRestoreService.restore(1L,
                new AccountRestoreRequest(List.of(vehicleData("12가1212",
                        List.of(oilData(LocalDate.of(2026, 5, 1), 30000)),
                        List.of(fuelData(tomorrow, 30100)))))))
                .isInstanceOf(InvalidRequestException.class);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("빈 계정에 넣으면 차량과 기록이 그대로 들어간다")
    void restoresIntoEmptyAccount() {
        when(userService.findByIdForUpdate(1L)).thenReturn(owner);
        when(vehicleRepository.findLockedByOwnerIdAndPlateNumber(eq(1L), any())).thenReturn(Optional.empty());
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
        when(userService.findByIdForUpdate(1L)).thenReturn(owner);
        when(vehicleRepository.findLockedByOwnerIdAndPlateNumber(eq(1L), any())).thenReturn(Optional.of(vehicle));

        MaintenanceRecord already = new MaintenanceRecord(vehicle, ServiceType.ENGINE_OIL,
                null, 80000, "KRW", 30000, LocalDate.of(2026, 5, 1));
        FuelRecord alreadyFuel = new FuelRecord(vehicle, LocalDate.of(2026, 5, 2), 30100,
                new BigDecimal("50.00"), 90000, "KRW", null);
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
        when(userService.findByIdForUpdate(1L)).thenReturn(owner);
        when(vehicleRepository.findLockedByOwnerIdAndPlateNumber(eq(1L), any())).thenReturn(Optional.of(vehicle));
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
        when(userService.findByIdForUpdate(1L)).thenReturn(owner);
        when(vehicleRepository.findLockedByOwnerIdAndPlateNumber(eq(1L), any())).thenReturn(Optional.of(vehicle));
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
        when(userService.findByIdForUpdate(1L)).thenReturn(owner);
        when(vehicleRepository.findLockedByOwnerIdAndPlateNumber(eq(1L), any())).thenReturn(Optional.of(vehicle));
        when(maintenanceRecordRepository.findByVehicleIdOrderByServiceDateDescIdDesc(10L))
                .thenReturn(List.of());
        when(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(10L))
                .thenReturn(List.of());

        // 파일 값 30,000 < 현재 50,000. 감소 금지
        accountRestoreService.restore(1L,
                new AccountRestoreRequest(List.of(vehicleData("12가1212", List.of(), List.of()))));

        assertThat(vehicle.getOdometer()).isEqualTo(50000);
    }

    @Test
    @DisplayName("있던 차량은 파일의 차량 값으로 올리지 않는다 — 손으로 낮춘 주행거리를 되살리지 않게")
    void mergedVehicleIgnoresFileOdometer() {
        // 자리수 오타를 정정해 낮춘 차. 옛 백업에는 오타 값이 남아 있음
        Vehicle vehicle = existing("12가1212", 10L);
        vehicle.updateOdometer(40000);
        when(userService.findByIdForUpdate(1L)).thenReturn(owner);
        when(vehicleRepository.findLockedByOwnerIdAndPlateNumber(eq(1L), any())).thenReturn(Optional.of(vehicle));
        when(maintenanceRecordRepository.findByVehicleIdOrderByServiceDateDescIdDesc(10L)).thenReturn(List.of());
        // 오타 값의 기록은 이미 있어 건너뜀
        when(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(10L)).thenReturn(List.of(
                new FuelRecord(vehicle, LocalDate.of(2026, 5, 2), 1_234_560, new BigDecimal("50.00"), 90000, "KRW", null)));

        accountRestoreService.restore(1L, new AccountRestoreRequest(List.of(
                new AccountRestoreRequest.VehicleData("12가1212", "기아", "카니발", 2020, 1_234_560,
                        List.of(), List.of(fuelData(LocalDate.of(2026, 5, 2), 1_234_560)), List.of()))));

        assertThat(vehicle.getOdometer()).isEqualTo(40000);
    }

    @Test
    @DisplayName("있던 차량도 이번에 넣은 기록의 주행거리까지는 오른다")
    void mergedVehicleLiftsToAddedRecords() {
        Vehicle vehicle = existing("12가1212", 10L);
        vehicle.updateOdometer(40000);
        when(userService.findByIdForUpdate(1L)).thenReturn(owner);
        when(vehicleRepository.findLockedByOwnerIdAndPlateNumber(eq(1L), any())).thenReturn(Optional.of(vehicle));
        when(maintenanceRecordRepository.findByVehicleIdOrderByServiceDateDescIdDesc(10L)).thenReturn(List.of());
        when(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(10L)).thenReturn(List.of());

        accountRestoreService.restore(1L, new AccountRestoreRequest(List.of(vehicleData("12가1212",
                List.of(), List.of(fuelData(LocalDate.of(2026, 5, 2), 41000))))));

        assertThat(vehicle.getOdometer()).isEqualTo(41000);
    }

    @Test
    @DisplayName("금액이 같아도 통화가 다르면 다른 기록이다")
    void currencyIsPartOfDuplicateKey() {
        Vehicle vehicle = existing("12가1212", 10L);
        when(userService.findByIdForUpdate(1L)).thenReturn(owner);
        when(vehicleRepository.findLockedByOwnerIdAndPlateNumber(eq(1L), any())).thenReturn(Optional.of(vehicle));
        when(maintenanceRecordRepository.findByVehicleIdOrderByServiceDateDescIdDesc(10L)).thenReturn(List.of(
                new MaintenanceRecord(vehicle, ServiceType.ENGINE_OIL, null, 80000, "USD", 30000, LocalDate.of(2026, 5, 1))));
        when(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(10L)).thenReturn(List.of());

        AccountRestoreResponse result = accountRestoreService.restore(1L, new AccountRestoreRequest(List.of(
                vehicleData("12가1212", List.of(oilData(LocalDate.of(2026, 5, 1), 30000, "KRW")), List.of()))));

        assertThat(result.addedMaintenanceRecords()).isEqualTo(1);
        assertThat(result.skippedRecords()).isZero();
    }

    @Test
    @DisplayName("빈 주기 항목이 같은 종류의 뒤 항목을 막지 않는다")
    void emptyIntervalDoesNotShadowLaterOne() {
        emptyAccount();

        accountRestoreService.restore(1L, new AccountRestoreRequest(List.of(
                new AccountRestoreRequest.VehicleData("12가1212", "기아", "카니발", 2020, 30000, List.of(), List.of(),
                        List.of(new AccountRestoreRequest.IntervalData(ServiceType.ENGINE_OIL, null, null),
                                new AccountRestoreRequest.IntervalData(ServiceType.ENGINE_OIL, 10000, null))))));

        ArgumentCaptor<ServiceInterval> saved = ArgumentCaptor.forClass(ServiceInterval.class);
        verify(serviceIntervalRepository).save(saved.capture());
        assertThat(saved.getValue().getIntervalKm()).isEqualTo(10000);
    }

    private AccountRestoreRequest.FuelData resetPointData(LocalDate date, int odometer) {
        return new AccountRestoreRequest.FuelData(date, odometer, new BigDecimal("50.00"),
                90000, null, null, true);
    }

    @Test
    @DisplayName("새로 만든 차량은 파일의 연비 기준점을 그대로 쓴다")
    void keepsResetPointOnNewVehicle() {
        emptyAccount();

        accountRestoreService.restore(1L, new AccountRestoreRequest(List.of(vehicleData("12가1212",
                List.of(), List.of(resetPointData(LocalDate.of(2026, 5, 2), 30100))))));

        ArgumentCaptor<FuelRecord> saved = ArgumentCaptor.forClass(FuelRecord.class);
        verify(fuelRecordRepository).save(saved.capture());
        assertThat(saved.getValue().isResetPoint()).isTrue();
    }

    @Test
    @DisplayName("있던 차량에 붙일 때는 파일의 기준점을 무시한다 — 지금의 연비 기준 유지")
    void ignoresResetPointWhenMerging() {
        // 옛 백업의 기준점이 들어오면 사용자가 하지 않은 초기화가 생김
        Vehicle vehicle = existing("12가1212", 10L);
        when(userService.findByIdForUpdate(1L)).thenReturn(owner);
        when(vehicleRepository.findLockedByOwnerIdAndPlateNumber(eq(1L), any())).thenReturn(Optional.of(vehicle));
        when(maintenanceRecordRepository.findByVehicleIdOrderByServiceDateDescIdDesc(10L))
                .thenReturn(List.of());
        when(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(10L))
                .thenReturn(List.of());

        accountRestoreService.restore(1L, new AccountRestoreRequest(List.of(vehicleData("12가1212",
                List.of(), List.of(resetPointData(LocalDate.of(2026, 5, 2), 30100))))));

        ArgumentCaptor<FuelRecord> saved = ArgumentCaptor.forClass(FuelRecord.class);
        verify(fuelRecordRepository).save(saved.capture());
        assertThat(saved.getValue().isResetPoint()).isFalse();
    }

    @Test
    @DisplayName("파일 전체의 기록이 상한을 넘으면 아무것도 넣지 않고 400")
    void rejectsOversizedFile() {
        // 차량별 상한(5,000)은 지키되 차량 수로 곱해 커지는 경우
        when(userService.findByIdForUpdate(1L)).thenReturn(owner);
        List<AccountRestoreRequest.FuelData> fuels = IntStream.range(0, 5000)
                .mapToObj(i -> fuelData(LocalDate.of(2026, 5, 1), 30000 + i))
                .toList();
        List<AccountRestoreRequest.VehicleData> vehicles = IntStream.range(0, 5)
                .mapToObj(i -> vehicleData("12가000" + i, List.of(), fuels))
                .toList();

        assertThatThrownBy(() -> accountRestoreService.restore(1L, new AccountRestoreRequest(vehicles)))
                .isInstanceOf(InvalidRequestException.class);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("한 파일 안의 똑같은 기록 둘은 둘 다 넣는다 — 원래 계정에도 둘이었다(내보내기 → 가져오기가 그대로 돌아오게)")
    void keepsIdenticalRecordsWithinFile() {
        emptyAccount();

        AccountRestoreResponse result = accountRestoreService.restore(1L, new AccountRestoreRequest(List.of(
                vehicleData("12가1212",
                        List.of(oilData(LocalDate.of(2026, 5, 1), 30000), oilData(LocalDate.of(2026, 5, 1), 30000)),
                        List.of(fuelData(LocalDate.of(2026, 5, 2), 30100), fuelData(LocalDate.of(2026, 5, 2), 30100))))));

        assertThat(result.addedMaintenanceRecords()).isEqualTo(2);
        assertThat(result.addedFuelRecords()).isEqualTo(2);
        assertThat(result.skippedRecords()).isZero();
    }

    @Test
    @DisplayName("차량별 주기는 이미 정한 종류와 둘 다 빈 값을 건너뛴다")
    void restoresIntervals() {
        Vehicle vehicle = existing("12가1212", 10L);
        when(userService.findByIdForUpdate(1L)).thenReturn(owner);
        when(vehicleRepository.findLockedByOwnerIdAndPlateNumber(eq(1L), any())).thenReturn(Optional.of(vehicle));
        when(maintenanceRecordRepository.findByVehicleIdOrderByServiceDateDescIdDesc(10L))
                .thenReturn(List.of());
        when(fuelRecordRepository.findAllByVehicleIdOrderByOdometerAscIdAsc(10L))
                .thenReturn(List.of());
        when(serviceIntervalRepository.findByVehicleId(10L)).thenReturn(List.of(
                new ServiceInterval(vehicle, ServiceType.ENGINE_OIL, 10000, 12)));

        AccountRestoreResponse result = accountRestoreService.restore(1L, new AccountRestoreRequest(List.of(
                new AccountRestoreRequest.VehicleData("12가1212", "기아", "카니발", 2020, 30000,
                        List.of(), List.of(), List.of(
                        new AccountRestoreRequest.IntervalData(ServiceType.ENGINE_OIL, 15000, 12),
                        new AccountRestoreRequest.IntervalData(ServiceType.TIRE_ROTATION, null, null),
                        new AccountRestoreRequest.IntervalData(ServiceType.AIR_FILTER, 20000, null))))));

        assertThat(result.addedServiceIntervals()).isEqualTo(1);
        verify(serviceIntervalRepository).save(any(ServiceInterval.class));
    }

    @Test
    @DisplayName("새로 만든 차량은 파일 기록의 가장 큰 주행거리까지 올라간다 — 등록과 같은 규칙")
    void liftsNewVehicleToRecordOdometer() {
        when(userService.findByIdForUpdate(1L)).thenReturn(owner);
        when(vehicleRepository.findLockedByOwnerIdAndPlateNumber(eq(1L), any())).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(call -> {
            Vehicle saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });

        // 파일의 차량 주행거리는 30,000, 주유 기록은 30,100km
        accountRestoreService.restore(1L,
                new AccountRestoreRequest(List.of(vehicleData("12가1212",
                        List.of(oilData(LocalDate.of(2026, 5, 1), 30000)),
                        List.of(fuelData(LocalDate.of(2026, 5, 2), 30100))))));

        ArgumentCaptor<Vehicle> saved = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(saved.capture());
        assertThat(saved.getValue().getOdometer()).isEqualTo(30100);
    }

    @Test
    @DisplayName("같은 날·같은 주행거리라도 내용이 다른 기록은 둘 다 들어간다 — 파일 안의 기록끼리는 버리지 않는다")
    void keepsDistinctRecordsOnSameDayAndOdometer() {
        emptyAccount();
        LocalDate day = LocalDate.of(2026, 9, 1);

        AccountRestoreResponse result = accountRestoreService.restore(1L,
                new AccountRestoreRequest(List.of(vehicleData("12가1212",
                        List.of(new AccountRestoreRequest.MaintenanceData(ServiceType.OTHER, "와이퍼 전구", null, null, null, day),
                                new AccountRestoreRequest.MaintenanceData(ServiceType.OTHER, "경적 수리", 30000, null, null, day)),
                        List.of(new AccountRestoreRequest.FuelData(day, 31500, new BigDecimal("10.00"), 17000, null, null, false),
                                new AccountRestoreRequest.FuelData(day, 31500, new BigDecimal("20.00"), 34000, null, null, false))))));

        assertThat(result.addedMaintenanceRecords()).isEqualTo(2);
        assertThat(result.addedFuelRecords()).isEqualTo(2);
        assertThat(result.skippedRecords()).isZero();
    }

    @Test
    @DisplayName("정비는 날짜 오름차순으로 넣는다 — 최신순 파일을 그대로 넣으면 같은 날 기록의 선후가 뒤집힌다")
    void insertsMaintenanceOldestFirst() {
        emptyAccount();
        LocalDate day = LocalDate.of(2026, 9, 1);

        // 내보내기 순서(날짜·id 내림차순): 2,000km 가 나중 기록
        accountRestoreService.restore(1L,
                new AccountRestoreRequest(List.of(vehicleData("12가1212",
                        List.of(oilData(day, 2000), oilData(day, 1000), oilData(LocalDate.of(2026, 8, 1), 500)),
                        List.of()))));

        ArgumentCaptor<MaintenanceRecord> saved = ArgumentCaptor.forClass(MaintenanceRecord.class);
        verify(maintenanceRecordRepository, times(3)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(MaintenanceRecord::getServiceOdometer)
                .containsExactly(500, 1000, 2000);
    }

    @Test
    @DisplayName("짝 없는 서로게이트는 DB 가 저장하는 '?' 로 바꿔 넣는다 — 같은 파일을 다시 넣어도 늘지 않게")
    void normalizesLoneSurrogates() {
        emptyAccount();

        accountRestoreService.restore(1L,
                new AccountRestoreRequest(List.of(vehicleData("12가1212",
                        List.of(new AccountRestoreRequest.MaintenanceData(ServiceType.OTHER, "\ud800x", null, null, null,
                                LocalDate.of(2026, 9, 1))),
                        List.of()))));

        ArgumentCaptor<MaintenanceRecord> saved = ArgumentCaptor.forClass(MaintenanceRecord.class);
        verify(maintenanceRecordRepository).save(saved.capture());
        assertThat(saved.getValue().getDescription()).isEqualTo("?x");
    }
}
