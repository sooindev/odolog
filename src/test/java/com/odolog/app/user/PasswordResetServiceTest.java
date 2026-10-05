package com.odolog.app.user;

import com.odolog.app.common.auth.LoginAttemptLimiter;
import com.odolog.app.common.auth.LoginSessionRegistry;
import com.odolog.app.common.exception.type.AuthenticationFailedException;
import com.odolog.app.user.domain.PasswordResetToken;
import com.odolog.app.user.domain.User;
import com.odolog.app.user.dto.request.PasswordResetConfirmRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Mock
    private PlatformTransactionManager transactionManager;

    /** 다른 스레드 대신 넘겨받은 작업을 모아 두는 실행기. run() 으로 실행 */
    private final java.util.List<Runnable> scheduled = new java.util.ArrayList<>();

    private PasswordResetService service;
    private User user;

    private static final Instant FIXED = Instant.parse("2026-09-21T00:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(FIXED, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, tokenRepository, mailer, rateLimiter, sessionRegistry,
                scheduled::add, transactionManager, new BCryptPasswordEncoder(), Clock.fixed(FIXED, ZoneOffset.UTC));

        user = new User("me@odolog.com", "old-hash", "닉네임");
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    @DisplayName("가입되지 않은 주소면 아무 일도 하지 않는다")
    void doesNothingForUnknownEmail() {
        // 없는 주소도 조용히 성공. 가입 여부 노출 방지
        when(userRepository.findLockedByEmail("nobody@odolog.com")).thenReturn(Optional.empty());

        service.request("nobody@odolog.com");
        runScheduled();

        verify(mailer, never()).send(anyString(), anyString(), anyInt(), any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("가입된 주소면 토큰을 저장하고 메일을 보낸다")
    void issuesTokenAndSendsMail() {
        when(userRepository.findLockedByEmail("me@odolog.com")).thenReturn(Optional.of(user));

        service.request("me@odolog.com");
        runScheduled();

        // 재발급 시 이전 토큰 폐기
        verify(tokenRepository).deleteByUserId(1L);

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(saved.capture());
        assertThat(saved.getValue().getExpiresAt())
                .isEqualTo(NOW.plusMinutes(PasswordResetService.VALID_MINUTES));

        verify(mailer).send(eq("me@odolog.com"), anyString(), anyInt(), any());
    }

    @Test
    @DisplayName("저장하는 것은 원본이 아니라 해시다")
    void storesHashNotRawToken() {
        // DB 유출만으로는 비밀번호 변경 불가
        when(userRepository.findLockedByEmail("me@odolog.com")).thenReturn(Optional.of(user));

        service.request("me@odolog.com");
        runScheduled();

        ArgumentCaptor<String> mailed = ArgumentCaptor.forClass(String.class);
        verify(mailer).send(anyString(), mailed.capture(), anyInt(), any());

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
        stubConfirm(raw, token);

        service.confirm(new PasswordResetConfirmRequest(raw, "new-password-1234"));

        assertThat(user.getPassword()).isNotEqualTo("old-hash");
        assertThat(token.getUsedAt()).isEqualTo(NOW);
        // 재설정 성공 시 로그인 잠금 해제
        verify(rateLimiter).recordSuccess("login:me@odolog.com");
        // 열려 있던 세션 전부 종료
        verify(sessionRegistry).invalidateAll(1L);
    }

    @Test
    @DisplayName("트랜잭션 안이면 잠금 해제·세션 종료를 커밋 뒤로 미룬다")
    void confirmDefersSideEffectsUntilCommit() {
        String raw = "raw-token-value";
        PasswordResetToken token = new PasswordResetToken(user, hashOf(raw), NOW.plusMinutes(10));
        stubConfirm(raw, token);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.confirm(new PasswordResetConfirmRequest(raw, "new-password-1234"));

            // 커밋 전. 여기서 롤백되면 세션·잠금이 그대로 남아야 함
            verifyNoInteractions(sessionRegistry);
            verify(rateLimiter, never()).recordSuccess(anyString());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(rateLimiter).recordSuccess("login:me@odolog.com");
        verify(sessionRegistry).invalidateAll(1L);
    }

    @Test
    @DisplayName("만료된 토큰이면 거절한다")
    void rejectsExpiredToken() {
        String raw = "raw-token-value";
        PasswordResetToken token = new PasswordResetToken(user, hashOf(raw), NOW.minusMinutes(1));
        stubConfirm(raw, token);

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
        stubConfirm(raw, token);

        assertThatThrownBy(() -> service.confirm(new PasswordResetConfirmRequest(raw, "new-password-1234")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("없는 토큰이면 거절한다")
    void rejectsUnknownToken() {
        when(tokenRepository.findUserIdByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm(new PasswordResetConfirmRequest("아무값", "new-password-1234")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("재설정 확정은 사용자 행을 먼저 잠그고 토큰을 나중에 잠근다(발급과 같은 순서)")
    void confirmLocksUserBeforeToken() {
        String raw = "raw-token-value";
        stubConfirm(raw, new PasswordResetToken(user, hashOf(raw), NOW.plusMinutes(10)));

        service.confirm(new PasswordResetConfirmRequest(raw, "new-password-1234"));

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(userRepository, tokenRepository);
        order.verify(userRepository).findLockedById(1L);
        order.verify(tokenRepository).findByTokenHash(hashOf(raw));
    }

    @Test
    @DisplayName("재설정하면 비밀번호 확인 잠금도 풀린다")
    void confirmClearsPasswordCheckLock() {
        String raw = "raw-token-value";
        stubConfirm(raw, new PasswordResetToken(user, hashOf(raw), NOW.plusMinutes(10)));

        service.confirm(new PasswordResetConfirmRequest(raw, "new-password-1234"));

        verify(rateLimiter).recordSuccess("password-check:1");
    }

    /** 주인 조회 → 사용자 잠금 → 토큰 잠금 */
    private void stubConfirm(String raw, PasswordResetToken token) {
        when(tokenRepository.findUserIdByTokenHash(hashOf(raw))).thenReturn(Optional.of(1L));
        when(userRepository.findLockedById(1L)).thenReturn(Optional.of(user));
        when(tokenRepository.findByTokenHash(hashOf(raw))).thenReturn(Optional.of(token));
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
        when(userRepository.findLockedByEmail("me@odolog.com")).thenReturn(Optional.of(user));
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("SMTP 실패"))
                .when(mailer).send(anyString(), anyString(), anyInt(), any());

        service.request("me@odolog.com");
        runScheduled();

        verify(tokenRepository).save(any());
    }

    @Test
    @DisplayName("요청 스레드는 횟수만 세고 조회·저장은 하지 않는다 — 응답 시간이 가입 여부를 알려주지 않게")
    void requestThreadDoesNoAccountWork() {
        service.request("me@odolog.com");

        // 가입 여부와 무관하게 같은 일만 하고 반환
        verify(rateLimiter).acquire(eq("password-reset:me@odolog.com"), any(), anyString());
        org.mockito.Mockito.verifyNoInteractions(userRepository, tokenRepository, mailer);
        assertThat(scheduled).hasSize(1);
    }

    @Test
    @DisplayName("동시 요청과 충돌해 발급이 실패하면 한 번 다시 시도한다 — 조용히 메일이 안 가는 것 방지")
    void retriesOnceOnConcurrencyFailure() {
        when(userRepository.findLockedByEmail("me@odolog.com")).thenReturn(Optional.of(user));
        when(tokenRepository.deleteByUserId(1L))
                .thenThrow(new CannotAcquireLockException("Record has changed since last read"))
                .thenReturn(0);

        service.request("me@odolog.com");
        runScheduled();

        verify(tokenRepository).save(any());
        verify(mailer).send(eq("me@odolog.com"), anyString(), anyInt(), any());
    }

    @Test
    @DisplayName("만료 토큰 정리가 실패해도 발급은 계속된다")
    void cleanupFailureDoesNotBlockIssue() {
        when(userRepository.findLockedByEmail("me@odolog.com")).thenReturn(Optional.of(user));
        when(tokenRepository.deleteByExpiresAtBefore(any()))
                .thenThrow(new CannotAcquireLockException("Record has changed since last read"));

        service.request("me@odolog.com");
        runScheduled();

        verify(tokenRepository).save(any());
    }

    @Test
    @DisplayName("대기 중인 발급 작업이 상한에 닿으면 더 받지 않는다 — 실행기 대기열이 끝없이 쌓이지 않게")
    void dropsRequestsBeyondPendingLimit() {
        for (int i = 0; i < PasswordResetService.MAX_PENDING + 5; i++) {
            service.request("flood" + i + "@odolog.com");
        }

        assertThat(scheduled).hasSize(PasswordResetService.MAX_PENDING);

        // 처리되면 자리가 다시 남
        runScheduled();
        service.request("me@odolog.com");
        assertThat(scheduled).hasSize(1);
    }

    private void runScheduled() {
        scheduled.forEach(Runnable::run);
        scheduled.clear();
    }
}
