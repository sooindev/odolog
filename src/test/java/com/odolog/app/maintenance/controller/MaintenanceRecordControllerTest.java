package com.odolog.app.maintenance.controller;

import com.odolog.app.common.auth.SessionConst;
import com.odolog.app.common.exception.ResourceNotFoundException;
import com.odolog.app.maintenance.domain.ServiceType;
import com.odolog.app.maintenance.domain.MaintenanceRecord;
import com.odolog.app.maintenance.dto.MaintenanceRecordRegisterRequest;
import com.odolog.app.maintenance.dto.NextServiceResponse;
import com.odolog.app.maintenance.service.MaintenanceRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaintenanceRecordController.class)
class MaintenanceRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MaintenanceRecordService maintenanceRecordService;

    private MockHttpSession loginSessionOf(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, userId);
        return session;
    }

    @Test
    @DisplayName("다음 정비 시점을 조회하면 200과 계산 결과를 반환한다")
    void nextServiceSuccess() throws Exception {
        when(maintenanceRecordService.calculateNextService(1L, 10L, ServiceType.ENGINE_OIL))
                .thenReturn(new NextServiceResponse(ServiceType.ENGINE_OIL, 40000, 45000));

        mockMvc.perform(get("/api/vehicles/10/maintenance-records/next-service")
                        .param("type", "ENGINE_OIL")
                        .session(loginSessionOf(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextServiceOdometer").value(45000));
    }

    @Test
    @DisplayName("존재하지 않는 ServiceType 값을 보내면 400")
    void nextServiceInvalidType() throws Exception {
        mockMvc.perform(get("/api/vehicles/10/maintenance-records/next-service")
                        .param("type", "존재하지않는값")
                        .session(loginSessionOf(1L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("정비 이력 단건 조회 성공")
    void findOneSuccess() throws Exception {
        MaintenanceRecord record = new MaintenanceRecord(null, ServiceType.ENGINE_OIL, "정기 교체",
                50000, 40000, LocalDate.of(2026, 1, 1));

        when(maintenanceRecordService.findOne(1L, 10L, 100L)).thenReturn(record);

        mockMvc.perform(get("/api/vehicles/10/maintenance-records/100").session(loginSessionOf(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ENGINE_OIL"));
    }

    @Test
    @DisplayName("존재하지 않는 정비 이력을 조회하면 404")
    void findOneNotFound() throws Exception {
        when(maintenanceRecordService.findOne(1L, 10L, 999L))
                .thenThrow(new ResourceNotFoundException("존재하지 않는 정비 이력입니다: 999"));

        mockMvc.perform(get("/api/vehicles/10/maintenance-records/999").session(loginSessionOf(1L)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("존재하지 않는 차량에 정비 이력을 등록하려 하면 404")
    void registerVehicleNotFound() throws Exception {
        when(maintenanceRecordService.register(anyLong(), anyLong(), any(MaintenanceRecordRegisterRequest.class)))
                .thenThrow(new ResourceNotFoundException("존재하지 않는 차량입니다: 999"));

        MaintenanceRecordRegisterRequest request = new MaintenanceRecordRegisterRequest(
                ServiceType.ENGINE_OIL, "정기 교체", 50000, 40000, LocalDate.of(2026, 1, 1));

        mockMvc.perform(post("/api/vehicles/999/maintenance-records")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("정비 이력 삭제 성공 시 204")
    void deleteSuccess() throws Exception {
        mockMvc.perform(delete("/api/vehicles/10/maintenance-records/100")
                        .session(loginSessionOf(1L)))
                .andExpect(status().isNoContent());
    }
}
