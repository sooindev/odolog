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
    @DisplayName("창은 첫 시도부터 10분이다 — 창보다 짧은 간격으로 드문드문 이어진 시도는 쌓이지 않는다")
    void windowStartsAtFirstAttempt() {
        MovableClock clock = new MovableClock();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

        // 마지막 시도 기준이면 9분 간격은 영영 초기화되지 않아 11번째에 잠김
        for (int i = 0; i < 20; i++) {
            assertThatCode(() -> attempt(limiter, "a@odolog.com")).doesNotThrowAnyException();
            clock.advance(Duration.ofMinutes(9));
        }
    }

    @Test
    @DisplayName("같은 창 안에서 한도를 넘으면 시도 간격과 무관하게 잠근다")
    void locksWithinOneWindow() {
        MovableClock clock = new MovableClock();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

        // 10번을 9분에 나눠 보내고 11번째도 창(10분) 안
        for (int i = 0; i < 10; i++) {
            attempt(limiter, "a@odolog.com");
            clock.advance(Duration.ofSeconds(54));
        }

        assertThatThrownBy(() -> attempt(limiter, "a@odolog.com"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    @DisplayName("되돌린 시도는 한도에 세지 않는다 — 같은 IP 의 성공한 로그인이 쌓여 잠기지 않음")
    void refundedAttemptsDoNotCount() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(new MovableClock());

        for (int i = 0; i < 100; i++) {
            limiter.acquireShared("login-ip:1.2.3.4", ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, REASON);
            limiter.refund("login-ip:1.2.3.4");
        }

        assertThatCode(() -> limiter.acquireShared("login-ip:1.2.3.4", ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, REASON))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("되돌리기는 자기 시도 하나만 — 사이에 끼운 성공이 앞선 실패 집계를 지우지 않는다")
    void refundKeepsOtherFailures() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(new MovableClock());

        attempt(limiter, "login-ip:1.2.3.4", 5);
        for (int i = 0; i < 10; i++) {
            attempt(limiter, "login-ip:1.2.3.4");
            limiter.refund("login-ip:1.2.3.4");
        }
        attempt(limiter, "login-ip:1.2.3.4", 5);

        // 실패만 11번째
        assertThatThrownBy(() -> attempt(limiter, "login-ip:1.2.3.4"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    @DisplayName("되돌려도 이미 걸린 잠금은 풀리지 않는다")
    void refundDoesNotLift() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(new MovableClock());
        attempt(limiter, "a@odolog.com", 10);
        assertThatThrownBy(() -> attempt(limiter, "a@odolog.com")).isInstanceOf(TooManyRequestsException.class);

        limiter.refund("a@odolog.com");

        assertThatThrownBy(() -> attempt(limiter, "a@odolog.com")).isInstanceOf(TooManyRequestsException.class);
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

    @Test
    @DisplayName("재설정을 마치면 그 계정 키와 모든 IP 짝 키를 지우고, 접두사만 같은 다른 계정은 남긴다")
    void forgetWithPairsClearsAccountAndPairsOnly() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(Clock.systemUTC(), 1);
        String account = "login:a@b.com";
        limiter.acquire(LoginAttemptLimiter.pair(account, "1.1.1.1"), ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, "잠김");
        limiter.acquire(LoginAttemptLimiter.pair("login:a@b.com.x", "1.1.1.1"), ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, "잠김");

        limiter.forgetWithPairs(account);

        // 지워진 짝은 다시 한 번 통과
        assertThatCode(() -> limiter.acquire(LoginAttemptLimiter.pair(account, "1.1.1.1"),
                ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, "잠김")).doesNotThrowAnyException();
        // 다른 계정의 짝은 남아 두 번째에서 잠김
        assertThatThrownBy(() -> limiter.acquire(LoginAttemptLimiter.pair("login:a@b.com.x", "1.1.1.1"),
                ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, "잠김")).isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    @DisplayName("계정 키가 잠겨 거절된 시도는 짝 키에 세지 않는다 — 계정 잠금이 풀린 뒤 자기 짝 잠금으로 더 기다리지 않게")
    void rejectedByAccountLockDoesNotCountOnPair() {
        MovableClock clock = new MovableClock();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);
        String account = "login:victim@odolog.com";

        // 다섯 곳에서 짝 한도(10)씩 채우면 계정 전체(50)가 다음 시도에 잠김
        for (int ip = 1; ip <= 5; ip++) {
            for (int i = 0; i < 10; i++) {
                limiter.acquirePaired(account, "10.0.0." + ip, ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, REASON);
            }
        }
        assertThatThrownBy(() -> limiter.acquirePaired(account, "10.0.0.6", ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, REASON))
                .isInstanceOf(TooManyRequestsException.class);

        // 잠긴 동안 진짜 사용자가 자기 IP 에서 열한 번 다시 누름
        clock.advance(Duration.ofMinutes(5));
        for (int i = 0; i < 11; i++) {
            assertThatThrownBy(() -> limiter.acquirePaired(account, "192.168.0.7", ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, REASON))
                    .isInstanceOf(TooManyRequestsException.class);
        }

        // 계정 잠금이 풀리면 바로 시도할 수 있음
        clock.advance(Duration.ofMinutes(5).plusSeconds(1));
        assertThatCode(() -> limiter.acquirePaired(account, "192.168.0.7", ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, REASON))
                .doesNotThrowAnyException();
    }
}
