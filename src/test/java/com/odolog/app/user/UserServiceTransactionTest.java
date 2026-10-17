package com.odolog.app.user;

import com.odolog.app.user.domain.User;
import com.odolog.app.user.dto.request.ChangePasswordRequest;
import com.odolog.app.user.dto.request.SignUpRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * BCrypt 가 트랜잭션 밖에서 도는지. 안에서 돌면 수십 ms 동안 커넥션(풀 10)·사용자 행 잠금을 쥠
 * Mockito 단위 테스트는 스프링 프록시를 안 거쳐 @Transactional 이 적용되지 않아 여기서 봄
 * 이 클래스에 @Transactional 금지. 서비스 설정이 무시됨
 */
@SpringBootTest
class UserServiceTransactionTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @MockitoSpyBean
    private PasswordEncoder passwordEncoder;

    /** BCrypt 를 부를 때마다 그 순간 트랜잭션이 열려 있었는지 */
    private final List<Boolean> inTransaction = new ArrayList<>();

    @BeforeEach
    void recordTransactionAtBcrypt() {
        doAnswer(invocation -> {
            inTransaction.add(TransactionSynchronizationManager.isActualTransactionActive());
            return invocation.callRealMethod();
        }).when(passwordEncoder).encode(any());
        doAnswer(invocation -> {
            inTransaction.add(TransactionSynchronizationManager.isActualTransactionActive());
            return invocation.callRealMethod();
        }).when(passwordEncoder).matches(any(), any());
    }

    @AfterEach
    void tearDown() {
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("회원가입의 BCrypt 는 트랜잭션 밖이다")
    void signUpHashesOutsideTransaction() {
        userService.signUp(new SignUpRequest("signup@odolog.com", "password1234", "가입자", null, null, null, null));

        assertThat(inTransaction).containsExactly(false);
        assertThat(userRepository.findByEmail("signup@odolog.com")).isPresent();
    }

    @Test
    @DisplayName("비밀번호 변경의 BCrypt 셋은 트랜잭션 밖이다")
    void changePasswordHashesOutsideTransaction() {
        User user = userService.signUp(new SignUpRequest("change@odolog.com", "password1234", "변경자", null, null, null, null));
        inTransaction.clear();

        userService.changePassword(user.getId(), new ChangePasswordRequest("password1234", "newpassword1234"));

        assertThat(inTransaction).containsExactly(false, false, false);
    }
}
