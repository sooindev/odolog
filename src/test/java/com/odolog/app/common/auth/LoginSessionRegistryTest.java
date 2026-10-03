package com.odolog.app.common.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 세션은 DB(Spring Session JDBC). 실제 표에 세션을 만들고 끊기는지 본다
 * 메모리 목록이던 때와 같은 약속: 지금 세션만 남기기, 남의 세션 불가침, 계정 전환
 */
@SpringBootTest
class LoginSessionRegistryTest {

    @Autowired
    private FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    @Autowired
    private LoginSessionRegistry registry;

    private final List<String> created = new ArrayList<>();

    @AfterEach
    void tearDown() {
        created.forEach(sessionRepository::deleteById);
    }

    /** 로그인한 세션 하나를 DB 에 만들고, 같은 id 를 가진 HttpSession 으로 돌려줌 */
    private MockHttpSession loggedIn(Long userId) {
        String id = saveSession(sessionRepository);
        MockHttpSession http = new MockHttpSession(null, id);
        registry.register(userId, http);
        indexAs(sessionRepository, id, http);
        created.add(id);
        return http;
    }

    /** 타입 매개변수로 묶어 공개 인터페이스(Session)만 쓴다. 구현 클래스는 패키지 밖에 안 열림 */
    private static <S extends Session> String saveSession(FindByIndexNameSessionRepository<S> repository) {
        S session = repository.createSession();
        repository.save(session);
        return session.getId();
    }

    /** register 가 HttpSession 에 단 색인을 저장된 세션에도 옮김(실제로는 SessionRepositoryFilter 가 함) */
    private static <S extends Session> void indexAs(FindByIndexNameSessionRepository<S> repository, String id,
                                                   MockHttpSession http) {
        S session = repository.findById(id);
        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
                http.getAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME));
        repository.save(session);
    }

    private boolean alive(MockHttpSession session) {
        return sessionRepository.findById(session.getId()) != null;
    }

    @Test
    @DisplayName("지금 세션만 남기고 같은 사용자의 다른 세션을 끊는다")
    void invalidatesOthersButKeepsCurrent() {
        MockHttpSession current = loggedIn(1L);
        MockHttpSession otherDevice = loggedIn(1L);

        registry.invalidateOthers(1L, current);

        assertThat(alive(current)).isTrue();
        assertThat(alive(otherDevice)).isFalse();
    }

    @Test
    @DisplayName("다른 사용자의 세션은 건드리지 않는다")
    void leavesOtherUsersAlone() {
        MockHttpSession mine = loggedIn(1L);
        MockHttpSession someoneElse = loggedIn(2L);

        registry.invalidateAll(1L);

        assertThat(alive(mine)).isFalse();
        assertThat(alive(someoneElse)).isTrue();
    }

    @Test
    @DisplayName("같은 브라우저에서 다른 계정으로 다시 로그인하면 옛 계정의 끊기에 휩쓸리지 않는다")
    void accountSwitchMovesSession() {
        MockHttpSession browser = loggedIn(1L);
        // 로그아웃 없이 2번으로 로그인. 색인이 덮어써짐
        registry.register(2L, browser);
        indexAs(sessionRepository, browser.getId(), browser);

        registry.invalidateAll(1L);

        assertThat(alive(browser)).isTrue();
    }

    @Test
    @DisplayName("세션이 DB 에 있어 재시작과 무관하다 — 저장된 세션을 색인으로 다시 찾는다")
    void findsSessionsFromDatabase() {
        MockHttpSession session = loggedIn(7L);

        assertThat(sessionRepository.findByPrincipalName("7")).containsKey(session.getId());
    }
}
