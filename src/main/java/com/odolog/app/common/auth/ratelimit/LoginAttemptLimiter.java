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
 * 시도 횟수 제한. 인메모리라 재시작 시 초기화
 * 로그인(이메일)·재설정 요청(접두사+이메일)·회원가입(접두사+IP) 공용. 잠금 문구는 호출하는 쪽이 전달
 * 없는 계정도 집계. 계정 존재 여부 노출 방지
 */
@Component
public class LoginAttemptLimiter {

    /** 잠금까지의 연속 실패 횟수 */
    private static final int MAX_FAILURES = 10;
    /** 실패 카운터 유지 시간 */
    private static final Duration WINDOW = Duration.ofMinutes(10);
    /** 잠금 유지 시간 */
    private static final Duration LOCK = Duration.ofMinutes(10);
    /** 만료 항목 정리 기준 크기 */
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
     * 잠겨 있으면 429. 검증보다 먼저 호출
     * reason 은 완성된 문장. 조사 자동 결합 시 받침 오류 방지
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

            // 마지막 실패가 오래됐으면 처음부터 집계
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

    /** 성공 시 기록 삭제. 옛 실패로 인한 잠금 방지 */
    public void recordSuccess(String key) {
        attempts.remove(key(key));
    }

    /**
     * 키 정규화(공백·대소문자)
     * DB 이메일 조회가 대소문자 무시라 A@x.com / a@x.com 교차 우회 차단
     */
    private String key(String rawKey) {
        return rawKey == null ? "" : rawKey.trim().toLowerCase(Locale.ROOT);
    }

    /** 임의 이메일 대량 입력 시 맵 무한 증가 방지 */
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
