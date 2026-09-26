package com.odolog.app.user.service.application;

import com.odolog.app.common.auth.ratelimit.LoginAttemptLimiter;
import com.odolog.app.common.auth.session.LoginSessionRegistry;
import com.odolog.app.common.exception.type.AuthenticationFailedException;
import com.odolog.app.user.domain.entity.PasswordResetToken;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.dto.request.password.PasswordResetConfirmRequest;
import com.odolog.app.user.repository.jpa.PasswordResetTokenRepository;
import com.odolog.app.user.repository.jpa.UserRepository;
import com.odolog.app.user.service.mail.PasswordResetMailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/** 비밀번호 재설정. 링크 발송(request) → 토큰으로 변경(confirm) */
@Service
@Transactional(readOnly = true)
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    /** 링크 유효 시간(분) */
    public static final int VALID_MINUTES = 30;

    /** 같은 주소로의 메일 폭주 방지. 로그인 리미터 공용 */
    private static final String RATE_LIMIT_PREFIX = "password-reset:";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetMailer mailer;
    private final LoginAttemptLimiter rateLimiter;
    private final LoginSessionRegistry sessionRegistry;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    // 스프링이 쓸 생성자 지정. 아래 생성자는 테스트의 시계 주입용
    @Autowired
    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordResetMailer mailer,
                                LoginAttemptLimiter rateLimiter,
                                LoginSessionRegistry sessionRegistry) {
        this(userRepository, tokenRepository, mailer, rateLimiter, sessionRegistry,
                Clock.systemDefaultZone());
    }

    PasswordResetService(UserRepository userRepository,
                         PasswordResetTokenRepository tokenRepository,
                         PasswordResetMailer mailer,
                         LoginAttemptLimiter rateLimiter,
                         LoginSessionRegistry sessionRegistry,
                         Clock clock) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailer = mailer;
        this.rateLimiter = rateLimiter;
        this.sessionRegistry = sessionRegistry;
        this.clock = clock;
    }

    /** 재설정 링크 발송. 없는 주소도 같은 정상 응답(가입 여부 노출 방지) */
    @Transactional
    public void request(String email) {
        String limitKey = RATE_LIMIT_PREFIX + email;
        rateLimiter.checkNotLocked(limitKey, "비밀번호 재설정 요청이 너무 많습니다.");
        rateLimiter.recordFailure(limitKey);

        // 만료 토큰 정리. 토큰이 쌓이는 유일한 경로라 스케줄러 불필요
        // 가입 여부 확인 전 실행. 주소 유무와 관계없이 같은 작업
        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now(clock));

        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty()) {
            return;
        }

        User user = found.get();
        // 재발급 시 이전 토큰 폐기
        tokenRepository.deleteByUserId(user.getId());

        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now(clock).plusMinutes(VALID_MINUTES);
        tokenRepository.save(new PasswordResetToken(user, hash(token), expiresAt));

        // 실제 발송은 커밋 후 다른 스레드(PasswordResetMailer). 응답 시간 차이 방지
        try {
            mailer.send(user.getEmail(), token, VALID_MINUTES);
        } catch (RuntimeException e) {
            // 발송 예약 실패도 삼킴. 가입된 주소에서만 500 이 나는 것 방지
            log.error("비밀번호 재설정 메일 예약 실패.", e);
        }
    }

    /** 토큰으로 비밀번호 변경. 성공 시 토큰 즉시 만료 */
    @Transactional
    public void confirm(PasswordResetConfirmRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);

        PasswordResetToken token = tokenRepository.findByTokenHash(hash(request.token()))
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(() -> new AuthenticationFailedException(
                        "링크가 만료되었거나 이미 사용되었습니다. 다시 요청해 주세요."));

        token.getUser().changePassword(passwordEncoder.encode(request.newPassword()));
        token.markUsed(now);

        // 로그인 잠금 해제
        rateLimiter.recordSuccess(token.getUser().getEmail());

        // 열려 있던 세션 전부 종료
        sessionRegistry.invalidateAll(token.getUser().getId());
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
