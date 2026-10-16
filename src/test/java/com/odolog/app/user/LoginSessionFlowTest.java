package com.odolog.app.user;

import com.odolog.app.user.domain.User;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 진짜 세션 저장소(Spring Session JDBC)와 필터를 거친 로그인·로그아웃·다른 기기 끊기
 * 단위 테스트는 세션 색인을 손으로 옮겨서, 필터가 색인을 실제로 다는지는 여기서만 드러남
 */
@SpringBootTest
@AutoConfigureMockMvc
class LoginSessionFlowTest {

    private static final String PASSWORD = "password1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JdbcIndexedSessionRepository sessionRepository;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from SPRING_SESSION_ATTRIBUTES");
        jdbcTemplate.update("delete from SPRING_SESSION");
        userRepository.deleteAll();
    }

    private User user(String email) {
        return userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), "세션"));
    }

    /** 로그인하고 받은 세션 쿠키. 기기 하나 */
    private Cookie login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String header = result.getResponse().getHeaders("Set-Cookie").stream()
                .filter(value -> value.startsWith("JSESSIONID="))
                .reduce((first, second) -> second)
                .orElseThrow();
        return new Cookie("JSESSIONID", header.substring("JSESSIONID=".length(), header.indexOf(';')));
    }

    private int me(Cookie session) throws Exception {
        return mockMvc.perform(get("/api/users/me").cookie(session)).andReturn().getResponse().getStatus();
    }

    @Test
    @DisplayName("비밀번호를 바꾸면 그 기기는 남고 다른 기기는 끊긴다 — 실제 세션 저장소에서")
    void changePasswordKeepsCurrentAndEndsOthers() throws Exception {
        user("flow@odolog.com");
        Cookie here = login("flow@odolog.com");
        Cookie elsewhere = login("flow@odolog.com");
        assertThat(me(here)).isEqualTo(200);
        assertThat(me(elsewhere)).isEqualTo(200);

        mockMvc.perform(patch("/api/users/me/password")
                        .cookie(here)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"newpassword1234\"}"))
                .andExpect(status().isNoContent());

        assertThat(me(here)).isEqualTo(200);
        assertThat(me(elsewhere)).isEqualTo(401);
    }

    @Test
    @DisplayName("로그아웃하면 그 세션으로는 다시 들어올 수 없다")
    void logoutEndsSession() throws Exception {
        user("logout@odolog.com");
        Cookie session = login("logout@odolog.com");
        assertThat(me(session)).isEqualTo(200);

        mockMvc.perform(post("/api/users/logout").cookie(session)).andExpect(status().isNoContent());

        assertThat(me(session)).isEqualTo(401);
    }

    @Test
    @DisplayName("로그인 전 세션 id 는 로그인 뒤에 쓸 수 없다 — 고정 공격 방지")
    void loginChangesSessionId() throws Exception {
        user("fixation@odolog.com");
        // 로그인 전에 만들어진 세션(공격자가 심어 둔 것일 수 있음)
        // 내부 클래스(JdbcSession)라 Session 으로 받아 씀
        Session planted = sessionRepository.createSession();
        saveSession(planted);
        String plantedCookie = Base64.getEncoder().encodeToString(planted.getId().getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(post("/api/users/login")
                        .cookie(new Cookie("JSESSIONID", plantedCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"fixation@odolog.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // 심어 둔 id 로는 로그인 상태가 아님
        assertThat(me(new Cookie("JSESSIONID", plantedCookie))).isEqualTo(401);
        assertThat(sessionRepository.findById(planted.getId())).isNull();
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(header -> assertThat(header).startsWith("JSESSIONID=").doesNotContain(plantedCookie));
    }

    /** createSession 이 돌려주는 타입이 공개되지 않아 원시 저장소로 저장 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void saveSession(Session session) {
        ((SessionRepository) sessionRepository).save(session);
    }
}
