package com.odolog.app.user;

import com.odolog.app.user.domain.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /** 행 잠금. 같은 토큰의 동시 사용 시 둘 다 유효로 판정되는 것 방지 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * 아직 쓸 수 있는 토큰의 주인. 잠금 없이. 재설정 확정이 사용자 행을 먼저 잠그려고 씀
     * 쓴 토큰·만료 토큰은 여기서 거름. 거르지 않으면 쓴 토큰 하나로 BCrypt 를 횟수 제한 없이 돌림
     */
    @Query("select t.user.id from PasswordResetToken t"
            + " where t.tokenHash = :tokenHash and t.usedAt is null and t.expiresAt > :now")
    Optional<Long> findUsableOwnerId(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

    /**
     * 재발급·탈퇴 시 기존 토큰 삭제
     * DELETE 한 문장. 메서드 이름만 쓰면 읽은 뒤 한 줄씩 지워, 동시 요청이 같은 행을 지울 때 충돌
     */
    @Modifying
    @Query("delete from PasswordResetToken t where t.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    /** 만료 토큰 정리. 위와 같은 이유로 한 문장 */
    @Modifying
    @Query("delete from PasswordResetToken t where t.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
