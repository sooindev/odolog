package com.odolog.app.user.controller;

import com.odolog.app.common.exception.ConflictException;
import com.odolog.app.common.auth.SessionConst;
import com.odolog.app.common.exception.AuthenticationFailedException;
import com.odolog.app.user.domain.User;
import com.odolog.app.user.dto.request.LoginRequest;
import com.odolog.app.user.dto.request.SignUpRequest;
import com.odolog.app.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("회원가입 성공 시 201과 사용자 정보를 반환한다")
    void signUpSuccess() throws Exception {
        User user = new User("test@odolog.com", "encoded", "닉네임", "010-0000-0000");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userService.signUp(any())).thenReturn(user);

        SignUpRequest request = new SignUpRequest("test@odolog.com", "password1234", "닉네임", "010-0000-0000");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@odolog.com"))
                .andExpect(jsonPath("$.nickname").value("닉네임"));
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 409")
    void signUpDuplicateEmail() throws Exception {
        when(userService.signUp(any())).thenThrow(new ConflictException("이미 가입된 이메일입니다."));

        SignUpRequest request = new SignUpRequest("test@odolog.com", "password1234", "닉네임", "010-0000-0000");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("로그인 성공 시 세션에 사용자 id가 저장되고 세션 id가 바뀐다")
    void loginSuccessStoresSessionAndRotatesId() throws Exception {
        User user = new User("test@odolog.com", "encoded", "닉네임", "010-0000-0000");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userService.login(any())).thenReturn(user);

        LoginRequest request = new LoginRequest("test@odolog.com", "password1234");

        var result = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute(SessionConst.LOGIN_USER_ID)).isEqualTo(1L);
    }

    @Test
    @DisplayName("로그인 실패(이메일/비밀번호 불일치)면 401")
    void loginFailure() throws Exception {
        when(userService.login(any()))
                .thenThrow(new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다."));

        LoginRequest request = new LoginRequest("test@odolog.com", "wrongpassword");

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그인하지 않고 /me 를 조회하면 401")
    void meWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그인한 상태로 /me 를 조회하면 200과 내 정보를 반환한다")
    void meWithLogin() throws Exception {
        User user = new User("test@odolog.com", "encoded", "닉네임", "010-0000-0000");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userService.findById(1L)).thenReturn(user);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, 1L);

        mockMvc.perform(get("/api/users/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("닉네임"));
    }
}
