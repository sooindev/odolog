package com.odolog.app.user.service.application;

import com.odolog.app.common.auth.session.LoginSessionRegistry;
import com.odolog.app.common.auth.ratelimit.LoginAttemptLimiter;
import com.odolog.app.common.exception.type.AuthenticationFailedException;
import com.odolog.app.user.domain.entity.PasswordResetToken;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.dto.request.password.PasswordResetConfirmRequest;
import com.odolog.app.user.repository.jpa.PasswordResetTokenRepository;
import com.odolog.app.user.repository.jpa.UserRepository;
import com.odolog.app.user.service.mail.PasswordResetMailer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordResetMailer mailer;

    @Mock
    private LoginSessionRegistry sessionRegistry;

    @Mock
    private LoginAttemptLimiter rateLimiter;

    private PasswordResetService service;
    private User user;

    private static final Instant FIXED = Instant.parse("2026-09-21T00:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(FIXED, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, tokenRepository, mailer, rateLimiter, sessionRegistry,
                Clock.fixed(FIXED, ZoneOffset.UTC));

        user = new User("me@odolog.com", "old-hash", "닉네임", null);
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    @DisplayName("가입되지 않은 주소면 아무 일도 하지 않는다")
    void doesNothingForUnknownEmail() {
        // 없는 주소도 조용히 성공. 가입 여부 노출 방지
        when(userRepository.findByEmail("nobody@odolog.com")).thenReturn(Optional.empty());

        service.request("nobody@odolog.com");

        verify(mailer, never()).send(anyString(), anyString(), anyInt());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("가입된 주소면 토큰을 저장하고 메일을 보낸다")
    void issuesTokenAndSendsMail() {
        when(userRepository.findByEmail("me@odolog.com")).thenReturn(Optional.of(user));

        service.request("me@odolog.com");

        // 재발급 시 이전 토큰 폐기
        verify(tokenRepository).deleteByUserId(1L);

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(saved.capture());
        assertThat(saved.getValue().getExpiresAt())
                .isEqualTo(NOW.plusMinutes(PasswordResetService.VALID_MINUTES));

        verify(mailer).send(eq("me@odolog.com"), anyString(), anyInt());
    }

    @Test
    @DisplayName("저장하는 것은 원본이 아니라 해시다")
    void storesHashNotRawToken() {
        // DB 유출만으로는 비밀번호 변경 불가
        when(userRepository.findByEmail("me@odolog.com")).thenReturn(Optional.of(user));

        service.request("me@odolog.com");

        ArgumentCaptor<String> mailed = ArgumentCaptor.forClass(String.class);
        verify(mailer).send(anyString(), mailed.capture(), anyInt());

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(saved.capture());

        assertThat(saved.getValue().getTokenHash())
                .isNotEqualTo(mailed.getValue())
                .hasSize(64)
                .matches("[0-9a-f]+");
    }

    @Test
    @DisplayName("유효한 토큰이면 비밀번호를 바꾸고 그 토큰을 죽인다")
    void confirmChangesPassword() {
        String raw = "raw-token-value";
        PasswordResetToken token = new PasswordResetToken(user, hashOf(raw), NOW.plusMinutes(10));
        when(tokenRepository.findByTokenHash(hashOf(raw))).thenReturn(Optional.of(token));

        service.confirm(new PasswordResetConfirmRequest(raw, "new-password-1234"));

        assertThat(user.getPassword()).isNotEqualTo("old-hash");
        assertThat(token.getUsedAt()).isEqualTo(NOW);
        // 재설정 성공 시 로그인 잠금 해제
        verify(rateLimiter).recordSuccess("me@odolog.com");
        // 열려 있던 세션 전부 종료
        verify(sessionRegistry).invalidateAll(1L);
    }

    @Test
    @DisplayName("만료된 토큰이면 거절한다")
    void rejectsExpiredToken() {
        String raw = "raw-token-value";
        PasswordResetToken token = new PasswordResetToken(user, hashOf(raw), NOW.minusMinutes(1));
        when(tokenRepository.findByTokenHash(hashOf(raw))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.confirm(new PasswordResetConfirmRequest(raw, "new-password-1234")))
                .isInstanceOf(AuthenticationFailedException.class);

        assertThat(user.getPassword()).isEqualTo("old-hash");
    }

    @Test
    @DisplayName("이미 쓴 토큰이면 거절한다")
    void rejectsUsedToken() {
        String raw = "raw-token-value";
        PasswordResetToken token = new PasswordResetToken(user, hashOf(raw), NOW.plusMinutes(10));
        token.markUsed(NOW.minusMinutes(1));
        when(tokenRepository.findByTokenHash(hashOf(raw))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.confirm(new PasswordResetConfirmRequest(raw, "new-password-1234")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("없는 토큰이면 거절한다")
    void rejectsUnknownToken() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm(new PasswordResetConfirmRequest("아무값", "new-password-1234")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    /** 저장값과 같은 방식의 해싱 */
    private String hashOf(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("메일 발송이 실패해도 요청은 성공으로 끝난다")
    void survivesMailFailure() {
        // 메일 실패도 성공 응답. 가입된 주소에서만 500 이 나는 것 방지
        when(userRepository.findByEmail("me@odolog.com")).thenReturn(Optional.of(user));
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("SMTP 실패"))
                .when(mailer).send(anyString(), anyString(), anyInt());

        service.request("me@odolog.com");

        verify(tokenRepository).save(any());
    }
}
