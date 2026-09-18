package com.odolog.app.fuel.controller.rest;

import com.odolog.app.common.auth.constant.SessionConst;
import com.odolog.app.fuel.dto.response.record.FuelRecordResponse;
import com.odolog.app.fuel.dto.response.summary.FuelSummaryResponse;
import com.odolog.app.fuel.service.application.FuelRecordService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FuelRecordController.class)
class FuelRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FuelRecordService fuelRecordService;

    private MockHttpSession loginSessionOf(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, userId);
        return session;
    }

    private FuelRecordResponse response() {
        return new FuelRecordResponse(1L, LocalDate.of(2026, 9, 10), 10500,
                new BigDecimal("25.00"), 50000, null, false,
                2000, 500, new BigDecimal("20.00"), false);
    }

    @Test
    @DisplayName("주유 기록 등록 성공 시 201과 연비를 함께 반환한다")
    void registerSuccess() throws Exception {
        when(fuelRecordService.register(eq(1L), eq(10L), any())).thenReturn(response());

        mockMvc.perform(post("/api/vehicles/10/fuel-records")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fueledAt":"2026-09-10","odometer":10500,"liters":25.00,"totalCost":50000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.efficiency").value(20.00))
                .andExpect(jsonPath("$.pricePerLiter").value(2000));
    }

    @Test
    @DisplayName("로그인하지 않고 주유 기록을 등록하면 401")
    void registerWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/vehicles/10/fuel-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fueledAt":"2026-09-10","odometer":10500,"liters":25.00,"totalCost":50000}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("주유량이 0이면 400 — 연비 계산이 0으로 나누기가 된다")
    void registerZeroLiters() throws Exception {
        mockMvc.perform(post("/api/vehicles/10/fuel-records")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fueledAt":"2026-09-10","odometer":10500,"liters":0,"totalCost":50000}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("주유량을 빠뜨리면 400 — 래퍼 타입이라 0으로 채워지지 않는다")
    void registerMissingLiters() throws Exception {
        mockMvc.perform(post("/api/vehicles/10/fuel-records")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fueledAt":"2026-09-10","odometer":10500,"totalCost":50000}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("미래 날짜로 주유를 등록하면 400")
    void registerFutureDateRejected() throws Exception {
        // 드럼 휠은 미래 년도를 아예 안 만들지만 네이티브 date 와 API 는 그대로 받고 있었다
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(post("/api/vehicles/10/fuel-records")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fueledAt\":\"" + tomorrow
                                + "\",\"odometer\":10500,\"liters\":25,\"totalCost\":50000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("주유량 소수점이 셋째 자리까지 가면 400")
    void registerTooManyDecimals() throws Exception {
        mockMvc.perform(post("/api/vehicles/10/fuel-records")
                        .session(loginSessionOf(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fueledAt":"2026-09-10","odometer":10500,"liters":25.123,"totalCost":50000}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("목록은 페이지 형태로 반환한다")
    void findByVehiclePaged() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        when(fuelRecordService.findByVehicle(eq(1L), eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response()), pageable, 1));

        mockMvc.perform(get("/api/vehicles/10/fuel-records").session(loginSessionOf(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].efficiency").value(20.00))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("/summary 는 {recordId} 보다 먼저 매칭된다")
    void summaryRoutesBeforePathVariable() throws Exception {
        when(fuelRecordService.summary(1L, 10L)).thenReturn(new FuelSummaryResponse(
                3, 160000, new BigDecimal("80.00"), 1000, new BigDecimal("20.00"), 3L, null, 0));

        mockMvc.perform(get("/api/vehicles/10/fuel-records/summary").session(loginSessionOf(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageEfficiency").value(20.00))
                .andExpect(jsonPath("$.recordCount").value(3));
    }

    @Test
    @DisplayName("주유 기록 삭제 성공 시 204")
    void deleteSuccess() throws Exception {
        mockMvc.perform(delete("/api/vehicles/10/fuel-records/1").session(loginSessionOf(1L)))
                .andExpect(status().isNoContent());
    }
}
