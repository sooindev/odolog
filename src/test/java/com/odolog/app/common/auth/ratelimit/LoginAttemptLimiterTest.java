package com.odolog.app.common.auth.ratelimit;

import com.odolog.app.common.exception.type.TooManyRequestsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptLimiterTest {

    /** 시계를 밖에서 넣는다. 안에서 now() 를 부르면 잠금 만료를 테스트할 수 없다 */
    private static final class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-09-21T00:00:00Z");

        void advance(Duration amount) {
            now = now.plus(amount);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    private void fail(LoginAttemptLimiter limiter, String email, int times) {
        for (int i = 0; i < times; i++) {
            limiter.recordFailure(email);
        }
    }

    @Test
    @DisplayName("실패가 한도 미만이면 막지 않는다")
    void allowsUnderThreshold() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(new MovableClock());

        fail(limiter, "a@odolog.com", 9);

        assertThatCode(() -> limiter.checkNotLocked("a@odolog.com")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("한도를 채우면 429를 던진다")
    void locksAtThreshold() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(new MovableClock());

        fail(limiter, "a@odolog.com", 10);

        assertThatThrownBy(() -> limiter.checkNotLocked("a@odolog.com"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("다시 시도");
    }

    @Test
    @DisplayName("잠금은 시간이 지나면 풀린다")
    void unlocksAfterLockDuration() {
        MovableClock clock = new MovableClock();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);
        fail(limiter, "a@odolog.com", 10);

        clock.advance(Duration.ofMinutes(11));

        assertThatCode(() -> limiter.checkNotLocked("a@odolog.com")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("실패 사이 간격이 창을 넘으면 처음부터 다시 센다")
    void forgetsOldFailures() {
        // 하루에 한 번씩 오타를 내는 사람이 열흘 뒤에 잠기면 안 된다
        MovableClock clock = new MovableClock();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

        for (int i = 0; i < 20; i++) {
            limiter.recordFailure("a@odolog.com");
            clock.advance(Duration.ofMinutes(11));
        }

        assertThatCode(() -> limiter.checkNotLocked("a@odolog.com")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("성공하면 실패 기록을 지운다")
    void successClearsFailures() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(new MovableClock());
        fail(limiter, "a@odolog.com", 9);

        limiter.recordSuccess("a@odolog.com");
        fail(limiter, "a@odolog.com", 9);

        assertThatCode(() -> limiter.checkNotLocked("a@odolog.com")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("대소문자와 공백을 다르게 써도 같은 계정으로 센다")
    void normalizesEmailKey() {
        // 안 맞추면 A@x.com / a@x.com 을 번갈아 보내 제한을 그냥 빠져나간다
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(new MovableClock());

        fail(limiter, "A@Odolog.com", 5);
        fail(limiter, "  a@odolog.com  ", 5);

        assertThatThrownBy(() -> limiter.checkNotLocked("a@odolog.com"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    @DisplayName("다른 계정의 실패는 서로 영향을 주지 않는다")
    void countsPerAccount() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(new MovableClock());

        fail(limiter, "a@odolog.com", 10);

        assertThatCode(() -> limiter.checkNotLocked("b@odolog.com")).doesNotThrowAnyException();
    }
}
