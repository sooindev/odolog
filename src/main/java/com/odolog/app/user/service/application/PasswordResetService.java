package com.odolog.app.user.service.application;

import com.odolog.app.common.exception.ErrorCode;
import com.odolog.app.common.auth.LoginAttemptLimiter;
import com.odolog.app.common.auth.LoginSessionRegistry;
import com.odolog.app.common.exception.type.AuthenticationFailedException;
import com.odolog.app.user.domain.entity.PasswordResetToken;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.dto.request.password.PasswordResetConfirmRequest;
import com.odolog.app.user.repository.PasswordResetTokenRepository;
import com.odolog.app.user.repository.UserRepository;
import com.odolog.app.user.service.PasswordResetMailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.Semaphore;

/** 비밀번호 재설정. 링크 발송(request) → 토큰으로 변경(confirm) */
@Service
@Transactional(readOnly = true)
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    /** 링크 유효 시간(분) */
    public static final int VALID_MINUTES = 30;

    /** 같은 주소로의 메일 폭주 방지. 로그인 리미터 공용 */
    private static final String RATE_LIMIT_PREFIX = "password-reset:";

    /**
     * 처리 대기 중인 발급 작업 상한. 넘으면 조용히 버림(응답은 어차피 204)
     * 주소마다 다른 대량 요청이 실행기 대기열을 끝없이 채워 진짜 요청이 수십 초 밀리는 것 방지
     */
    static final int MAX_PENDING = 200;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetMailer mailer;
    private final LoginAttemptLimiter rateLimiter;
    private final LoginSessionRegistry sessionRegistry;
    private final TaskExecutor taskExecutor;
    private final TransactionTemplate transactionTemplate;
    private final Semaphore pending = new Semaphore(MAX_PENDING);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    // 스프링이 쓸 생성자 지정. 아래 생성자는 테스트의 시계 주입용
    @Autowired
    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordResetMailer mailer,
                                LoginAttemptLimiter rateLimiter,
                                LoginSessionRegistry sessionRegistry,
                                TaskExecutor taskExecutor,
                                PlatformTransactionManager transactionManager) {
        this(userRepository, tokenRepository, mailer, rateLimiter, sessionRegistry,
                taskExecutor, transactionManager, Clock.systemDefaultZone());
    }

    PasswordResetService(UserRepository userRepository,
                         PasswordResetTokenRepository tokenRepository,
                         PasswordResetMailer mailer,
                         LoginAttemptLimiter rateLimiter,
                         LoginSessionRegistry sessionRegistry,
                         TaskExecutor taskExecutor,
                         PlatformTransactionManager transactionManager,
                         Clock clock) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailer = mailer;
        this.rateLimiter = rateLimiter;
        this.sessionRegistry = sessionRegistry;
        this.taskExecutor = taskExecutor;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    /**
     * 재설정 링크 발송. 없는 주소도 같은 정상 응답(가입 여부 노출 방지)
     * 횟수 집계만 요청 스레드에서. 조회·토큰 저장은 다른 스레드라 가입 여부와 무관하게 같은 응답 시간
     */
    public void request(String email) {
        String limitKey = RATE_LIMIT_PREFIX + email;
        rateLimiter.acquire(limitKey, ErrorCode.TOO_MANY_RESET_REQUESTS, "비밀번호 재설정 요청이 너무 많습니다.");

        if (!pending.tryAcquire()) {
            log.warn("비밀번호 재설정 대기 작업이 상한({})에 닿아 요청을 버립니다.", MAX_PENDING);
            return;
        }
        try {
            taskExecutor.execute(() -> {
                try {
                    issueWithRetry(email);
                } finally {
                    pending.release();
                }
            });
        } catch (RuntimeException e) {
            // 실행기가 거절하면 작업이 돌지 않으므로 여기서 반납
            pending.release();
            log.error("비밀번호 재설정 작업을 맡기지 못했습니다.", e);
        }
    }

    /**
     * 만료 정리 → 발급. 각각 따로 트랜잭션
     * 동시 요청과 같은 행을 건드려 충돌하면 한 번 다시. 응답은 이미 나가서 실패는 로그로만
     */
    private void issueWithRetry(String email) {
        try {
            // 정리 실패가 발급을 막지 않게 분리
            transactionTemplate.executeWithoutResult(
                    status -> tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now(clock)));
        } catch (RuntimeException e) {
            log.warn("만료된 재설정 토큰 정리 실패. 다음 요청 때 다시 정리됩니다.", e);
        }

        for (int attempt = 1; ; attempt++) {
            try {
                transactionTemplate.executeWithoutResult(status -> issue(email));
                return;
            } catch (ConcurrencyFailureException e) {
                if (attempt >= 2) {
                    log.error("비밀번호 재설정 토큰 발급 실패(동시 요청과 충돌).", e);
                    return;
                }
            } catch (RuntimeException e) {
                log.error("비밀번호 재설정 토큰 발급 실패.", e);
                return;
            }
        }
    }

    /** 토큰 발급 + 커밋 후 메일 예약. 다른 스레드에서 트랜잭션 안에 실행 */
    void issue(String email) {
        // 첫 조회가 잠금. 같은 주소의 동시 요청은 여기서 기다렸다가 앞선 발급의 결과를 보고 진행
        Optional<User> found = userRepository.findLockedByEmail(email);
        if (found.isEmpty()) {
            return;
        }

        User user = found.get();
        // 재발급 시 이전 토큰 폐기
        tokenRepository.deleteByUserId(user.getId());

        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now(clock).plusMinutes(VALID_MINUTES);
        tokenRepository.save(new PasswordResetToken(user, hash(token), expiresAt));

        try {
            mailer.send(user.getEmail(), token, VALID_MINUTES, user.getLanguage());
        } catch (RuntimeException e) {
            // 토큰은 저장. 메일만 실패
            log.error("비밀번호 재설정 메일 예약 실패.", e);
        }
    }

    /** 토큰으로 비밀번호 변경. 성공 시 토큰 즉시 만료 */
    @Transactional
    public void confirm(PasswordResetConfirmRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);

        PasswordResetToken token = tokenRepository.findByTokenHash(hash(request.token()))
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(() -> new AuthenticationFailedException(ErrorCode.RESET_LINK_INVALID,
                        "링크가 만료되었거나 이미 사용되었습니다. 다시 요청해 주세요."));

        token.getUser().changePassword(passwordEncoder.encode(request.newPassword()));
        token.markUsed(now);

        // 잠금 해제·세션 종료는 커밋 뒤. 세션 표는 별도 트랜잭션이라 안에서 지우면 롤백돼도 사라짐
        String loginKey = UserService.LOGIN_KEY_PREFIX + token.getUser().getEmail();
        Long userId = token.getUser().getId();
        afterCommit(() -> {
            rateLimiter.recordSuccess(loginKey);
            sessionRegistry.invalidateAll(userId);
        });
    }

    /** 트랜잭션 밖(단위 테스트)이면 바로 실행 */
    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    /** 256비트 난수 */
    private String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256. 256비트 난수라 사전 공격 대상 아님
     * BCrypt 는 같은 값도 매번 다른 해시라 조회 키 불가
     */
    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 쓸 수 없습니다", e);
        }
    }
}
