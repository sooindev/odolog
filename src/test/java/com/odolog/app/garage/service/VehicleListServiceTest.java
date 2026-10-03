package com.odolog.app.garage.service;

import com.odolog.app.common.exception.type.InvalidRequestException;
import com.odolog.app.maintenance.repository.MaintenanceRecordRepository;
import com.odolog.app.maintenance.repository.ServiceIntervalRepository;
import com.odolog.app.user.service.UserToday;
import com.odolog.app.vehicle.VehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
}
