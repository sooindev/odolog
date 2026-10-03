package com.odolog.app.user.dto.request;

import com.odolog.app.common.validation.InputLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        // 길이 상한은 시도 횟수 맵의 키 크기 상한
        @NotBlank
        @Size(max = InputLimits.MAX_EMAIL_LENGTH)
        @Pattern(regexp = InputLimits.EMAIL_CHARS)
        String email,

        // 확인용 칸도 상한. 긴 문자열로 BCrypt·메모리 낭비 방지
        @NotBlank
        @Size(max = InputLimits.MAX_PASSWORD_LENGTH)
        String password
) {
}
