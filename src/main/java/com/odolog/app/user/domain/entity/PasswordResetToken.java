package com.odolog.app.user.domain.entity;

import com.odolog.app.common.domain.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * 비밀번호 재설정 토큰
 *
 * 원본이 아니라 해시를 저장한다. DB 가 새어도 그것만으로는 남의 비밀번호를 바꿀 수 없다 —
 * 비밀번호를 해시로 저장하는 것과 같은 이유다. 원본은 메일로 한 번 나가고 서버에 남지 않는다
 */
@Entity
@Table(
        name = "password_reset_tokens",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_password_reset_tokens_token_hash",
                columnNames = "token_hash"))
public class PasswordResetToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_password_reset_tokens_user"))
    private User user;

    /** SHA-256 을 16진수로. BCrypt 가 아닌 이유는 아래 verify 주석 참고 */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** 한 번 쓰면 못 쓴다. null 이면 아직 안 씀 */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    protected PasswordResetToken() {
    }

    public PasswordResetToken(User user, String tokenHash, LocalDateTime expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    /** 아직 쓸 수 있는 토큰인가 */
    public boolean isUsable(LocalDateTime now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public void markUsed(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }
}
