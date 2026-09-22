package com.odolog.app.user.controller.rest;

import com.odolog.app.common.exception.type.ConflictException;
import com.odolog.app.common.auth.constant.SessionConst;
import com.odolog.app.common.auth.ratelimit.LoginAttemptLimiter;
import com.odolog.app.common.exception.type.AuthenticationFailedException;
import com.odolog.app.common.exception.type.TooManyRequestsException;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.dto.request.login.LoginRequest;
import com.odolog.app.user.dto.request.password.ChangePasswordRequest;
import com.odolog.app.user.dto.request.signup.SignUpRequest;
import com.odolog.app.user.service.application.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    // 컨트롤러가 직접 주입받는다 — @WebMvcTest 는 @Component 를 안 올리므로 여기서 대신 준다
    @MockitoBean
    private LoginAttemptLimiter attemptLimiter;

    @Test
    @DisplayName("가입 시도가 한도를 넘으면 429를 반환한다")
    void signUpTooManyAttempts() throws Exception {
        // 가입 409 가 가입 여부를 알려주므로, 한 곳에서 주소를 쓸어 보는 것을 IP 로 막는다
        doThrow(new TooManyRequestsException("회원가입 시도가 너무 많습니다. 10분 후 다시 시도해 주세요."))
                .when(attemptLimiter).checkNotLocked(any(), any());

        SignUpRequest request = new SignUpRequest("test@odolog.com", "password1234", "닉네임", null);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(
                        "회원가입 시도가 너무 많습니다. 10분 후 다시 시도해 주세요."));
    }

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
    @DisplayName("전화번호가 20자를 넘으면 400 (DB까지 가서 500이 나면 안 된다)")
    void signUpWithTooLongPhone() throws Exception {
        SignUpRequest request = new SignUpRequest(
                "test@odolog.com", "password1234", "닉네임", "0".repeat(21));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
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

    @Test
    @DisplayName("닉네임을 빈 문자열로 수정하려 하면 400")
    void updateProfileBlankNickname() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, 1L);

        mockMvc.perform(patch("/api/users/me")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("중복 검사를 통과한 뒤 DB 유니크 제약에 막히면 500이 아니라 409")
    void signUpLosesRaceToUniqueConstraint() throws Exception {
        // existsByEmail 과 save 사이에 다른 요청이 끼어든 상황
        ConstraintViolationException unique = new ConstraintViolationException(
                "Duplicate entry", new SQLException("duplicate"),
                ConstraintViolationException.ConstraintKind.UNIQUE, "uk_users_email");

        when(userService.signUp(any()))
                .thenThrow(new DataIntegrityViolationException("could not execute statement", unique));

        SignUpRequest request = new SignUpRequest("test@odolog.com", "password123", "닉네임", null);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("비밀번호 변경에 성공하면 204이고 본문이 없다")
    void changePasswordSuccess() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, 1L);

        mockMvc.perform(patch("/api/users/me/password")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("oldpassword", "newpassword1234"))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("로그인하지 않고 비밀번호를 바꾸려 하면 401")
    void changePasswordWithoutLogin() throws Exception {
        mockMvc.perform(patch("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("oldpassword", "newpassword1234"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 401")
    void changePasswordWrongCurrent() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, 1L);

        doThrow(new AuthenticationFailedException("현재 비밀번호가 올바르지 않습니다."))
                .when(userService).changePassword(any(), any());

        mockMvc.perform(patch("/api/users/me/password")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("wrongpassword", "newpassword1234"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("새 비밀번호가 8자 미만이면 400 — 가입 때와 같은 제한")
    void changePasswordTooShort() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, 1L);

        mockMvc.perform(patch("/api/users/me/password")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("oldpassword", "short"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("가입 비밀번호가 72바이트를 넘으면 400을 반환한다")
    void signUpRejectsPasswordOverByteLimit() throws Exception {
        // BCrypt 가 72바이트에서 IllegalArgumentException 을 던진다
        // @Size 는 글자 수라 한글 25자(75바이트)를 막지 못해 500 으로 새어 나갔다
        String password = "가".repeat(25);
        SignUpRequest request = new SignUpRequest("test@odolog.com", password, "닉네임", null);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("72바이트")));
    }

    @Test
    @DisplayName("가입 비밀번호가 정확히 72바이트면 통과한다")
    void signUpAcceptsPasswordAtByteLimit() throws Exception {
        // 경계를 한 칸 안쪽으로 잘못 잡으면 24자 한글 비밀번호가 막힌다
        User user = new User("test@odolog.com", "encoded", "닉네임", null);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userService.signUp(any())).thenReturn(user);

        String password = "가".repeat(24);
        SignUpRequest request = new SignUpRequest("test@odolog.com", password, "닉네임", null);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("비밀번호 변경도 72바이트를 넘으면 400을 반환한다")
    void changePasswordRejectsPasswordOverByteLimit() throws Exception {
        // 가입만 막으면 가입으로 못 만드는 비밀번호가 변경으로 통과한다
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, 1L);

        ChangePasswordRequest request = new ChangePasswordRequest("current1234", "a".repeat(73));

        mockMvc.perform(patch("/api/users/me/password")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
