package com.odolog.app.garage;

import com.odolog.app.common.auth.SessionConst;
import com.odolog.app.vehicle.domain.Vehicle;
import com.odolog.app.vehicle.dto.response.VehicleResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GarageVehicleController.class)
class GarageVehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleListService vehicleListService;

    @MockitoBean
    private VehicleRemovalService vehicleRemovalService;

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

        when(vehicleListService.findMyVehicles(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(VehicleResponse.of(vehicle, 2, 1)),
                        PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/vehicles").session(loginSessionOf(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].plateNumber").value("12가3456"))
                // 목록에서만 채움
                .andExpect(jsonPath("$.items[0].overdueServiceCount").value(2))
                .andExpect(jsonPath("$.items[0].dueSoonServiceCount").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("정렬할 수 없는 속성을 sort 로 보내면 500이 아니라 400")
    void findMyVehiclesInvalidSort() throws Exception {
        when(vehicleListService.findMyVehicles(eq(1L), any(Pageable.class)))
                .thenThrow(new PropertyReferenceException("nonexistent",
                        TypeInformation.of(Vehicle.class), List.<PropertyPath>of()));

        mockMvc.perform(get("/api/vehicles")
                        .param("sort", "nonexistent")
                        .session(loginSessionOf(1L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("차량 삭제는 204")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/vehicles/V10").session(loginSessionOf(1L)))
                .andExpect(status().isNoContent());

        verify(vehicleRemovalService).delete(1L, "V10");
    }

    @Test
    @DisplayName("로그인하지 않고 차량을 삭제하면 401")
    void deleteWithoutLogin() throws Exception {
        mockMvc.perform(delete("/api/vehicles/V10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("페이지 크기는 100 이 상한이다 — ?size=2000 으로 통째로 받지 못하게")
    void pageSizeIsCapped() throws Exception {
        when(vehicleListService.findMyVehicles(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/vehicles").param("size", "2000").session(loginSessionOf(1L)))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(vehicleListService).findMyVehicles(eq(1L), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }
}
