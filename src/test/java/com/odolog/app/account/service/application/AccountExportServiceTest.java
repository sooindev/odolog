package com.odolog.app.account.service.application;

import com.odolog.app.account.dto.response.export.AccountExportResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountExportServiceTest {

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
    private AccountExportService accountExportService;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 21, 12, 0);

    private User owner;
    private Vehicle first;
    private Vehicle second;

    @BeforeEach
    void setUp() {
        owner = new User("me@odolog.com", "encoded", "닉네임", "010-0000-0000");
        ReflectionTestUtils.setField(owner, "id", 1L);

        first = new Vehicle(owner, "12가3456", "현대", "아반떼", 2020);
        first.updateOdometer(50_000);
        ReflectionTestUtils.setField(first, "id", 10L);

        second = new Vehicle(owner, "34나5678", "기아", "K5", 2022);
        second.updateOdometer(20_000);
        ReflectionTestUtils.setField(second, "id", 20L);
    }

    private MaintenanceRecord maintenance(Vehicle vehicle) {
        return new MaintenanceRecord(vehicle, ServiceType.ENGINE_OIL, "엔진오일", 80_000,
                45_000, LocalDate.of(2026, 7, 15));
    }

    private FuelRecord fuel(Vehicle vehicle) {
        return new FuelRecord(vehicle, LocalDate.of(2026, 8, 1), 48_000,
                new BigDecimal("32.45"), 60_000, "메모");
    }

    @Test
    @DisplayName("이력을 각 차량 밑으로 나눠 담는다")
    void nestsRecordsUnderOwningVehicle() {
        // 평평하게 내보내면 어느 기록이 어느 차의 것인지 우리 DB 안에서만 뜻이 있는 id 로만 알 수 있다
        when(userService.findById(1L)).thenReturn(owner);
        when(vehicleRepository.findAllByOwnerId(1L)).thenReturn(List.of(first, second));
        when(maintenanceRecordRepository.findByVehicle_Owner_IdOrderByServiceDateDescIdDesc(1L))
                .thenReturn(List.of(maintenance(first), maintenance(second), maintenance(second)));
        when(fuelRecordRepository.findByVehicle_Owner_IdOrderByOdometerAscIdAsc(1L))
                .thenReturn(List.of(fuel(first)));

        AccountExportResponse response = accountExportService.export(1L, NOW);

        assertThat(response.vehicles()).hasSize(2);
        assertThat(response.vehicles().get(0).plateNumber()).isEqualTo("12가3456");
        assertThat(response.vehicles().get(0).maintenanceRecords()).hasSize(1);
        assertThat(response.vehicles().get(0).fuelRecords()).hasSize(1);
        assertThat(response.vehicles().get(1).maintenanceRecords()).hasSize(2);
        assertThat(response.vehicles().get(1).fuelRecords()).isEmpty();
    }

    @Test
    @DisplayName("비밀번호 해시는 담지 않는다")
    void neverExportsPasswordHash() {
        // 백업에 넣을 이유가 없고 새어 나갈 경로만 늘린다
        when(userService.findById(1L)).thenReturn(owner);
        when(vehicleRepository.findAllByOwnerId(1L)).thenReturn(List.of());
        when(maintenanceRecordRepository.findByVehicle_Owner_IdOrderByServiceDateDescIdDesc(1L))
                .thenReturn(List.of());
        when(fuelRecordRepository.findByVehicle_Owner_IdOrderByOdometerAscIdAsc(1L))
                .thenReturn(List.of());

        AccountExportResponse response = accountExportService.export(1L, NOW);

        assertThat(response.user().email()).isEqualTo("me@odolog.com");
        assertThat(response.toString()).doesNotContain("encoded");
    }

    @Test
    @DisplayName("차량이 없어도 빈 목록으로 내려준다")
    void exportsEmptyAccount() {
        when(userService.findById(1L)).thenReturn(owner);
        when(vehicleRepository.findAllByOwnerId(1L)).thenReturn(List.of());
        when(maintenanceRecordRepository.findByVehicle_Owner_IdOrderByServiceDateDescIdDesc(1L))
                .thenReturn(List.of());
        when(fuelRecordRepository.findByVehicle_Owner_IdOrderByOdometerAscIdAsc(1L))
                .thenReturn(List.of());

        AccountExportResponse response = accountExportService.export(1L, NOW);

        assertThat(response.vehicles()).isEmpty();
        assertThat(response.exportedAt()).isEqualTo(NOW);
    }
}
