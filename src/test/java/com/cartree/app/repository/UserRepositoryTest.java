package com.cartree.app.repository;

import com.cartree.app.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
        User user = new User("a@cartree.com", "encoded-pw", "차주A", "010-1111-2222");

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    @DisplayName("findByEmail 은 이메일로 사용자를 찾고, 없으면 빈 Optional 을 준다")
    void findByEmail() {
        em.persist(new User("b@cartree.com", "encoded-pw", "차주B", null));
        em.flush();
        em.clear();

        Optional<User> found = userRepository.findByEmail("b@cartree.com");
        Optional<User> notFound = userRepository.findByEmail("none@cartree.com");

        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("차주B");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail 은 가입 여부만 boolean 으로 알려준다")
    void existsByEmail() {
        em.persist(new User("c@cartree.com", "encoded-pw", "차주C", null));
        em.flush();

        assertThat(userRepository.existsByEmail("c@cartree.com")).isTrue();
        assertThat(userRepository.existsByEmail("none@cartree.com")).isFalse();
    }
}
