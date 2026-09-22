package com.odolog.app.vehicle.controller.rest;

import com.odolog.app.common.auth.constant.SessionConst;
import com.odolog.app.common.exception.type.ResourceNotFoundException;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.vehicle.dto.request.odometer.UpdateOdometerRequest;
import com.odolog.app.vehicle.dto.request.register.VehicleRegisterRequest;
import com.odolog.app.vehicle.dto.request.update.VehicleUpdateRequest;
import com.odolog.app.vehicle.service.application.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehicleController.class)
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VehicleService vehicleService;

    private MockHttpSession loginSessionOf(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, userId);
        return session;
    }

    @Test
    @DisplayName("차량 목록은 페이지 형태로 반환한다")
    void findMyVehiclesPaged() throws Exception {
        Vehicle vehicle = new Vehicle(null, "12가3456", "현대", "아반떼", 2023);
        ReflectionTestUtils.setField(vehicle, "id", 10L);

        when(vehicleService.findMyVehicles(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(vehicle), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/vehicles").session(loginSessionOf(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].plateNumber").value("12가3456"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("로그인하지 않고 차량을 등록하면 401")
    void registerWithoutLogin() throws Exception {
        VehicleRegisterRequest request = new VehicleRegisterRequest("12가3456", "현대", "아반떼", 2023);

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("번호판을 안 보내면 400")
    void registerInvalidRequest() throws Exception {
        String invalidJson = """
                {"plateNumber": "", "manufacturer": "현대", "modelName": "아반떼", "modelYear": 2023}
                """;

        mockMvc.perform(post("/api/vehicles")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("연식을 안 보내면 400")
    void registerWithoutModelYear() throws Exception {
        String noModelYear = """
                {"plateNumber": "12가3456", "manufacturer": "현대", "modelName": "아반떼"}
                """;

        mockMvc.perform(post("/api/vehicles")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(noModelYear))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("연식이 범위를 벗어나면 400")
    void registerWithModelYearOutOfRange() throws Exception {
        String outOfRange = """
                {"plateNumber": "12가3456", "manufacturer": "현대", "modelName": "아반떼", "modelYear": 999999}
                """;

        mockMvc.perform(post("/api/vehicles")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(outOfRange))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인한 사용자가 차량을 등록하면 201과 응답 바디를 반환한다")
    void registerSuccess() throws Exception {
        User owner = new User("owner@odolog.com", "encoded", "닉네임", "010-0000-0000");
        ReflectionTestUtils.setField(owner, "id", 1L);
        Vehicle vehicle = new Vehicle(owner, "12가3456", "현대", "아반떼", 2023);
        ReflectionTestUtils.setField(vehicle, "id", 10L);

        when(vehicleService.register(eq(1L), any(VehicleRegisterRequest.class))).thenReturn(vehicle);

        VehicleRegisterRequest request = new VehicleRegisterRequest("12가3456", "현대", "아반떼", 2023);

        mockMvc.perform(post("/api/vehicles")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plateNumber").value("12가3456"));
    }

    @Test
    @DisplayName("차량 단건 조회 성공")
    void findOneSuccess() throws Exception {
        User owner = new User("owner@odolog.com", "encoded", "닉네임", "010-0000-0000");
        ReflectionTestUtils.setField(owner, "id", 1L);
        Vehicle vehicle = new Vehicle(owner, "12가3456", "현대", "아반떼", 2023);
        ReflectionTestUtils.setField(vehicle, "id", 10L);

        when(vehicleService.findOwnedVehicle(1L, 10L)).thenReturn(vehicle);

        mockMvc.perform(get("/api/vehicles/10").session(loginSessionOf(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plateNumber").value("12가3456"));
    }

    @Test
    @DisplayName("존재하지 않는 차량을 조회하면 404")
    void findOneNotFound() throws Exception {
        when(vehicleService.findOwnedVehicle(1L, 999L))
                .thenThrow(new ResourceNotFoundException("존재하지 않는 차량입니다: 999"));

        mockMvc.perform(get("/api/vehicles/999").session(loginSessionOf(1L)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("본인 소유가 아닌 차량의 주행거리를 갱신하려 하면 404 — 존재 자체를 알리지 않는다")
    void updateOdometerForbidden() throws Exception {
        when(vehicleService.updateOdometer(eq(1L), eq(10L), any(UpdateOdometerRequest.class)))
                .thenThrow(new ResourceNotFoundException("존재하지 않는 차량입니다: 1"));

        mockMvc.perform(patch("/api/vehicles/10/odometer")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"odometer\":50000}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("주행거리 갱신에서 odometer 를 빠뜨리면 400")
    void updateOdometerMissingValue() throws Exception {
        mockMvc.perform(patch("/api/vehicles/10/odometer")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("차량 정보를 수정하면 200과 바뀐 값을 돌려준다")
    void updateVehicle() throws Exception {
        User owner = new User("owner@odolog.com", "encoded", "닉네임", "010-0000-0000");
        ReflectionTestUtils.setField(owner, "id", 1L);
        Vehicle vehicle = new Vehicle(owner, "12가3456", "기아", "아반떼", 2023);
        ReflectionTestUtils.setField(vehicle, "id", 10L);

        when(vehicleService.update(eq(1L), eq(10L), any(VehicleUpdateRequest.class))).thenReturn(vehicle);

        mockMvc.perform(patch("/api/vehicles/10")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VehicleUpdateRequest(null, "기아", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manufacturer").value("기아"));
    }

    @Test
    @DisplayName("로그인하지 않고 차량을 수정하면 401")
    void updateVehicleWithoutLogin() throws Exception {
        mockMvc.perform(patch("/api/vehicles/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"manufacturer\":\"기아\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("빈 본문은 아무 필드도 안 바꾸겠다는 뜻이라 통과한다")
    void updateVehicleWithEmptyBodyPasses() throws Exception {
        User owner = new User("owner@odolog.com", "encoded", "닉네임", "010-0000-0000");
        ReflectionTestUtils.setField(owner, "id", 1L);
        Vehicle vehicle = new Vehicle(owner, "12가3456", "현대", "아반떼", 2023);
        ReflectionTestUtils.setField(vehicle, "id", 10L);

        when(vehicleService.update(eq(1L), eq(10L), any(VehicleUpdateRequest.class))).thenReturn(vehicle);

        mockMvc.perform(patch("/api/vehicles/10")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("수정에서도 연식 범위를 벗어나면 400")
    void updateVehicleInvalidModelYear() throws Exception {
        mockMvc.perform(patch("/api/vehicles/10")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modelYear\":1899}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("공백만으로는 수정할 수 없다 — 400")
    void updateVehicleBlankFails() throws Exception {
        mockMvc.perform(patch("/api/vehicles/10")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"manufacturer\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("정렬할 수 없는 속성을 sort 로 보내면 500이 아니라 400")
    void findMyVehiclesInvalidSort() throws Exception {
        when(vehicleService.findMyVehicles(eq(1L), any(Pageable.class)))
                .thenThrow(new PropertyReferenceException("nonexistent",
                        TypeInformation.of(Vehicle.class), List.<PropertyPath>of()));

        mockMvc.perform(get("/api/vehicles")
                        .param("sort", "nonexistent")
                        .session(loginSessionOf(1L)))
                .andExpect(status().isBadRequest());
    }
}
