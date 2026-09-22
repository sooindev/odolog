package com.odolog.app.common.auth.ratelimit;

import com.odolog.app.common.exception.type.TooManyRequestsException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 시도 횟수 제한. 인메모리라 앱을 재시작하면 잊는다
 *
 * 세 곳이 키만 갈라 쓴다 — 로그인(이메일), 비밀번호 재설정 요청(접두사+이메일),
 * 회원가입(접두사+IP). 그래서 잠겼을 때의 문구는 여기서 정하지 않고 부르는 쪽이 넘긴다.
 * 안 그러면 회원가입 화면에 "로그인 시도가 너무 많습니다" 가 뜬다
 *
 * 로그인은 계정 존재 여부와 무관하게 센다 — 없는 이메일만 빨리 답하면
 * 그 자체가 존재 여부를 알려준다. 실패 메시지를 통일해 둔 방침과 같은 이유다
 */
@Component
public class LoginAttemptLimiter {

    /** 이 횟수만큼 연속 실패하면 잠근다 */
    private static final int MAX_FAILURES = 10;
    /** 이 시간 동안 실패가 없으면 카운터를 잊는다 */
    private static final Duration WINDOW = Duration.ofMinutes(10);
    /** 잠금 유지 시간 */
    private static final Duration LOCK = Duration.ofMinutes(10);
    /** 맵이 이보다 커지면 만료된 항목을 걷어낸다 */
    private static final int PURGE_THRESHOLD = 10_000;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptLimiter() {
        this(Clock.systemUTC());
    }

    LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * 잠겨 있으면 429. 검증보다 먼저 부른다
     *
     * reason 은 완성된 한 문장으로 받는다("로그인 시도가 너무 많습니다.").
     * 명사만 받아 여기서 조사를 붙이면 받침에 따라 "요청가" 같은 말이 나온다
     */
    public void checkNotLocked(String key, String reason) {
        Attempt attempt = attempts.get(key(key));
        if (attempt == null || attempt.lockedUntil == null) {
            return;
        }

        Instant now = clock.instant();
        if (now.isBefore(attempt.lockedUntil)) {
            long minutes = Math.max(1, Duration.between(now, attempt.lockedUntil).toMinutes() + 1);
            throw new TooManyRequestsException(reason + " " + minutes + "분 후 다시 시도해 주세요.");
        }
    }

    public void recordFailure(String key) {
        Instant now = clock.instant();

        attempts.compute(key(key), (ignored, current) -> {
            Attempt attempt = current == null ? new Attempt() : current;

            // 마지막 실패가 오래됐으면 처음부터 다시 센다
            if (attempt.lastFailure != null && Duration.between(attempt.lastFailure, now).compareTo(WINDOW) > 0) {
                attempt.failures = 0;
                attempt.lockedUntil = null;
            }

            attempt.failures += 1;
            attempt.lastFailure = now;

            if (attempt.failures >= MAX_FAILURES) {
                attempt.lockedUntil = now.plus(LOCK);
                attempt.failures = 0;
            }

            return attempt;
        });

        purgeIfCrowded(now);
    }

    /** 성공하면 기록을 지운다 — 평소에 쓰는 계정이 옛 실패 때문에 잠기지 않게 */
    public void recordSuccess(String key) {
        attempts.remove(key(key));
    }

    /**
     * 키를 정규화한다
     * 이메일 조회가 utf8mb4_unicode_ci 라 이미 대소문자를 가리지 않으므로,
     * 여기서 안 맞추면 A@x.com 과 a@x.com 으로 번갈아 보내 제한을 그냥 빠져나간다
     */
    private String key(String rawKey) {
        return rawKey == null ? "" : rawKey.trim().toLowerCase(Locale.ROOT);
    }

    /** 공격자가 임의의 이메일을 쏟아부으면 맵이 무한히 커진다 */
    private void purgeIfCrowded(Instant now) {
        if (attempts.size() < PURGE_THRESHOLD) {
            return;
        }

        attempts.values().removeIf(attempt ->
                (attempt.lockedUntil == null || now.isAfter(attempt.lockedUntil))
                        && attempt.lastFailure != null
                        && Duration.between(attempt.lastFailure, now).compareTo(WINDOW) > 0);
    }

    private static final class Attempt {
        private int failures;
        private Instant lastFailure;
        private Instant lockedUntil;
    }
}
