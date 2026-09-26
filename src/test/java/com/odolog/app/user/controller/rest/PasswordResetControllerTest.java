package com.odolog.app.user.controller.rest;

import com.odolog.app.common.exception.type.AuthenticationFailedException;
import com.odolog.app.common.exception.type.TooManyRequestsException;
import com.odolog.app.user.dto.request.password.PasswordResetConfirmRequest;
import com.odolog.app.user.dto.request.password.PasswordResetRequest;
import com.odolog.app.user.service.application.PasswordResetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordResetController.class)
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PasswordResetService passwordResetService;

    private org.springframework.test.web.servlet.ResultActions request(Object body, boolean isPatch) throws Exception {
        var builder = isPatch ? patch("/api/users/password-reset") : post("/api/users/password-reset");

        return mockMvc.perform(builder
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    @Test
    @DisplayName("재설정 요청은 로그인 없이 204")
    void requestReturnsNoContent() throws Exception {
        request(new PasswordResetRequest("me@odolog.com"), false)
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("가입되지 않은 주소여도 똑같이 204")
    void requestHidesWhetherAccountExists() throws Exception {
        // 가입 여부와 무관한 같은 응답
        request(new PasswordResetRequest("nobody@odolog.com"), false)
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("이메일 형식이 아니면 400")
    void requestRejectsMalformedEmail() throws Exception {
        request(new PasswordResetRequest("이메일아님"), false)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("같은 주소로 너무 자주 요청하면 429")
    void requestIsRateLimited() throws Exception {
        // 메일 폭주 방지. 로그인 리미터 공용
        doThrow(new TooManyRequestsException("로그인 시도가 너무 많습니다. 10분 후 다시 시도해 주세요."))
                .when(passwordResetService).request(anyString());

        request(new PasswordResetRequest("me@odolog.com"), false)
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("토큰으로 비밀번호를 바꾸면 204")
    void confirmReturnsNoContent() throws Exception {
        request(new PasswordResetConfirmRequest("token", "new-password-1234"), true)
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("만료·사용된 토큰이면 401")
    void confirmRejectsDeadToken() throws Exception {
        doThrow(new AuthenticationFailedException("링크가 만료되었거나 이미 사용되었습니다. 다시 요청해 주세요."))
                .when(passwordResetService).confirm(any());

        request(new PasswordResetConfirmRequest("token", "new-password-1234"), true)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("새 비밀번호가 8자 미만이면 400")
    void confirmRejectsShortPassword() throws Exception {
        request(new PasswordResetConfirmRequest("token", "short"), true)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("새 비밀번호도 72바이트를 넘으면 400")
    void confirmRejectsPasswordOverByteLimit() throws Exception {
        // 새 비밀번호 제한은 가입·변경과 동일
        request(new PasswordResetConfirmRequest("token", "가".repeat(25)), true)
                .andExpect(status().isBadRequest());
    }
}
