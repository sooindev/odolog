package com.odolog.app.summary;

import com.odolog.app.common.auth.SessionConst;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.service.application.UserService;
import com.odolog.app.user.service.UserToday;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GarageSummaryController.class)
class GarageSummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GarageSummaryService garageSummaryService;

    @MockitoBean
    private UserToday userToday;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("요약 조회 성공 시 200과 합계·구성을 함께 반환한다")
    void summarySuccess() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, 1L);

        User user = new User("a@b.com", "encoded", "nick");
        when(userService.findById(1L)).thenReturn(user);
        when(userToday.of(user)).thenReturn(LocalDate.of(2026, 9, 29));
        when(garageSummaryService.summarize(eq(1L), eq(LocalDate.of(2026, 9, 29)), eq("KRW"))).thenReturn(
                new GarageSummaryResponse(2, 90000, 5, "KRW", 0, 148000, 80000, 68000,
                        List.of(new GarageSummaryResponse.MonthlyCost("2026-09", 148000, 80000, 68000, 2)),
                        List.of(), List.of(), List.of()));

        mockMvc.perform(get("/api/summary").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(148000))
                .andExpect(jsonPath("$.maintenanceCost").value(80000))
                .andExpect(jsonPath("$.fuelCost").value(68000))
                .andExpect(jsonPath("$.monthly[0].month").value("2026-09"));
    }

    @Test
    @DisplayName("로그인하지 않고 요약을 조회하면 401")
    void summaryWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/summary")).andExpect(status().isUnauthorized());
    }
}
