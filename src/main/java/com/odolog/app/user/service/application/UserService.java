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

    /**
     * 되돌릴 수 없는 동작 앞에서 "지금 이 사람이 맞는가"를 다시 묻는다.
     * changePassword 와 탈퇴가 같은 관문을 쓰므로 여기 한 곳에만 둔다.
     */
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
        // 로그인되어 있다는 것만으로는 부족하다. 자리를 비운 사이 열린 세션을 누가 잡으면
        // 비밀번호를 바꿔 계정을 통째로 가져갈 수 있다.
        verifyPassword(userId, request.currentPassword());

        // findById 가 두 번 불리지만 쿼리는 한 번만 나간다. 같은 트랜잭션 안에서는
        // 영속성 컨텍스트(1차 캐시)가 같은 id 의 엔티티를 이미 들고 있기 때문이다.
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
            // 부분 수정에서 null 은 "안 보냄" 이라 "지움" 은 빈 문자열이 맡는다.
            // 그대로 저장하면 "없음" 이 null 과 "" 두 가지 모양으로 갈린다.
            String phone = request.phone().isBlank() ? null : request.phone();
            user.changePhone(phone);
        }

        return user;
    }
}
