package com.odolog.app.garage;

import com.odolog.app.common.exception.type.InvalidRequestException;
import com.odolog.app.maintenance.MaintenanceRecordRepository;
import com.odolog.app.maintenance.ServiceIntervalRepository;
import com.odolog.app.maintenance.domain.MaintenanceRecord;
import com.odolog.app.maintenance.domain.ServiceInterval;
import com.odolog.app.maintenance.domain.ServiceType;
import com.odolog.app.user.UserToday;
import com.odolog.app.user.domain.User;
import com.odolog.app.vehicle.VehicleRepository;
import com.odolog.app.vehicle.domain.Vehicle;
import com.odolog.app.vehicle.dto.response.VehicleResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleListServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Mock
    private ServiceIntervalRepository serviceIntervalRepository;

    @Mock
    private UserToday userToday;

    @InjectMocks
    private VehicleListService vehicleListService;

    @Test
    @DisplayName("허용 목록에 없는 속성으로 정렬하면 InvalidRequestException")
    void rejectsSortOutsideWhitelist() {
        // ?sort=owner.password 같은 연관 엔티티 정렬 차단
        assertThatThrownBy(() -> vehicleListService.findMyVehicles(1L,
                PageRequest.of(0, 20, Sort.by("owner.password"))))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("허용 목록 안의 속성은 그대로 통과한다")
    void allowsWhitelistedSort() {
        when(vehicleRepository.findByOwnerId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThatCode(() -> vehicleListService.findMyVehicles(1L,
                PageRequest.of(0, 20, Sort.by("plateNumber"))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("고른 정렬 뒤에 같은 방향의 id 를 붙인다 — 동점끼리 페이지마다 겹치거나 빠지지 않게")
    void appendsIdTieBreaker() {
        when(vehicleRepository.findByOwnerId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        vehicleListService.findMyVehicles(1L, PageRequest.of(2, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        ArgumentCaptor<Pageable> used = ArgumentCaptor.forClass(Pageable.class);
        verify(vehicleRepository).findByOwnerId(eq(1L), used.capture());
        assertThat(used.getValue().getPageNumber()).isEqualTo(2);
        assertThat(used.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
    }

    @Test
    @DisplayName("차량 목록의 지남·곧 수는 차량마다 따로 센다 — 차량별 주기도 그 차에만")
    void countsOverdueAndDueSoonPerVehicle() {
        User owner = new User("owner@odolog.com", "encoded", "차주");
        ReflectionTestUtils.setField(owner, "id", 1L);
        Vehicle overdueCar = new Vehicle(owner, "12가3456", "현대", "아반떼", 2020);
        ReflectionTestUtils.setField(overdueCar, "id", 10L);
        overdueCar.updateOdometer(50000);
        Vehicle soonCar = new Vehicle(owner, "34나5678", "기아", "K5", 2022);
        ReflectionTestUtils.setField(soonCar, "id", 20L);
        soonCar.updateOdometer(14500);

        LocalDate today = LocalDate.of(2026, 9, 17);
        Pageable pageable = PageRequest.of(0, 20);
        when(vehicleRepository.findByOwnerId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(overdueCar, soonCar), pageable, 2));
        when(maintenanceRecordRepository.findByVehicle_Owner_IdOrderByServiceDateDescIdDesc(1L)).thenReturn(List.of(
                new MaintenanceRecord(overdueCar, ServiceType.ENGINE_OIL, null, null, "KRW", 40000, LocalDate.of(2025, 1, 1)),
                new MaintenanceRecord(soonCar, ServiceType.ENGINE_OIL, null, null, "KRW", 10000, LocalDate.of(2026, 8, 1)),
                new MaintenanceRecord(soonCar, ServiceType.TIRE, null, null, "KRW", 10000, LocalDate.of(2026, 8, 1))));
        // 둘째 차의 타이어는 주기를 5,000km 로 줄여 곧. 첫 차에는 영향 없어야 함
        when(serviceIntervalRepository.findByVehicle_Owner_Id(1L))
                .thenReturn(List.of(new ServiceInterval(soonCar, ServiceType.TIRE, 5000, null)));
        when(userToday.of(1L)).thenReturn(today);

        List<VehicleResponse> vehicles = vehicleListService.findMyVehicles(1L, pageable).getContent();

        assertThat(vehicles.get(0).overdueServiceCount()).isEqualTo(1);
        assertThat(vehicles.get(0).dueSoonServiceCount()).isZero();
        assertThat(vehicles.get(1).overdueServiceCount()).isZero();
        assertThat(vehicles.get(1).dueSoonServiceCount()).isEqualTo(2);
    }
}
