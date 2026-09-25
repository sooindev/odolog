package com.odolog.app.account.controller.rest;

import com.odolog.app.common.auth.session.LoginSessionRegistry;
import com.odolog.app.account.dto.request.withdraw.WithdrawRequest;
import com.odolog.app.account.dto.response.export.AccountExportResponse;
import com.odolog.app.account.service.application.AccountExportService;
import com.odolog.app.account.service.application.AccountRestoreService;
import com.odolog.app.account.service.application.AccountWithdrawalService;
import com.odolog.app.common.auth.constant.SessionConst;
import com.odolog.app.common.exception.type.AuthenticationFailedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountWithdrawalService accountWithdrawalService;

    @MockitoBean
    private AccountExportService accountExportService;

    @MockitoBean
    private AccountRestoreService accountRestoreService;

    @MockitoBean
    private LoginSessionRegistry sessionRegistry;

    @Test
    @DisplayName("탈퇴에 성공하면 204이고 세션이 끊긴다")
    void withdrawSuccess() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, 1L);

        mockMvc.perform(delete("/api/users/me")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WithdrawRequest("password1234"))))
                .andExpect(status().isNoContent());

        // 세션이 살아 있으면 없는 사용자 id 로 다음 요청이 500
        assertThat(session.isInvalid()).isTrue();
        // 다른 기기의 세션도
        verify(sessionRegistry).invalidateAll(1L);
    }

    @Test
    @DisplayName("로그인하지 않고 탈퇴하려 하면 401")
    void withdrawWithoutLogin() throws Exception {
        mockMvc.perform(delete("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WithdrawRequest("password1234"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401이고 세션은 그대로다")
    void withdrawWrongPassword() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, 1L);

        doThrow(new AuthenticationFailedException("현재 비밀번호가 올바르지 않습니다."))
                .when(accountWithdrawalService).withdraw(any(), any());

        mockMvc.perform(delete("/api/users/me")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WithdrawRequest("wrongpassword"))))
                .andExpect(status().isUnauthorized());

        assertThat(session.isInvalid()).isFalse();
    }

    @Test
    @DisplayName("비밀번호를 보내지 않으면 400")
    void withdrawWithoutPassword() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, 1L);

        mockMvc.perform(delete("/api/users/me")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("내보내기는 계정의 기록 전부를 내려준다")
    void exportSuccess() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, 1L);

        AccountExportResponse response = new AccountExportResponse(
                java.time.LocalDateTime.of(2026, 9, 21, 12, 0),
                new AccountExportResponse.UserData("me@odolog.com", "닉네임", null, null),
                java.util.List.of());
        when(accountExportService.export(eq(1L), any())).thenReturn(response);

        mockMvc.perform(get("/api/users/me/export").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("me@odolog.com"))
                .andExpect(jsonPath("$.vehicles").isArray());
    }

    @Test
    @DisplayName("로그인하지 않으면 내보내기는 401")
    void exportRequiresLogin() throws Exception {
        mockMvc.perform(get("/api/users/me/export"))
                .andExpect(status().isUnauthorized());
    }
}
