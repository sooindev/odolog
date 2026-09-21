package com.odolog.app.user.repository.jpa;

import com.odolog.app.user.domain.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** 새로 발급하면 이전 것은 지운다. 메일함에 남은 옛 링크가 계속 살아 있으면 안 된다 */
    void deleteByUserId(Long userId);
}
