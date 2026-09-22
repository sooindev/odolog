package com.odolog.app.maintenance.controller.rest;

import com.odolog.app.common.auth.constant.SessionConst;
import com.odolog.app.common.exception.type.ResourceNotFoundException;
import com.odolog.app.maintenance.domain.type.ServiceType;
import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.dto.request.register.MaintenanceRecordRegisterRequest;
import com.odolog.app.maintenance.dto.response.schedule.NextServiceResponse;
import com.odolog.app.maintenance.service.application.MaintenanceRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
    @DisplayName("정비 이력 목록은 페이지 형태로 반환한다")
    void findByVehiclePaged() throws Exception {
        MaintenanceRecord record = new MaintenanceRecord(null, ServiceType.ENGINE_OIL, "정기 교체",
                50000, 40000, LocalDate.of(2026, 1, 1));

        when(maintenanceRecordService.findByVehicle(eq(1L), eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/vehicles/10/maintenance-records").session(loginSessionOf(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].type").value("ENGINE_OIL"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }


    @Test
    @DisplayName("미래 날짜로 정비 이력을 등록하면 400")
    void registerFutureDateRejected() throws Exception {
        // 날짜를 잘못 치면 그 기록이 목록 맨 위에 고정되고 다음 정비 시점까지 그 값으로 계산된다
        MaintenanceRecordRegisterRequest request = new MaintenanceRecordRegisterRequest(
                ServiceType.ENGINE_OIL, "정기 교체", 50000, 40000, LocalDate.now().plusDays(1));

        mockMvc.perform(post("/api/vehicles/10/maintenance-records")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("오늘 날짜는 통과한다")
    void registerTodayAccepted() throws Exception {
        MaintenanceRecordRegisterRequest request = new MaintenanceRecordRegisterRequest(
                ServiceType.ENGINE_OIL, "정기 교체", 50000, 40000, LocalDate.now());

        when(maintenanceRecordService.register(anyLong(), anyLong(), any(MaintenanceRecordRegisterRequest.class)))
                .thenReturn(new MaintenanceRecord(null, ServiceType.ENGINE_OIL, "정기 교체",
                        50000, 40000, LocalDate.now()));

        mockMvc.perform(post("/api/vehicles/10/maintenance-records")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("경로 변수 타입이 안 맞으면 500이 아니라 400")
    void invalidPathVariableType() throws Exception {
        // MethodArgumentTypeMismatchException 핸들러 검증
        // 겨냥하던 /next-service 를 걷어내며 경로 변수로 옮김 — 핸들러 자체는 범용
        mockMvc.perform(get("/api/vehicles/abc/maintenance-records")
                        .session(loginSessionOf(1L)))
                .andExpect(status().isBadRequest());
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
    @DisplayName("정비 이력 등록에서 cost/serviceOdometer 를 빠뜨리면 400")
    void registerMissingRequiredNumbers() throws Exception {
        mockMvc.perform(post("/api/vehicles/10/maintenance-records")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ENGINE_OIL\",\"serviceDate\":\"2026-09-01\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("없는 종류를 보내면 400 — 우리 에러 모양으로, 어느 필드인지까지")
    void unknownServiceTypeIsBadRequest() throws Exception {
        /*
         * 전에는 핸들러가 없어 스프링 기본 응답(timestamp/status/error/path)이 나갔다.
         * message 가 없어 화면이 "요청에 실패했습니다 (HTTP 400)" 로 떨어졌고, 무엇이
         * 틀렸는지 말해 주지 못했다.
         */
        mockMvc.perform(post("/api/vehicles/10/maintenance-records")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"NOT_A_TYPE","cost":1000,"serviceOdometer":100,"serviceDate":"2026-09-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("type: 값의 형식이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("날짜 형식이 깨져도 같은 모양으로 400")
    void brokenDateIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/vehicles/10/maintenance-records")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"ENGINE_OIL","cost":1000,"serviceOdometer":100,"serviceDate":"2026-13-45"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("serviceDate: 값의 형식이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("JSON 이 통째로 깨지면 필드를 특정할 수 없어 일반 문구로 400")
    void brokenJsonIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/vehicles/10/maintenance-records")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 본문을 읽을 수 없습니다. 형식을 확인해 주세요."));
    }

    @Test
    @DisplayName("정비 이력 삭제 성공 시 204")
    void deleteSuccess() throws Exception {
        mockMvc.perform(delete("/api/vehicles/10/maintenance-records/100")
                        .session(loginSessionOf(1L)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("/next-services 는 이력 있는 종류를 한 번에 돌려준다")
    void calculateAllNextServices() throws Exception {
        when(maintenanceRecordService.calculateAllNextServices(1L, 10L)).thenReturn(List.of(
                new NextServiceResponse(ServiceType.ENGINE_OIL, 20000, 25000,
                        LocalDate.of(2026, 9, 1), LocalDate.of(2027, 3, 1)),
                new NextServiceResponse(ServiceType.TRANSMISSION_FLUID, 15000, 75000,
                        LocalDate.of(2026, 6, 1), LocalDate.of(2030, 6, 1))));

        mockMvc.perform(get("/api/vehicles/10/maintenance-records/next-services")
                        .session(loginSessionOf(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("ENGINE_OIL"))
                .andExpect(jsonPath("$[1].type").value("TRANSMISSION_FLUID"));
    }

}
