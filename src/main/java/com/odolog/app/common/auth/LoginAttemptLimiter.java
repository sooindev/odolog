package com.odolog.app.common.auth;

import com.odolog.app.common.exception.ErrorCode;
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

    /** 창 안에서 허용하는 시도 횟수 */
    private static final int MAX_ATTEMPTS = 10;
    /** 시도 카운터 유지 시간 */
    private static final Duration WINDOW = Duration.ofMinutes(10);
    /** 잠금 유지 시간 */
    private static final Duration LOCK = Duration.ofMinutes(10);
    /** 만료 항목 정리 기준 크기 */
    private static final int PURGE_THRESHOLD = 10_000;
    /**
     * 맵 크기 상한. 10분 안에 서로 다른 키가 쏟아지면 만료 정리로는 못 줄임
     * 넘으면 한 번만 시도한 키부터 버림. 대입 대상은 시도가 여러 번이라 남음
     */
    private static final int HARD_LIMIT = 100_000;
    /** 정리 최소 간격. 기준을 넘긴 뒤 매 요청 전체 순회 방지 */
    private static final Duration PURGE_INTERVAL = Duration.ofMinutes(1);

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;
    private volatile Instant lastPurge = Instant.EPOCH;

    public LoginAttemptLimiter() {
        this(Clock.systemUTC());
    }

    LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * 시도 한 번 집계 후 한도를 넘었으면 429. 검증보다 먼저 호출, 성공하면 recordSuccess
     * 확인과 집계가 한 번의 compute. 동시 요청이 함께 확인을 통과하는 것 방지
     * reason 은 완성된 문장. 조사 자동 결합 시 받침 오류 방지. 화면 문구는 code 로
     */
    public void acquire(String key, ErrorCode code, String reason) {
        Instant now = clock.instant();
        Instant[] lockedUntil = new Instant[1];

        attempts.compute(key(key), (ignored, current) -> {
            Attempt attempt = current == null ? new Attempt() : current;

            if (attempt.lockedUntil != null && now.isBefore(attempt.lockedUntil)) {
                lockedUntil[0] = attempt.lockedUntil;
                return attempt;
            }

            // 마지막 시도가 오래됐거나 잠금이 끝났으면 처음부터 집계
            if (attempt.lockedUntil != null
                    || (attempt.lastAttempt != null && Duration.between(attempt.lastAttempt, now).compareTo(WINDOW) > 0)) {
                attempt.attempts = 0;
                attempt.lockedUntil = null;
            }

            attempt.attempts += 1;
            attempt.lastAttempt = now;

            if (attempt.attempts > MAX_ATTEMPTS) {
                attempt.lockedUntil = now.plus(LOCK);
                lockedUntil[0] = attempt.lockedUntil;
            }

            return attempt;
        });

        purgeIfCrowded(now);

        if (lockedUntil[0] != null) {
            long minutes = Math.max(1, Duration.between(now, lockedUntil[0]).toMinutes() + 1);
            throw new TooManyRequestsException(code, reason + " " + minutes + "분 후 다시 시도해 주세요.", minutes);
        }
    }

    /** 성공 시 기록 삭제. 옛 실패로 인한 잠금 방지 */
    public void recordSuccess(String key) {
        attempts.remove(key(key));
    }

    /**
     * 키 정규화(공백·대소문자)
     * 이메일은 출력 가능한 ASCII 만 받으므로 DB(unicode_ci) 비교와 같은 기준
     */
    private String key(String rawKey) {
        return rawKey == null ? "" : rawKey.trim().toLowerCase(Locale.ROOT);
    }

    /** 임의 이메일 대량 입력 시 맵 무한 증가 방지 */
    private void purgeIfCrowded(Instant now) {
        if (attempts.size() > HARD_LIMIT) {
            attempts.values().removeIf(attempt -> attempt.attempts <= 1 && attempt.lockedUntil == null);
        }
        if (attempts.size() < PURGE_THRESHOLD || now.isBefore(lastPurge.plus(PURGE_INTERVAL))) {
            return;
        }
        lastPurge = now;

        attempts.values().removeIf(attempt ->
                (attempt.lockedUntil == null || now.isAfter(attempt.lockedUntil))
                        && attempt.lastAttempt != null
                        && Duration.between(attempt.lastAttempt, now).compareTo(WINDOW) > 0);
    }

    private static final class Attempt {
        private int attempts;
        private Instant lastAttempt;
        private Instant lockedUntil;
    }
}
