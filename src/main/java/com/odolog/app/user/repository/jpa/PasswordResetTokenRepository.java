package com.odolog.app.user.repository.jpa;

import com.odolog.app.user.domain.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** 새로 발급하면 이전 것은 지운다. 메일함에 남은 옛 링크가 계속 살아 있으면 안 된다 */
    void deleteByUserId(Long userId);

    /**
     * 만료된 토큰 정리
     * 쓰지 않고 버려진 토큰은 아무도 안 지워서 계속 쌓인다 — 해시라 위험하지는 않지만
     * 지울 이유가 있는 행을 안 지우는 상태다
     */
    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
