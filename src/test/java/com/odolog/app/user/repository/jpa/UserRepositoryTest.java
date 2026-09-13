package com.odolog.app.user.repository.jpa;

import com.odolog.app.user.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("사용자를 저장하면 id와 createdAt이 채워진다")
    void save() {
        User user = new User("a@odolog.com", "encoded-pw", "차주A", "010-1111-2222");

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    @DisplayName("findByEmail 은 이메일로 사용자를 찾고, 없으면 빈 Optional 을 준다")
    void findByEmail() {
        em.persist(new User("b@odolog.com", "encoded-pw", "차주B", null));
        em.flush();
        em.clear();

        Optional<User> found = userRepository.findByEmail("b@odolog.com");
        Optional<User> notFound = userRepository.findByEmail("none@odolog.com");

        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("차주B");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail 은 가입 여부만 boolean 으로 알려준다")
    void existsByEmail() {
        em.persist(new User("c@odolog.com", "encoded-pw", "차주C", null));
        em.flush();

        assertThat(userRepository.existsByEmail("c@odolog.com")).isTrue();
        assertThat(userRepository.existsByEmail("none@odolog.com")).isFalse();
    }

    @Test
    @DisplayName("같은 이메일을 두 번 저장하면 유니크 제약이 막는다")
    void duplicateEmailHitsUniqueConstraint() {
        userRepository.saveAndFlush(new User("dup@odolog.com", "encoded-pw", "차주A", null));

        // GlobalExceptionHandler 가 이 예외의 "모양"에 기대어 409 를 판정한다.
        // (DataIntegrityViolationException 의 cause 가 Hibernate ConstraintViolationException 이고,
        //  그 kind 가 UNIQUE) 그래서 그 가정을 여기서 못박아 둔다.
        assertThatThrownBy(() -> userRepository.saveAndFlush(
                new User("dup@odolog.com", "encoded-pw", "차주B", null)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .cause()
                .isInstanceOf(ConstraintViolationException.class)
                .extracting(cause -> ((ConstraintViolationException) cause).getKind())
                .isEqualTo(ConstraintViolationException.ConstraintKind.UNIQUE);
    }
}
