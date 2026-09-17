package com.odolog.app.user.service.application;

import com.odolog.app.common.exception.type.ConflictException;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.dto.request.login.LoginRequest;
import com.odolog.app.user.dto.request.password.ChangePasswordRequest;
import com.odolog.app.user.dto.request.signup.SignUpRequest;
import com.odolog.app.user.dto.request.profile.UpdateProfileRequest;
import com.odolog.app.common.exception.type.AuthenticationFailedException;
import com.odolog.app.user.repository.jpa.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("이미 가입된 이메일입니다: " + request.email());
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.email(), encodedPassword, request.nickname(), request.phone());

        return userRepository.save(user);
    }

    public User login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return user;
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
        userRepository.delete(findById(userId));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        // 로그인 상태만으로는 부족. 열린 세션을 잡은 사람이 계정을 가져갈 수 있음
        verifyPassword(userId, request.currentPassword());

        // findById 가 두 번이지만 쿼리는 한 번 — 같은 트랜잭션의 1차 캐시
        User user = findById(userId);
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
