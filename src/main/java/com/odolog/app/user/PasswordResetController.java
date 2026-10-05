package com.odolog.app.user;

import com.odolog.app.common.auth.ClientIp;
import com.odolog.app.common.auth.LoginAttemptLimiter;
import com.odolog.app.common.exception.ErrorCode;
import com.odolog.app.user.dto.request.PasswordResetConfirmRequest;
import com.odolog.app.user.dto.request.PasswordResetRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 비로그인 사용자의 쓰기 엔드포인트. 로그인 사용자용 UserController 와 분리 */
@RestController
@RequestMapping("/api/users/password-reset")
public class PasswordResetController {

    /** 주소마다 다른 대량 요청 차단. 주소별 키(password-reset:)만으로는 매번 새 키라 못 막음 */
    private static final String IP_KEY_PREFIX = "password-reset-ip:";

    private final PasswordResetService passwordResetService;
    private final LoginAttemptLimiter attemptLimiter;

    public PasswordResetController(PasswordResetService passwordResetService, LoginAttemptLimiter attemptLimiter) {
        this.passwordResetService = passwordResetService;
        this.attemptLimiter = attemptLimiter;
    }

    /** 가입 여부와 무관하게 204. 한도는 주소와 무관하게 같아 가입 여부를 알려주지 않음 */
    @PostMapping
    public ResponseEntity<Void> request(@Valid @RequestBody PasswordResetRequest request,
                                        HttpServletRequest httpRequest) {
        attemptLimiter.acquire(IP_KEY_PREFIX + ClientIp.of(httpRequest), ErrorCode.TOO_MANY_RESET_REQUESTS,
                "비밀번호 재설정 요청이 너무 많습니다.");
        passwordResetService.request(request.email());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping
    public ResponseEntity<Void> confirm(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirm(request);

        return ResponseEntity.noContent().build();
    }
}
