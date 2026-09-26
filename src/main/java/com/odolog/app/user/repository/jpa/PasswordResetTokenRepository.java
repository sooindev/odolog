package com.odolog.app.user.repository.jpa;

import com.odolog.app.user.domain.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** 재발급·탈퇴 시 기존 토큰 삭제 */
    void deleteByUserId(Long userId);

    /** 만료 토큰 정리 */
    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
