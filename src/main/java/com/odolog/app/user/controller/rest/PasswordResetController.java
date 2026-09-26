package com.odolog.app.user.controller.rest;

import com.odolog.app.user.dto.request.password.PasswordResetConfirmRequest;
import com.odolog.app.user.dto.request.password.PasswordResetRequest;
import com.odolog.app.user.service.application.PasswordResetService;
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

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    /** 가입 여부와 무관하게 204 */
    @PostMapping
    public ResponseEntity<Void> request(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.request(request.email());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping
    public ResponseEntity<Void> confirm(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirm(request);

        return ResponseEntity.noContent().build();
    }
}
