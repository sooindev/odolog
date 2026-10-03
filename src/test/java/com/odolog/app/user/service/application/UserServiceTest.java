package com.odolog.app.user.service.application;

import com.odolog.app.common.exception.type.ConflictException;
import com.odolog.app.common.exception.type.AuthenticationFailedException;
import com.odolog.app.common.auth.LoginAttemptLimiter;
import com.odolog.app.common.exception.type.InvalidRequestException;
import com.odolog.app.common.exception.type.TooManyRequestsException;
import com.odolog.app.common.exception.ErrorCode;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.domain.type.Language;
import com.odolog.app.user.domain.type.UnitSystem;
import com.odolog.app.user.repository.PasswordResetTokenRepository;
import com.odolog.app.user.dto.request.LoginRequest;
import com.odolog.app.user.dto.request.password.ChangePasswordRequest;
import com.odolog.app.user.dto.request.SignUpRequest;
import com.odolog.app.user.dto.request.UpdateProfileRequest;
import com.odolog.app.user.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private LoginAttemptLimiter loginAttemptLimiter;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("새 비밀번호가 현재와 같으면 막는다 — '바꿨다' 는 안내만 뜨고 아무것도 안 바뀐다")
    void rejectsUnchangedPassword() {
        User user = new User("me@odolog.com", new BCryptPasswordEncoder().encode("password1234"),
                "나");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findLockedById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.changePassword(1L,
                new ChangePasswordRequest("password1234", "password1234")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("로그인 시도는 login: 접두사 키로 센다 — 이메일 칸에 다른 용도의 키를 넣어 남을 잠그지 못하게")
    void loginUsesItsOwnLimiterKey() {
        when(userRepository.findByEmail("password-check:1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(new LoginRequest("password-check:1", "x")))
                .isInstanceOf(AuthenticationFailedException.class);

        // 비밀번호 확인 키(password-check:1)와 겹치지 않음
        verify(loginAttemptLimiter).acquire(eq("login:password-check:1"), eq(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS), any());
        verify(loginAttemptLimiter, never()).acquire(eq("password-check:1"), any(), any());
    }

    @Test
    @DisplayName("회원가입 시 비밀번호는 암호화되어 저장된다")
    void signUpEncodesPassword() {
        SignUpRequest request = new SignUpRequest("test@odolog.com", "password1234", "닉네임", null, null, null, null);
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.signUp(request);

        assertThat(saved.getPassword()).isNotEqualTo("password1234");
        assertThat(new BCryptPasswordEncoder().matches("password1234", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("가입 때 보낸 설정으로 시작한다 — 미국 사용자가 Asia/Seoul 로 시작하지 않게")
    void signUpWithSettings() {
        SignUpRequest request = new SignUpRequest("test@odolog.com", "password1234", "닉네임",
                Language.EN, "America/Chicago", "USD", UnitSystem.MPG_US);
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.signUp(request);

        assertThat(saved.getLanguage()).isEqualTo(Language.EN);
        assertThat(saved.getTimeZone()).isEqualTo("America/Chicago");
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(saved.getUnitSystem()).isEqualTo(UnitSystem.MPG_US);
    }

    @Test
    @DisplayName("가입 때 없는 시간대를 보내면 400 이고 저장하지 않는다")
    void signUpUnknownTimeZone() {
        SignUpRequest request = new SignUpRequest("test@odolog.com", "password1234", "닉네임",
                null, "Mars/Olympus", null, null);
        when(userRepository.existsByEmail(request.email())).thenReturn(false);

        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(InvalidRequestException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 예외가 발생하고 저장하지 않는다")
    void signUpDuplicateEmail() {
        SignUpRequest request = new SignUpRequest("test@odolog.com", "password1234", "닉네임", null, null, null, null);
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("로그인 성공")
    void loginSuccess() {
        String encoded = new BCryptPasswordEncoder().encode("password1234");
        User user = new User("test@odolog.com", encoded, "닉네임");
        when(userRepository.findByEmail("test@odolog.com")).thenReturn(Optional.of(user));

        User result = userService.login(new LoginRequest("test@odolog.com", "password1234"));

        assertThat(result).isEqualTo(user);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인하면 인증 실패")
    void loginEmailNotFound() {
        when(userRepository.findByEmail("nobody@odolog.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(new LoginRequest("nobody@odolog.com", "password1234")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 인증 실패")
    void loginWrongPassword() {
        String encoded = new BCryptPasswordEncoder().encode("password1234");
        User user = new User("test@odolog.com", encoded, "닉네임");
        when(userRepository.findByEmail("test@odolog.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login(new LoginRequest("test@odolog.com", "wrongpassword")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("닉네임만 보내면 설정은 그대로 유지된다")
    void updateProfilePartial() {
        User user = new User("test@odolog.com", "encoded", "기존닉네임");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.updateProfile(1L, new UpdateProfileRequest("새닉네임", null, null, null, null));

        assertThat(result.getNickname()).isEqualTo("새닉네임");
        assertThat(result.getTimeZone()).isEqualTo("Asia/Seoul");
    }

    @Test
    @DisplayName("설정만 보내면 설정만 바뀌고 닉네임은 그대로")
    void updateProfileSettingsOnly() {
        User user = new User("test@odolog.com", "encoded", "닉네임");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.updateProfile(1L, new UpdateProfileRequest(
                null, Language.EN, "America/New_York", "USD", UnitSystem.MPG_US));

        assertThat(result.getLanguage()).isEqualTo(Language.EN);
        assertThat(result.getTimeZone()).isEqualTo("America/New_York");
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getUnitSystem()).isEqualTo(UnitSystem.MPG_US);
        assertThat(result.getNickname()).isEqualTo("닉네임");
    }

    @Test
    @DisplayName("현재 비밀번호가 맞으면 새 비밀번호가 암호화되어 저장된다")
    void changePasswordSuccess() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User user = new User("test@odolog.com", encoder.encode("oldpassword"), "닉네임");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findLockedById(1L)).thenReturn(Optional.of(user));

        userService.changePassword(1L, new ChangePasswordRequest("oldpassword", "newpassword1234"));

        assertThat(user.getPassword()).isNotEqualTo("newpassword1234");
        assertThat(encoder.matches("newpassword1234", user.getPassword())).isTrue();
        assertThat(encoder.matches("oldpassword", user.getPassword())).isFalse();
        // 발급돼 있던 재설정 링크도 폐기
        verify(passwordResetTokenRepository).deleteByUserId(1L);
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 401이고 비밀번호는 그대로다")
    void changePasswordWrongCurrentFails() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String original = encoder.encode("oldpassword");
        User user = new User("test@odolog.com", original, "닉네임");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findLockedById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.changePassword(1L,
                new ChangePasswordRequest("wrongpassword", "newpassword1234")))
                .isInstanceOf(AuthenticationFailedException.class);

        assertThat(user.getPassword()).isEqualTo(original);
        // 시도는 사용자 단위로 집계, 틀렸으니 지우지 않음
        verify(loginAttemptLimiter).acquire(eq("password-check:1"), eq(ErrorCode.TOO_MANY_PASSWORD_ATTEMPTS), any());
        verify(loginAttemptLimiter, never()).recordSuccess("password-check:1");
    }

    @Test
    @DisplayName("비밀번호 확인이 잠겨 있으면 맞는 비밀번호도 429 — 훔친 세션의 무제한 대입 방지")
    void verifyPasswordLocked() {
        doThrow(new TooManyRequestsException(ErrorCode.TOO_MANY_PASSWORD_ATTEMPTS, "잠김", 10))
                .when(loginAttemptLimiter)
                .acquire(eq("password-check:1"), eq(ErrorCode.TOO_MANY_PASSWORD_ATTEMPTS), any());

        assertThatThrownBy(() -> userService.verifyPassword(1L, "oldpassword"))
                .isInstanceOf(TooManyRequestsException.class);

        // 잠긴 동안은 비밀번호 비교까지 가지 않음
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("전각 공백만 적은 닉네임은 400 — strip 뒤 빈 문자열이 저장되지 않게")
    void signUpFullWidthSpaceNickname() {
        assertThatThrownBy(() -> userService.signUp(new SignUpRequest(
                "new@odolog.com", "password1234", "\u3000", null, null, null, null)))
                .isInstanceOf(InvalidRequestException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("없는 이메일도 있는 이메일과 비슷한 시간이 걸린다 — 응답 시간으로 가입 여부를 알 수 없게")
    void unknownEmailTakesAsLongAsWrongPassword() {
        User user = new User("test@odolog.com", new BCryptPasswordEncoder().encode("password1234"), "나");
        when(userRepository.findByEmail("test@odolog.com")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("nobody@odolog.com")).thenReturn(Optional.empty());

        long wrongPassword = fastestLogin("test@odolog.com");
        long unknownEmail = fastestLogin("nobody@odolog.com");

        // BCrypt 생략 시 수백 배 차이. 절반 기준은 부하가 있어도 여유
        assertThat(unknownEmail).isGreaterThan(wrongPassword / 2);
    }

    /** 5회 중 최솟값(ns). 평균은 GC·JIT 에 흔들림 */
    private long fastestLogin(String email) {
        long fastest = Long.MAX_VALUE;
        for (int i = 0; i < 5; i++) {
            long start = System.nanoTime();
            try {
                userService.login(new LoginRequest(email, "wrongpassword"));
            } catch (AuthenticationFailedException expected) {
                // 둘 다 실패가 정상. 시간만 측정
            }
            fastest = Math.min(fastest, System.nanoTime() - start);
        }
        return fastest;
    }
}
