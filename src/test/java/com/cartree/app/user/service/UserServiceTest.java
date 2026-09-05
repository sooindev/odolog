package com.cartree.app.user.service;

import com.cartree.app.common.exception.AuthenticationFailedException;
import com.cartree.app.user.domain.User;
import com.cartree.app.user.dto.LoginRequest;
import com.cartree.app.user.dto.SignUpRequest;
import com.cartree.app.user.dto.UpdateProfileRequest;
import com.cartree.app.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원가입 시 비밀번호는 암호화되어 저장된다")
    void signUpEncodesPassword() {
        SignUpRequest request = new SignUpRequest("test@cartree.com", "password1234", "닉네임", "010-0000-0000");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.signUp(request);

        assertThat(saved.getPassword()).isNotEqualTo("password1234");
        assertThat(new BCryptPasswordEncoder().matches("password1234", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 예외가 발생하고 저장하지 않는다")
    void signUpDuplicateEmail() {
        SignUpRequest request = new SignUpRequest("test@cartree.com", "password1234", "닉네임", "010-0000-0000");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("로그인 성공")
    void loginSuccess() {
        String encoded = new BCryptPasswordEncoder().encode("password1234");
        User user = new User("test@cartree.com", encoded, "닉네임", "010-0000-0000");
        when(userRepository.findByEmail("test@cartree.com")).thenReturn(Optional.of(user));

        User result = userService.login(new LoginRequest("test@cartree.com", "password1234"));

        assertThat(result).isEqualTo(user);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인하면 인증 실패")
    void loginEmailNotFound() {
        when(userRepository.findByEmail("nobody@cartree.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(new LoginRequest("nobody@cartree.com", "password1234")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 인증 실패")
    void loginWrongPassword() {
        String encoded = new BCryptPasswordEncoder().encode("password1234");
        User user = new User("test@cartree.com", encoded, "닉네임", "010-0000-0000");
        when(userRepository.findByEmail("test@cartree.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login(new LoginRequest("test@cartree.com", "wrongpassword")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("닉네임만 보내면 전화번호는 그대로 유지된다")
    void updateProfilePartial() {
        User user = new User("test@cartree.com", "encoded", "기존닉네임", "010-0000-0000");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.updateProfile(1L, new UpdateProfileRequest("새닉네임", null));

        assertThat(result.getNickname()).isEqualTo("새닉네임");
        assertThat(result.getPhone()).isEqualTo("010-0000-0000");
    }
}
