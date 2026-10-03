package com.odolog.app.garage.service;

import com.odolog.app.common.exception.type.InvalidRequestException;
import com.odolog.app.maintenance.repository.MaintenanceRecordRepository;
import com.odolog.app.maintenance.repository.ServiceIntervalRepository;
import com.odolog.app.user.service.UserToday;
import com.odolog.app.vehicle.VehicleRepository;
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
}
