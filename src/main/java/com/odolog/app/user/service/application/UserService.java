package com.odolog.app.user.service.application;

import com.odolog.app.common.exception.type.ConflictException;
import com.odolog.app.common.exception.type.InvalidRequestException;
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
        // 빈 전화번호는 null 로. updateProfile 과 같은 규칙 — 화면을 안 거친 요청도 같은 모양으로 저장
        String phone = (request.phone() == null || request.phone().isBlank()) ? null : request.phone();
        User user = new User(request.email(), encodedPassword, request.nickname(), phone);

        return userRepository.save(user);
    }

    public User login(LoginRequest request) {
        // 검증보다 먼저. 잠긴 동안에는 비밀번호를 맞혀도 들여보내지 않는다
        loginAttemptLimiter.checkNotLocked(request.email(), "로그인 시도가 너무 많습니다.");

        try {
            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다."));

            if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                throw new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다.");
            }

            loginAttemptLimiter.recordSuccess(request.email());
            return user;
        } catch (AuthenticationFailedException e) {
            // 없는 계정도 센다 — 존재하는 이메일에서만 잠기면 그게 곧 존재 여부 신호다
            loginAttemptLimiter.recordFailure(request.email());
            throw e;
        }
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 사용자입니다: " + userId));
    }

    /** 되돌릴 수 없는 동작 앞의 관문. 비밀번호 변경과 탈퇴가 공유 */
    public void verifyPassword(Long userId, String rawPassword) {
        User user = findById(userId);

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new AuthenticationFailedException("현재 비밀번호가 올바르지 않습니다.");
        }
    }

    @Transactional
    public void delete(Long userId) {
        // 재설정 토큰이 사용자를 참조한다. 안 지우면 FK 제약 위반
        // 조율 층이 아니라 여기서 하는 이유는 토큰이 user 기능 안의 사정이기 때문
        passwordResetTokenRepository.deleteByUserId(userId);
        userRepository.delete(findById(userId));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        // 로그인 상태만으로는 부족. 열린 세션을 잡은 사람이 계정을 가져갈 수 있음
        verifyPassword(userId, request.currentPassword());

        // findById 가 두 번이지만 쿼리는 한 번 — 같은 트랜잭션의 1차 캐시
        User user = findById(userId);

        /*
         * 같은 값이면 막는다. 안 막으면 "바꿨다" 는 안내가 뜨는데 아무것도 안 바뀐다 —
         * 비밀번호가 샜다고 생각해 바꾸러 온 사람이 안 바뀐 채로 안심하고 나간다.
         * 재설정(PasswordResetService)에는 두지 않았다. 그쪽은 옛 비밀번호를 모르는 사람이라
         * "같습니다" 라는 말이 도움이 안 된다
         */
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new InvalidRequestException("새 비밀번호가 현재 비밀번호와 같습니다.");
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findById(userId);

        if (request.nickname() != null) {
            user.changeNickname(request.nickname());
        }
        if (request.phone() != null) {
            // null 은 "안 보냄", 빈 문자열이 "지움". 그대로 저장하면 "없음" 이 두 모양이 됨
            String phone = request.phone().isBlank() ? null : request.phone();
            user.changePhone(phone);
        }

        return user;
    }
}
