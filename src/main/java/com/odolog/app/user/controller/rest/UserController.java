package com.odolog.app.user.controller.rest;

import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.dto.request.login.LoginRequest;
import com.odolog.app.user.dto.request.password.ChangePasswordRequest;
import com.odolog.app.user.dto.request.signup.SignUpRequest;
import com.odolog.app.user.dto.request.profile.UpdateProfileRequest;
import com.odolog.app.user.dto.response.profile.UserResponse;
import com.odolog.app.user.service.application.UserService;
import com.odolog.app.common.auth.annotation.LoginUser;
import com.odolog.app.common.auth.constant.SessionConst;
import com.odolog.app.common.auth.ratelimit.LoginAttemptLimiter;
import com.odolog.app.common.auth.session.LoginSessionRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    /** 로그인·재설정과 공용 리미터, 키만 구분 */
    private static final String SIGNUP_KEY_PREFIX = "signup:";

    private final UserService userService;
    private final LoginAttemptLimiter attemptLimiter;
    private final LoginSessionRegistry sessionRegistry;

    public UserController(UserService userService, LoginAttemptLimiter attemptLimiter,
                          LoginSessionRegistry sessionRegistry) {
        this.userService = userService;
        this.attemptLimiter = attemptLimiter;
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * 가입 409 는 유지, 대신 한 곳에서의 주소 대량 조회 차단
     * IP 기준 집계. 이메일 기준이면 매번 다른 주소로 우회
     * 성공한 가입도 집계. 없는 주소 탐색 + 계정 생성 방지
     */
    @PostMapping
    public ResponseEntity<UserResponse> signUp(@Valid @RequestBody SignUpRequest request,
                                                 HttpServletRequest httpRequest) {
        String limitKey = SIGNUP_KEY_PREFIX + clientIp(httpRequest);
        attemptLimiter.checkNotLocked(limitKey, "회원가입 시도가 너무 많습니다.");
        attemptLimiter.recordFailure(limitKey);

        User user = userService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    /** X-Forwarded-For 미사용. 신뢰할 프록시가 없어 헤더 조작으로 우회 가능 */
    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        User user = userService.login(request);

        HttpSession session = httpRequest.getSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, user.getId());
        httpRequest.changeSessionId();
        // 비밀번호 변경 시 다른 기기 세션 종료용 등록
        sessionRegistry.register(user.getId(), session);

        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@LoginUser Long userId) {
        User user = userService.findById(userId);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    // 204. 현재 세션 유지, 다른 기기 세션 종료
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                 @LoginUser Long userId,
                                                 HttpServletRequest httpRequest) {
        userService.changePassword(userId, request);
        sessionRegistry.invalidateOthers(userId, httpRequest.getSession(false));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@LoginUser Long userId,
                                                  @Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.updateProfile(userId, request);
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
