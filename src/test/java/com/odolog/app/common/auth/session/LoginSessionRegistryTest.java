package com.odolog.app.common.auth.session;

import jakarta.servlet.http.HttpSessionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class LoginSessionRegistryTest {

    private final LoginSessionRegistry registry = new LoginSessionRegistry();

    @Test
    @DisplayName("지금 세션만 남기고 같은 사용자의 다른 세션을 끊는다")
    void invalidatesOthersButKeepsCurrent() {
        MockHttpSession current = new MockHttpSession();
        MockHttpSession otherDevice = new MockHttpSession();
        registry.register(1L, current);
        registry.register(1L, otherDevice);

        registry.invalidateOthers(1L, current);

        assertThat(current.isInvalid()).isFalse();
        assertThat(otherDevice.isInvalid()).isTrue();
    }

    @Test
    @DisplayName("다른 사용자의 세션은 건드리지 않는다")
    void leavesOtherUsersAlone() {
        MockHttpSession mine = new MockHttpSession();
        MockHttpSession someoneElse = new MockHttpSession();
        registry.register(1L, mine);
        registry.register(2L, someoneElse);

        registry.invalidateAll(1L);

        assertThat(mine.isInvalid()).isTrue();
        assertThat(someoneElse.isInvalid()).isFalse();
    }

    @Test
    @DisplayName("같은 브라우저에서 다른 계정으로 다시 로그인하면 옛 계정 목록에서 빠진다")
    void movesSessionToNewOwner() {
        // 로그인은 세션 id 만 바꿔 재사용. 같은 객체가 두 목록에 남을 가능성
        MockHttpSession browser = new MockHttpSession();
        registry.register(1L, browser);
        registry.register(2L, browser);

        registry.invalidateAll(1L);

        assertThat(browser.isInvalid()).isFalse();
    }

    @Test
    @DisplayName("이미 끝난 세션이 섞여 있어도 나머지를 끊는다")
    void toleratesAlreadyInvalidatedSession() {
        MockHttpSession expired = new MockHttpSession();
        MockHttpSession alive = new MockHttpSession();
        registry.register(1L, expired);
        registry.register(1L, alive);
        expired.invalidate();

        assertThatCode(() -> registry.invalidateAll(1L)).doesNotThrowAnyException();
        assertThat(alive.isInvalid()).isTrue();
    }

    @Test
    @DisplayName("끝난 세션은 목록에서 빠진다")
    void forgetsDestroyedSession() {
        MockHttpSession loggedOut = new MockHttpSession();
        registry.register(1L, loggedOut);

        registry.sessionDestroyed(new HttpSessionEvent(loggedOut));

        // 목록에 남아 있었다면 invalidate() 재호출로 예외
        assertThat(loggedOut.isInvalid()).isFalse();
        registry.invalidateAll(1L);
        assertThat(loggedOut.isInvalid()).isFalse();
    }
}
