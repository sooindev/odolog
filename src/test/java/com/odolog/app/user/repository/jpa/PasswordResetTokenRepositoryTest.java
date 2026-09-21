package com.odolog.app.user.repository.jpa;

import com.odolog.app.common.config.jpa.JpaAuditingConfig;
import com.odolog.app.user.domain.entity.PasswordResetToken;
import com.odolog.app.user.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PasswordResetTokenRepositoryTest {

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        user = entityManager.persist(new User("me@odolog.com", "hash", "닉네임", null));
    }

    private PasswordResetToken token(String hash) {
        return new PasswordResetToken(user, hash, LocalDateTime.now().plusMinutes(30));
    }

    @Test
    @DisplayName("해시로 토큰을 찾는다")
    void findsByTokenHash() {
        passwordResetTokenRepository.save(token("abc123"));
        entityManager.flush();
        entityManager.clear();

        Optional<PasswordResetToken> found = passwordResetTokenRepository.findByTokenHash("abc123");

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("같은 해시를 두 번 저장할 수 없다")
    void rejectsDuplicateHash() {
        // 제약이 없으면 findByTokenHash 가 2건을 만나 예외가 난다
        passwordResetTokenRepository.save(token("abc123"));
        entityManager.flush();

        // IDENTITY 라 save() 시점에 바로 INSERT 된다 — 예외도 거기서 난다
        assertThatThrownBy(() -> {
            passwordResetTokenRepository.save(token("abc123"));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("사용자의 토큰을 한 번에 지운다")
    void deletesAllTokensOfUser() {
        // 새로 발급할 때와 탈퇴할 때 둘 다 쓴다
        passwordResetTokenRepository.save(token("aaa"));
        passwordResetTokenRepository.save(token("bbb"));
        entityManager.flush();

        passwordResetTokenRepository.deleteByUserId(user.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(passwordResetTokenRepository.findByTokenHash("aaa")).isEmpty();
        assertThat(passwordResetTokenRepository.findByTokenHash("bbb")).isEmpty();
    }

    @Test
    @DisplayName("만료·사용 여부는 엔티티가 판단한다")
    void decidesUsability() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 21, 12, 0);
        PasswordResetToken fresh = new PasswordResetToken(user, "ccc", now.plusMinutes(10));

        assertThat(fresh.isUsable(now)).isTrue();
        assertThat(fresh.isUsable(now.plusMinutes(11))).isFalse();

        fresh.markUsed(now);
        assertThat(fresh.isUsable(now)).isFalse();
    }
}
