package com.odolog.app.user.service.application;

import com.odolog.app.common.exception.type.ConflictException;
import com.odolog.app.common.exception.type.InvalidRequestException;
import com.odolog.app.common.text.InputText;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.dto.request.login.LoginRequest;
import com.odolog.app.user.dto.request.password.ChangePasswordRequest;
import com.odolog.app.user.dto.request.signup.SignUpRequest;
import com.odolog.app.user.dto.request.profile.UpdateProfileRequest;
import com.odolog.app.common.auth.ratelimit.LoginAttemptLimiter;
import com.odolog.app.common.exception.type.AuthenticationFailedException;
import com.odolog.app.user.repository.jpa.PasswordResetTokenRepository;
import com.odolog.app.user.repository.jpa.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 없는 계정의 비교 상대. 같은 인코더로 생성해 비용 동일 */
    private final String dummyHash = passwordEncoder.encode("no-such-account");

    public UserService(UserRepository userRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       LoginAttemptLimiter loginAttemptLimiter) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.loginAttemptLimiter = loginAttemptLimiter;
    }

    @Transactional
    public User signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("이미 가입된 이메일입니다: " + request.email());
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        // 빈 전화번호는 null
        String phone = (request.phone() == null || request.phone().isBlank()) ? null : request.phone();
        User user = new User(request.email(), encodedPassword, InputText.strip(request.nickname()), phone);

        return userRepository.save(user);
    }

    public User login(LoginRequest request) {
        // 검증보다 먼저. 잠긴 동안은 맞는 비밀번호도 거절
        loginAttemptLimiter.checkNotLocked(request.email(), "로그인 시도가 너무 많습니다.");

        try {
            User user = userRepository.findByEmail(request.email()).orElse(null);

            // 없는 계정도 BCrypt 한 번. 응답 시간으로 가입 여부가 드러나는 것 방지
            String hash = (user == null) ? dummyHash : user.getPassword();
            boolean matches = passwordEncoder.matches(request.password(), hash);

            if (user == null || !matches) {
                throw new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다.");
            }

            loginAttemptLimiter.recordSuccess(request.email());
            return user;
        } catch (AuthenticationFailedException e) {
            // 없는 계정도 실패 집계
            loginAttemptLimiter.recordFailure(request.email());
            throw e;
        }
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 사용자입니다: " + userId));
    }

    /** 되돌릴 수 없는 동작 앞의 비밀번호 확인. 변경·탈퇴 공용 */
    public void verifyPassword(Long userId, String rawPassword) {
        User user = findById(userId);

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new AuthenticationFailedException("현재 비밀번호가 올바르지 않습니다.");
        }
    }

    @Transactional
    public void delete(Long userId) {
        // 재설정 토큰 먼저 삭제. FK 제약
        passwordResetTokenRepository.deleteByUserId(userId);
        userRepository.delete(findById(userId));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        // 현재 비밀번호 확인. 열린 세션만으로는 변경 불가
        verifyPassword(userId, request.currentPassword());

        // 같은 트랜잭션 1차 캐시라 쿼리 1번
        User user = findById(userId);

        // 같은 비밀번호로 변경 거부. 바뀌지 않았는데 바뀐 것처럼 보이는 문제 방지
        // 재설정에는 미적용(옛 비밀번호를 모르는 사용자)
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new InvalidRequestException("새 비밀번호가 현재 비밀번호와 같습니다.");
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findById(userId);

        if (request.nickname() != null) {
            user.changeNickname(InputText.strip(request.nickname()));
        }
        if (request.phone() != null) {
            // null 은 안 보냄, 빈 문자열은 지움
            String phone = request.phone().isBlank() ? null : request.phone();
            user.changePhone(phone);
        }

        return user;
    }
}
