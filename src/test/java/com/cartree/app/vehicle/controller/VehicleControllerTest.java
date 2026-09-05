package com.cartree.app.vehicle.controller;

import com.cartree.app.common.auth.SessionConst;
import com.cartree.app.common.exception.ForbiddenAccessException;
import com.cartree.app.user.domain.User;
import com.cartree.app.vehicle.domain.Vehicle;
import com.cartree.app.vehicle.dto.UpdateOdometerRequest;
import com.cartree.app.vehicle.dto.VehicleRegisterRequest;
import com.cartree.app.vehicle.service.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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
    @DisplayName("로그인한 사용자가 차량을 등록하면 201과 응답 바디를 반환한다")
    void registerSuccess() throws Exception {
        User owner = new User("owner@cartree.com", "encoded", "닉네임", "010-0000-0000");
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
    @DisplayName("본인 소유가 아닌 차량의 주행거리를 갱신하려 하면 403")
    void updateOdometerForbidden() throws Exception {
        when(vehicleService.updateOdometer(eq(1L), eq(10L), any(UpdateOdometerRequest.class)))
                .thenThrow(new ForbiddenAccessException("본인 소유의 차량만 접근할 수 있습니다."));

        mockMvc.perform(patch("/api/vehicles/10/odometer")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"odometer\":50000}"))
                .andExpect(status().isForbidden());
    }
}
