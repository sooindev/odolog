package com.odolog.app.common.auth;

import com.odolog.app.common.exception.ErrorCode;
import com.odolog.app.common.exception.type.TooManyRequestsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptLimiterTest {

    /** 시계 주입. 잠금 만료 테스트용 */
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

    private static final String REASON = "로그인 시도가 너무 많습니다.";

    private void attempt(LoginAttemptLimiter limiter, String key) {
        limiter.acquire(key, ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, REASON);
    }

    private void attempt(LoginAttemptLimiter limiter, String key, int times) {
        for (int i = 0; i < times; i++) {
            attempt(limiter, key);
        }
    }

    @Test
    @DisplayName("한도까지는 막지 않는다")
    void allowsUpToThreshold() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(new MovableClock());

        assertThatCode(() -> attempt(limiter, "a@odolog.com", 10)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("공유 키(IP)는 한 사람 한도의 다섯 배까지 받는다")
    void sharedKeyAllowsFiveTimes() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(new MovableClock());
        for (int i = 0; i < 50; i++) {
            limiter.acquireShared("login-ip:1.2.3.4", ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, REASON);
        }

        assertThatThrownBy(() -> limiter.acquireShared("login-ip:1.2.3.4", ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, REASON))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    @DisplayName("한도를 넘는 시도는 429를 던진다")
    void locksOverThreshold() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(new MovableClock());
        attempt(limiter, "a@odolog.com", 10);

        assertThatThrownBy(() -> attempt(limiter, "a@odolog.com"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("다시 시도");
    }

    @Test
    @DisplayName("남은 분은 올림한다. 막 잠기면 10분, 9분 30초 뒤면 1분")
    void roundsRemainingMinutesUp() {
        MovableClock clock = new MovableClock();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);
        attempt(limiter, "a@odolog.com", 10);

        assertThatThrownBy(() -> attempt(limiter, "a@odolog.com"))
                .isInstanceOfSatisfying(TooManyRequestsException.class,
                        e -> assertThat(e.getRetryAfterMinutes()).isEqualTo(10))
                .hasMessageContaining("10분 후");

        clock.advance(Duration.ofSeconds(30));
        assertThatThrownBy(() -> attempt(limiter, "a@odolog.com"))
                .isInstanceOfSatisfying(TooManyRequestsException.class,
                        e -> assertThat(e.getRetryAfterMinutes()).isEqualTo(10));

        clock.advance(Duration.ofSeconds(9 * 60));
        assertThatThrownBy(() -> attempt(limiter, "a@odolog.com"))
                .isInstanceOfSatisfying(TooManyRequestsException.class,
                        e -> assertThat(e.getRetryAfterMinutes()).isEqualTo(1));
    }

    @Test
    @DisplayName("잠긴 이유는 부르는 쪽이 넘긴 문장을 그대로 쓴다")
    void usesCallerSuppliedReason() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(new MovableClock());
        for (int i = 0; i < 10; i++) {
            limiter.acquire("signup:10.0.0.1", ErrorCode.TOO_MANY_SIGNUP_ATTEMPTS, "회원가입 시도가 너무 많습니다.");
        }

        // 잠금 문구는 호출하는 쪽이 결정
        assertThatThrownBy(() -> limiter.acquire("signup:10.0.0.1", ErrorCode.TOO_MANY_SIGNUP_ATTEMPTS, "회원가입 시도가 너무 많습니다."))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("회원가입 시도가 너무 많습니다.");
    }

    @Test
    @DisplayName("잠금은 시간이 지나면 풀리고 처음부터 다시 센다")
    void unlocksAfterLockDuration() {
        MovableClock clock = new MovableClock();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);
        attempt(limiter, "a@odolog.com", 10);
        assertThatThrownBy(() -> attempt(limiter, "a@odolog.com")).isInstanceOf(TooManyRequestsException.class);

        clock.advance(Duration.ofMinutes(11));

        assertThatCode(() -> attempt(limiter, "a@odolog.com", 10)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("잠긴 동안의 시도는 잠금을 늘리지 않는다")
    void lockedAttemptsDoNotExtendLock() {
        MovableClock clock = new MovableClock();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);
        attempt(limiter, "a@odolog.com", 10);
        assertThatThrownBy(() -> attempt(limiter, "a@odolog.com")).isInstanceOf(TooManyRequestsException.class);

        clock.advance(Duration.ofMinutes(9));
        assertThatThrownBy(() -> attempt(limiter, "a@odolog.com")).isInstanceOf(TooManyRequestsException.class);
        clock.advance(Duration.ofMinutes(2));

        assertThatCode(() -> attempt(limiter, "a@odolog.com")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("시도 사이 간격이 창을 넘으면 처음부터 다시 센다")
    void forgetsOldAttempts() {
        // 드문 오타가 누적되어 잠기지 않음
        MovableClock clock = new MovableClock();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

        for (int i = 0; i < 20; i++) {
            attempt(limiter, "a@odolog.com");
            clock.advance(Duration.ofMinutes(11));
        }

        assertThatCode(() -> attempt(limiter, "a@odolog.com")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("성공하면 기록을 지운다")
    void successClearsAttempts() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(new MovableClock());
        attempt(limiter, "a@odolog.com", 9);

        limiter.recordSuccess("a@odolog.com");

        assertThatCode(() -> attempt(limiter, "a@odolog.com", 10)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("대소문자와 공백을 다르게 써도 같은 계정으로 센다")
    void normalizesEmailKey() {
        // 대소문자 교차 우회 차단
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(new MovableClock());

        attempt(limiter, "A@Odolog.com", 5);
        attempt(limiter, "  a@odolog.com  ", 5);

        assertThatThrownBy(() -> attempt(limiter, "a@odolog.com"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    @DisplayName("다른 계정의 시도는 서로 영향을 주지 않는다")
    void countsPerAccount() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(new MovableClock());

        attempt(limiter, "a@odolog.com", 10);

        assertThatCode(() -> attempt(limiter, "b@odolog.com")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("동시에 들어온 시도도 한도만큼만 통과한다")
    void concurrentAttemptsRespectLimit() throws InterruptedException {
        // 확인과 집계가 따로면 BCrypt 동안 모두 확인을 통과
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(new MovableClock());
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger passed = new AtomicInteger();

        for (int i = 0; i < 200; i++) {
            pool.submit(() -> {
                start.await();
                try {
                    attempt(limiter, "a@odolog.com");
                    passed.incrementAndGet();
                } catch (TooManyRequestsException ignored) {
                    // 잠김
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(passed.get()).isEqualTo(10);
    }

    @Test
    @DisplayName("서로 다른 키가 상한을 넘게 쏟아져도 한 번만 시도한 키부터 버리고 잠긴 키는 남긴다")
    void dropsOneShotKeysWhenFlooded() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(new MovableClock());
        for (int i = 0; i < 11; i++) {
            try {
                limiter.acquire("login:victim@x.com", ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, "잠김.");
            } catch (TooManyRequestsException expected) {
                // 11번째에 잠김
            }
        }

        // 10분 안의 서로 다른 키 대량 유입
        for (int i = 0; i < 100_002; i++) {
            limiter.acquire("password-reset:flood" + i + "@x.com", ErrorCode.TOO_MANY_RESET_REQUESTS, "많음.");
        }

        Map<?, ?> map = (Map<?, ?>) ReflectionTestUtils.getField(limiter, "attempts");
        assertThat(map.size()).isLessThan(100_000);
        assertThatThrownBy(() -> limiter.acquire("login:victim@x.com", ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, "잠김."))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    @DisplayName("키마다 두 번씩 쏟아내도 맵이 상한을 넘어 자라지 않는다 — 시도 횟수가 아니라 오래 쉰 순서로 버린다")
    void evictsIdlestEvenWhenEachKeyTriedTwice() {
        MovableClock clock = new MovableClock();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

        for (int i = 0; i < LoginAttemptLimiter.HARD_LIMIT + 10; i++) {
            String key = "password-reset:flood" + i + "@x.com";
            limiter.acquire(key, ErrorCode.TOO_MANY_RESET_REQUESTS, "많음.");
            limiter.acquire(key, ErrorCode.TOO_MANY_RESET_REQUESTS, "많음.");
            if (i % 1000 == 0) {
                clock.advance(Duration.ofMillis(1));
            }
        }

        Map<?, ?> map = (Map<?, ?>) ReflectionTestUtils.getField(limiter, "attempts");
        assertThat(map.size()).isLessThanOrEqualTo(LoginAttemptLimiter.HARD_LIMIT);
    }
}
