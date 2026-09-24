package com.odolog.app.user.service.application;

import com.odolog.app.common.auth.ratelimit.LoginAttemptLimiter;
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

/**
 * 비밀번호 재설정
 *
 * 두 단계다: 메일로 링크를 보내고(request), 그 링크의 토큰으로 비밀번호를 바꾼다(confirm)
 */
@Service
@Transactional(readOnly = true)
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    /** 링크 유효 시간. 길면 메일함이 곧 열쇠가 되고, 짧으면 메일 도착 전에 만료된다 */
    public static final int VALID_MINUTES = 30;

    /** 같은 주소로 메일을 쏟아붓지 못하게. 로그인 제한과 같은 장치를 키만 갈라 쓴다 */
    private static final String RATE_LIMIT_PREFIX = "password-reset:";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetMailer mailer;
    private final LoginAttemptLimiter rateLimiter;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    // 생성자가 둘이라 어느 쪽을 쓸지 스프링에게 알려 줘야 한다.
    // 아래 것은 시계를 갈아 끼우려고 테스트만 쓰는 통로다
    @Autowired
    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordResetMailer mailer,
                                LoginAttemptLimiter rateLimiter) {
        this(userRepository, tokenRepository, mailer, rateLimiter, Clock.systemDefaultZone());
    }

    PasswordResetService(UserRepository userRepository,
                         PasswordResetTokenRepository tokenRepository,
                         PasswordResetMailer mailer,
                         LoginAttemptLimiter rateLimiter,
                         Clock clock) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailer = mailer;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    /**
     * 재설정 링크를 보낸다
     *
     * 가입되지 않은 주소여도 **아무 일도 일어나지 않은 채 정상 응답**한다 —
     * 여기서 404 를 주면 그게 곧 가입 여부 조회 API 가 된다.
     * 로그인 실패 메시지를 일부러 통일해 둔 방침과 같은 이유다
     */
    @Transactional
    public void request(String email) {
        String limitKey = RATE_LIMIT_PREFIX + email;
        rateLimiter.checkNotLocked(limitKey, "비밀번호 재설정 요청이 너무 많습니다.");
        rateLimiter.recordFailure(limitKey);

        /*
         * 만료된 토큰을 여기서 같이 치운다
         *
         * 스케줄러를 따로 두지 않은 이유: 토큰이 쌓이는 유일한 경로가 이 메서드라,
         * 여기가 곧 "쌓이는 만큼 치워지는" 자리다. 앱이 안 뜨는 시간에도 돌아야 할 일이 아니다.
         * 가입 여부를 확인하기 **전에** 부르는 것도 의도다 — 없는 주소로 요청해도 하는 일이
         * 같아야 응답 시간으로 가입 여부가 드러나지 않는다
         */
        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now(clock));

        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty()) {
            return;
        }

        User user = found.get();
        // 새로 발급하면 이전 것은 버린다. 메일함에 남은 옛 링크가 계속 열쇠면 안 된다
        tokenRepository.deleteByUserId(user.getId());

        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now(clock).plusMinutes(VALID_MINUTES);
        tokenRepository.save(new PasswordResetToken(user, hash(token), expiresAt));

        try {
            mailer.send(user.getEmail(), token, VALID_MINUTES);
        } catch (RuntimeException e) {
            // 발송 실패를 그대로 올려보내면 가입된 주소에서만 500 이 나고, 그 차이가
            // 곧 가입 여부를 알려준다. 위에서 없는 주소를 조용히 넘긴 것이 무의미해진다
            //
            // 대신 로그에 남긴다. 메일 설정이 잘못된 것은 운영 쪽 문제이지
            // 요청한 사람이 알아서 할 수 있는 일이 아니다
            log.error("비밀번호 재설정 메일 발송 실패. 메일 설정을 확인하세요.", e);
        }
    }

    /** 토큰으로 비밀번호를 바꾼다. 성공하면 그 토큰은 즉시 죽는다 */
    @Transactional
    public void confirm(PasswordResetConfirmRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);

        PasswordResetToken token = tokenRepository.findByTokenHash(hash(request.token()))
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(() -> new AuthenticationFailedException(
                        "링크가 만료되었거나 이미 사용되었습니다. 다시 요청해 주세요."));

        token.getUser().changePassword(passwordEncoder.encode(request.newPassword()));
        token.markUsed(now);

        // 비밀번호를 바꿨으니 로그인 잠금도 푼다 — 잊어버려서 여러 번 틀린 사람이 여기까지 왔다
        rateLimiter.recordSuccess(token.getUser().getEmail());
    }

    /** 256비트. 추측으로 맞힐 수 없어야 한다 */
    private String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256. 비밀번호와 달리 BCrypt 를 쓰지 않는다 —
     * 토큰은 우리가 만든 256비트 난수라 사전 공격 대상이 아니고,
     * BCrypt 는 같은 값도 매번 다른 해시를 내놓아 조회 키로 쓸 수 없다
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
