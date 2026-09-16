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

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        User user = userService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        User user = userService.login(request);

        HttpSession session = httpRequest.getSession();
        session.setAttribute(SessionConst.LOGIN_USER_ID, user.getId());
        httpRequest.changeSessionId();

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

    // 204. 돌려줄 것이 없다 — UserResponse 를 주면 비밀번호와 무관한 값만 다시 보내는 꼴이고,
    // 비밀번호 자체는 어떤 경우에도 응답에 담지 않는다.
    //
    // 세션은 그대로 둔다. 본인이 바꾼 것이라 다시 로그인시킬 이유가 없다.
    // (다른 기기의 세션까지 끊으려면 세션 저장소를 따로 둬야 한다 — 지금 범위 밖)
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                 @LoginUser Long userId) {
        userService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@LoginUser Long userId,
                                                  @Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.updateProfile(userId, request);
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
