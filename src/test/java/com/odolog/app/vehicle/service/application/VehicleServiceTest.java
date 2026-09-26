package com.odolog.app.vehicle.service.application;

import com.odolog.app.common.exception.type.ConflictException;
import com.odolog.app.common.exception.type.InvalidRequestException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.odolog.app.common.exception.type.ResourceNotFoundException;
import com.odolog.app.fuel.repository.jpa.FuelRecordRepository;
import com.odolog.app.maintenance.repository.jpa.MaintenanceRecordRepository;
import com.odolog.app.maintenance.repository.jpa.ServiceIntervalRepository;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.repository.jpa.UserRepository;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.vehicle.dto.request.odometer.UpdateOdometerRequest;
import com.odolog.app.vehicle.dto.request.register.VehicleRegisterRequest;
import com.odolog.app.vehicle.dto.request.update.VehicleUpdateRequest;
import com.odolog.app.vehicle.repository.jpa.VehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Mock
    private ServiceIntervalRepository serviceIntervalRepository;

    @Mock
    private FuelRecordRepository fuelRecordRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private User createOwner(Long id) {
        User owner = new User("owner@odolog.com", "encoded", "닉네임", "010-0000-0000");
        ReflectionTestUtils.setField(owner, "id", id);
        return owner;
    }

    private Vehicle createVehicle(Long id, User owner) {
        Vehicle vehicle = new Vehicle(owner, "12가3456", "현대", "아반떼", 2023);
        ReflectionTestUtils.setField(vehicle, "id", id);
        return vehicle;
    }

    @Test
    @DisplayName("같은 사용자가 이미 등록한 번호판이면 예외가 발생하고 저장하지 않는다")
    void registerDuplicatePlateNumber() {
        VehicleRegisterRequest request = new VehicleRegisterRequest("12가3456", "현대", "아반떼", 2023);
        when(vehicleRepository.existsByOwnerIdAndPlateNumber(1L, "12가3456")).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.register(1L, request))
                .isInstanceOf(ConflictException.class);

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("차량 등록 성공")
    void registerSuccess() {
        User owner = createOwner(1L);
        VehicleRegisterRequest request = new VehicleRegisterRequest("12가3456", "현대", "아반떼", 2023);
        when(vehicleRepository.existsByOwnerIdAndPlateNumber(1L, "12가3456")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vehicle saved = vehicleService.register(1L, request);

        assertThat(saved.getOwner()).isEqualTo(owner);
        assertThat(saved.getPlateNumber()).isEqualTo("12가3456");
    }

    @Test
    @DisplayName("허용 목록에 없는 속성으로 정렬하면 InvalidRequestException")
    void rejectsSortOutsideWhitelist() {
        /*
         * Spring Data 는 ?sort=owner.password 를 그대로 받아 암묵적 조인을 만든다.
         * 값이 응답에 실리지는 않지만 정렬 대상이 될 이유가 없다 —
         * 없는 속성만 400 이 되던 상태(PropertyReferenceException)로는 안 걸렸다.
         */
        assertThatThrownBy(() -> vehicleService.findMyVehicles(1L,
                PageRequest.of(0, 20, Sort.by("owner.password"))))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("허용 목록 안의 속성은 그대로 통과한다")
    void allowsWhitelistedSort() {
        when(vehicleRepository.findByOwnerId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThatCode(() -> vehicleService.findMyVehicles(1L,
                PageRequest.of(0, 20, Sort.by("plateNumber"))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("존재하지 않는 차량에 접근하면 ResourceNotFoundException")
    void findOwnedVehicleNotFound() {
        when(vehicleRepository.findByPublicId("V10")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.findOwnedVehicle(1L, "V10"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("본인 소유가 아닌 차량에 접근하면 없는 것처럼 ResourceNotFoundException")
    void findOwnedVehicleForbidden() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        when(vehicleRepository.findByPublicId("V10")).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.findOwnedVehicle(999L, "V10"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("없는 차량과 남의 차량은 메시지까지 같다")
    void hidesExistenceOfOthersVehicles() {
        // 상태 코드만 맞추고 문구가 다르면 그 문구가 존재 여부를 알려준다
        when(vehicleRepository.findByPublicId("V10")).thenReturn(Optional.of(createVehicle(10L, createOwner(1L))));
        when(vehicleRepository.findByPublicId("V11")).thenReturn(Optional.empty());

        String othersVehicle = catchThrowable(() -> vehicleService.findOwnedVehicle(999L, "V10")).getMessage();
        String missingVehicle = catchThrowable(() -> vehicleService.findOwnedVehicle(999L, "V11")).getMessage();

        // id 만 다르고 나머지는 같아야 한다 — 보낸 쪽이 이미 아는 값이다
        assertThat(othersVehicle).isEqualTo("존재하지 않는 차량입니다: V10");
        assertThat(missingVehicle).isEqualTo("존재하지 않는 차량입니다: V11");
    }

    @Test
    @DisplayName("주행거리가 감소하면 예외가 발생한다")
    void updateOdometerDecreaseFails() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        vehicle.updateOdometer(50000);
        when(vehicleRepository.findByPublicId("V10")).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.updateOdometer(1L, "V10", new UpdateOdometerRequest(40000, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("force 를 실으면 주행거리를 낮출 수 있다")
    void updateOdometerForcedDecrease() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        vehicle.updateOdometer(5000000);
        when(vehicleRepository.findByPublicId("V10")).thenReturn(Optional.of(vehicle));

        // 자리수를 잘못 넣은 뒤 고치는 경로. 이게 없으면 되돌릴 방법이 아예 없다
        vehicleService.updateOdometer(1L, "V10", new UpdateOdometerRequest(500000, true));

        assertThat(vehicle.getOdometer()).isEqualTo(500000);
    }

    @Test
    @DisplayName("force 가 false 면 여전히 감소를 막는다")
    void updateOdometerForceFalseStillBlocks() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        vehicle.updateOdometer(50000);
        when(vehicleRepository.findByPublicId("V10")).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.updateOdometer(1L, "V10", new UpdateOdometerRequest(40000, false)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("force 를 실어도 증가는 그대로 동작한다")
    void updateOdometerForcedIncrease() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        vehicle.updateOdometer(50000);
        when(vehicleRepository.findByPublicId("V10")).thenReturn(Optional.of(vehicle));

        vehicleService.updateOdometer(1L, "V10", new UpdateOdometerRequest(60000, true));

        assertThat(vehicle.getOdometer()).isEqualTo(60000);
    }

    @Test
    @DisplayName("차량 수정은 보낸 필드만 바꾸고 나머지는 건드리지 않는다")
    void updateChangesOnlyGivenFields() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        when(vehicleRepository.findByPublicId("V10")).thenReturn(Optional.of(vehicle));

        vehicleService.update(1L, "V10", new VehicleUpdateRequest(null, "기아", null, null));

        assertThat(vehicle.getManufacturer()).isEqualTo("기아");
        assertThat(vehicle.getPlateNumber()).isEqualTo("12가3456");
        assertThat(vehicle.getModelName()).isEqualTo("아반떼");
        assertThat(vehicle.getModelYear()).isEqualTo(2023);
    }

    @Test
    @DisplayName("번호판을 그대로 둔 채 다른 필드만 고치면 중복 검사를 아예 하지 않는다")
    void updateSkipsDuplicateCheckWhenPlateNumberUnchanged() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        when(vehicleRepository.findByPublicId("V10")).thenReturn(Optional.of(vehicle));

        // 번호판을 같은 값으로 전송. 자기를 빼지 않고 검사하면 409
        vehicleService.update(1L, "V10", new VehicleUpdateRequest("12가3456", "기아", null, null));

        verify(vehicleRepository, never()).existsByOwnerIdAndPlateNumber(any(), any());
        assertThat(vehicle.getManufacturer()).isEqualTo("기아");
    }

    @Test
    @DisplayName("번호판을 이미 가진 다른 차량의 번호로 바꾸면 예외가 발생한다")
    void updateDuplicatePlateNumberFails() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        when(vehicleRepository.findByPublicId("V10")).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.existsByOwnerIdAndPlateNumber(1L, "99하9999")).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.update(1L, "V10",
                new VehicleUpdateRequest("99하9999", null, null, null)))
                .isInstanceOf(ConflictException.class);

        assertThat(vehicle.getPlateNumber()).isEqualTo("12가3456");
    }

    @Test
    @DisplayName("남의 차량은 수정할 수 없다")
    void updateOtherUsersVehicleFails() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        when(vehicleRepository.findByPublicId("V10")).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.update(999L, "V10",
                new VehicleUpdateRequest(null, "기아", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("차량 삭제 시 정비 이력을 먼저 지운 뒤 차량을 지운다")
    void deleteRemovesMaintenanceRecordsBeforeVehicle() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        when(vehicleRepository.findByPublicId("V10")).thenReturn(Optional.of(vehicle));

        vehicleService.delete(1L, "V10");

        // 자식(정비·주유) 먼저, 차량 마지막
        InOrder order = inOrder(maintenanceRecordRepository, fuelRecordRepository, vehicleRepository);
        order.verify(maintenanceRecordRepository).deleteByVehicleId(10L);
        order.verify(fuelRecordRepository).deleteByVehicleId(10L);
        order.verify(vehicleRepository).delete(vehicle);
    }

    @Test
    @DisplayName("소유 차량 일괄 삭제는 차량마다 이력을 먼저 지운 뒤 차량을 한 번에 지운다")
    void deleteAllOwnedByRemovesRecordsFirst() {
        User owner = createOwner(1L);
        Vehicle first = createVehicle(10L, owner);
        Vehicle second = createVehicle(11L, owner);
        when(vehicleRepository.findAllByOwnerId(1L)).thenReturn(List.of(first, second));

        vehicleService.deleteAllOwnedBy(1L);

        InOrder order = inOrder(maintenanceRecordRepository, fuelRecordRepository, vehicleRepository);
        order.verify(maintenanceRecordRepository).deleteByVehicleId(10L);
        order.verify(fuelRecordRepository).deleteByVehicleId(10L);
        order.verify(maintenanceRecordRepository).deleteByVehicleId(11L);
        order.verify(fuelRecordRepository).deleteByVehicleId(11L);
        order.verify(vehicleRepository).deleteAll(List.of(first, second));
    }
}
