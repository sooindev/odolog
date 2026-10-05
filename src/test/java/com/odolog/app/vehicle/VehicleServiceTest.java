package com.odolog.app.vehicle;

import com.odolog.app.common.exception.type.ConflictException;
import com.odolog.app.common.exception.type.ResourceNotFoundException;
import com.odolog.app.user.UserRepository;
import com.odolog.app.user.domain.User;
import com.odolog.app.vehicle.domain.Vehicle;
import com.odolog.app.vehicle.dto.request.UpdateOdometerRequest;
import com.odolog.app.vehicle.dto.request.VehicleRegisterRequest;
import com.odolog.app.vehicle.dto.request.VehicleUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private User createOwner(Long id) {
        User owner = new User("owner@odolog.com", "encoded", "닉네임");
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
        VehicleRegisterRequest request = new VehicleRegisterRequest("12가3456", "현대", "아반떼", 2023, 45000);
        when(vehicleRepository.existsByOwnerIdAndPlateNumber(1L, "12가3456")).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.register(1L, request))
                .isInstanceOf(ConflictException.class);

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("차량 등록 성공")
    void registerSuccess() {
        User owner = createOwner(1L);
        VehicleRegisterRequest request = new VehicleRegisterRequest("12가3456", "현대", "아반떼", 2023, 45000);
        when(vehicleRepository.existsByOwnerIdAndPlateNumber(1L, "12가3456")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vehicle saved = vehicleService.register(1L, request);

        assertThat(saved.getOwner()).isEqualTo(owner);
        assertThat(saved.getPlateNumber()).isEqualTo("12가3456");
        // 타던 차는 지금 계기판 값에서 시작
        assertThat(saved.getOdometer()).isEqualTo(45000);
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
        // 문구까지 같아야 존재 여부가 드러나지 않음
        when(vehicleRepository.findByPublicId("V10")).thenReturn(Optional.of(createVehicle(10L, createOwner(1L))));
        when(vehicleRepository.findByPublicId("V11")).thenReturn(Optional.empty());

        String othersVehicle = catchThrowable(() -> vehicleService.findOwnedVehicle(999L, "V10")).getMessage();
        String missingVehicle = catchThrowable(() -> vehicleService.findOwnedVehicle(999L, "V11")).getMessage();

        // id 만 다르고 나머지 동일
        assertThat(othersVehicle).isEqualTo("존재하지 않는 차량입니다: V10");
        assertThat(missingVehicle).isEqualTo("존재하지 않는 차량입니다: V11");
    }

    @Test
    @DisplayName("주행거리가 감소하면 예외가 발생한다")
    void updateOdometerDecreaseFails() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        vehicle.updateOdometer(50000);
        when(vehicleRepository.findLockedByPublicId("V10")).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.updateOdometer(1L, "V10", new UpdateOdometerRequest(40000, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("force 를 실으면 주행거리를 낮출 수 있다")
    void updateOdometerForcedDecrease() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        vehicle.updateOdometer(5000000);
        when(vehicleRepository.findLockedByPublicId("V10")).thenReturn(Optional.of(vehicle));

        // 자리수 오타 복구 경로
        vehicleService.updateOdometer(1L, "V10", new UpdateOdometerRequest(500000, true));

        assertThat(vehicle.getOdometer()).isEqualTo(500000);
    }

    @Test
    @DisplayName("force 가 false 면 여전히 감소를 막는다")
    void updateOdometerForceFalseStillBlocks() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        vehicle.updateOdometer(50000);
        when(vehicleRepository.findLockedByPublicId("V10")).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.updateOdometer(1L, "V10", new UpdateOdometerRequest(40000, false)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("force 를 실어도 증가는 그대로 동작한다")
    void updateOdometerForcedIncrease() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        vehicle.updateOdometer(50000);
        when(vehicleRepository.findLockedByPublicId("V10")).thenReturn(Optional.of(vehicle));

        vehicleService.updateOdometer(1L, "V10", new UpdateOdometerRequest(60000, true));

        assertThat(vehicle.getOdometer()).isEqualTo(60000);
    }

    @Test
    @DisplayName("차량 수정은 보낸 필드만 바꾸고 나머지는 건드리지 않는다")
    void updateChangesOnlyGivenFields() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        when(vehicleRepository.findLockedByPublicId("V10")).thenReturn(Optional.of(vehicle));

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
        when(vehicleRepository.findLockedByPublicId("V10")).thenReturn(Optional.of(vehicle));

        // 같은 번호판 전송. 자기 자신 중복 판정 방지
        vehicleService.update(1L, "V10", new VehicleUpdateRequest("12가3456", "기아", null, null));

        verify(vehicleRepository, never()).existsByOwnerIdAndPlateNumber(any(), any());
        assertThat(vehicle.getManufacturer()).isEqualTo("기아");
    }

    @Test
    @DisplayName("등록할 때 앞뒤 공백을 잘라 저장한다 — 앞 공백 번호판이 같은 번호판 두 대가 되지 않게")
    void registerStripsWhitespace() {
        when(vehicleRepository.existsByOwnerIdAndPlateNumber(1L, "12가3456")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(createOwner(1L)));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vehicle saved = vehicleService.register(1L,
                new VehicleRegisterRequest(" 12가3456 ", "현대\u3000", " 아반떼", 2023, 45000));

        // 중복 검사도 자른 값 기준
        assertThat(saved.getPlateNumber()).isEqualTo("12가3456");
        // 전각 공백(U+3000)까지 제거
        assertThat(saved.getManufacturer()).isEqualTo("현대");
        assertThat(saved.getModelName()).isEqualTo("아반떼");
    }

    @Test
    @DisplayName("공백만 지우는 번호판 수정은 자기 자신과 중복으로 잡지 않는다")
    void updateOnlyWhitespaceIsNotDuplicate() {
        // DB 는 뒤 공백 무시 비교라 검사 시 자기 자신과 충돌
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        vehicle.changePlateNumber("12가3456 ");
        when(vehicleRepository.findLockedByPublicId("V10")).thenReturn(Optional.of(vehicle));

        vehicleService.update(1L, "V10", new VehicleUpdateRequest("12가3456", null, null, null));

        // 판단은 DB 에. 자기 자신은 빼고 묻는다
        verify(vehicleRepository).existsByOwnerIdAndPlateNumberAndIdNot(1L, "12가3456", 10L);
        assertThat(vehicle.getPlateNumber()).isEqualTo("12가3456");
    }

    @Test
    @DisplayName("번호판을 이미 가진 다른 차량의 번호로 바꾸면 예외가 발생한다")
    void updateDuplicatePlateNumberFails() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        when(vehicleRepository.findLockedByPublicId("V10")).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.existsByOwnerIdAndPlateNumberAndIdNot(1L, "99하9999", 10L)).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.update(1L, "V10",
                new VehicleUpdateRequest("99하9999", null, null, null)))
                .isInstanceOf(ConflictException.class);

        assertThat(vehicle.getPlateNumber()).isEqualTo("12가3456");
    }

    @Test
    @DisplayName("남의 차량은 수정할 수 없다")
    void updateOtherUsersVehicleFails() {
        Vehicle vehicle = createVehicle(10L, createOwner(1L));
        when(vehicleRepository.findLockedByPublicId("V10")).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.update(999L, "V10",
                new VehicleUpdateRequest(null, "기아", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
