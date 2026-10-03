package com.odolog.app.user.repository;

import com.odolog.app.user.domain.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /** 행 잠금. 같은 토큰의 동시 사용 시 둘 다 유효로 판정되는 것 방지 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** 재발급·탈퇴 시 기존 토큰 삭제 */
    void deleteByUserId(Long userId);

    /** 만료 토큰 정리 */
    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
