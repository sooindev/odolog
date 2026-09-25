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

    /** 로그인·재설정과 같은 리미터를 키만 갈라 쓴다 */
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
     * 가입은 이미 가입된 주소에 409 를 준다 — 즉 주소 하나로 가입 여부를 물을 수 있다.
     * 그 응답을 없애지 않기로 했으므로(가입하려는 사람에게는 이게 맞는 안내다)
     * 대신 **한 곳에서 주소를 쓸어 보는 것**을 막는다
     *
     * 이메일이 아니라 IP 로 세는 이유: 이메일로 세면 매번 다른 주소를 넣는 열거자는
     * 카운터가 늘 1 이라 그냥 빠져나간다. 여기서 반복되는 것은 보내는 쪽이다
     *
     * 성공한 가입도 센다. 409 만 세면 아직 없는 주소를 찔러 보는 쪽은 안 걸리고,
     * 그쪽은 계정을 실제로 만들어 버리므로 더 나쁘다
     *
     * 서비스가 아니라 컨트롤러에 둔 이유: IP 는 웹 계층의 사정이고,
     * UserService.signUp 이 그걸 알면 테스트마다 가짜 IP 를 넘겨야 한다
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

    /**
     * X-Forwarded-For 를 읽지 않는다. 그 헤더는 보내는 쪽이 마음대로 적을 수 있어서,
     * 앞에 그 값을 덮어써 주는 신뢰할 수 있는 프록시가 있을 때만 뜻이 있다.
     * 지금은 그런 프록시가 없으므로 읽으면 제한을 헤더 한 줄로 빠져나가게 만들 뿐이다.
     * 프록시 뒤에 두는 날 server.forward-headers-strategy 와 함께 다시 본다
     */
    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        User user = userService.login(request);

        HttpSession session = httpRequest.getSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, user.getId());
        httpRequest.changeSessionId();
        // 비밀번호가 바뀌면 이 목록으로 다른 기기의 세션을 끊는다
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

    // 204. 돌려줄 것이 없고 비밀번호는 어떤 경우에도 응답에 안 담음
    // 이 세션은 유지 — 본인이 바꾼 것이라 다시 로그인시킬 이유가 없음
    // 다른 기기의 세션은 끊는다 — 비밀번호를 바꾸는 흔한 이유가 "누가 쓰고 있는 것 같아서"다
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
