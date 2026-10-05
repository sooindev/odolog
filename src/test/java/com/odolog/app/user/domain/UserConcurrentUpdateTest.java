package com.odolog.app.user.domain;

import com.odolog.app.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 프로필 저장과 비밀번호 변경이 겹칠 때. 늦게 커밋한 프로필 저장이 옛 해시를 다시 쓰면 안 됨
 * User 에 @Version 이 없어도 MariaDB 의 innodb_snapshot_isolation 이 늦은 쪽을 거절해서 막힘
 * 이 클래스에 @Transactional 금지. 두 트랜잭션을 실제로 겹쳐야 함
 */
@SpringBootTest
class UserConcurrentUpdateTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("먼저 읽고 늦게 커밋한 닉네임 변경은 거절되고, 그 사이 바뀐 비밀번호가 남는다")
    void profileSaveKeepsConcurrentPasswordChange() {
        Long id = userRepository.save(new User("dynamic@odolog.com", "old-hash", "처음")).getId();

        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        TransactionTemplate inner = new TransactionTemplate(transactionManager);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // 409 CONCURRENT_UPDATE 로 번역되는 예외
        assertThatThrownBy(() -> outer.executeWithoutResult(status -> {
            // 프로필 저장: 옛 해시를 든 채로 읽음
            User profile = userRepository.findById(id).orElseThrow();

            // 그 사이 다른 요청이 비밀번호 변경 커밋
            inner.executeWithoutResult(s ->
                    userRepository.findById(id).orElseThrow().changePassword("new-hash"));

            profile.changeNickname("바꿈");
        })).isInstanceOf(ConcurrencyFailureException.class);

        User saved = userRepository.findById(id).orElseThrow();
        assertThat(saved.getPassword()).isEqualTo("new-hash");
        assertThat(saved.getNickname()).isEqualTo("처음");
    }
}
