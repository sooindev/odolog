package com.odolog.app.user.service.application;

import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.repository.PasswordResetTokenRepository;
import com.odolog.app.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 같은 주소의 재설정 발급을 실제 DB 에 동시에 여섯 번
 * 사용자 행을 잠그지 않으면 토큰 삭제·저장끼리 충돌(1020·데드락)해 대부분 메일 없이 끝났다
 * 이 클래스에 @Transactional 금지. 발급마다 따로 트랜잭션
 */
@SpringBootTest
class PasswordResetServiceTransactionTest {

    private static final int REQUESTS = 6;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void tearDown() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("같은 주소로 동시에 발급해도 충돌 없이 끝나고 토큰은 하나만 남는다")
    void concurrentIssuesForSameUser() throws Exception {
        userRepository.save(new User("reset@odolog.com", "encoded-pw", "재설정"));
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        CyclicBarrier start = new CyclicBarrier(REQUESTS);
        ExecutorService pool = Executors.newFixedThreadPool(REQUESTS);
        try {
            List<Future<?>> results = new ArrayList<>();
            for (int i = 0; i < REQUESTS; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    tx.executeWithoutResult(status -> passwordResetService.issue("reset@odolog.com"));
                    return null;
                }));
            }
            // 하나라도 충돌로 실패하면 여기서 예외
            for (Future<?> result : results) {
                result.get(60, TimeUnit.SECONDS);
            }

            // 재발급마다 이전 토큰 폐기. 마지막 하나만 유효
            assertThat(tokenRepository.count()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
