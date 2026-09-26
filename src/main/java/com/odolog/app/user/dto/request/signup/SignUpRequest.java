package com.odolog.app.user.dto.request.signup;

import com.odolog.app.common.validation.annotation.MaxBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(

        @NotBlank
        @Email
        @Size(max = 100)
        String email,

        // 바이트 기준 상한. BCrypt 72바이트, 글자 수 기준이면 한글에서 초과
        @NotBlank
        @Size(min = 8)
        @MaxBytes(value = 72, message = "비밀번호는 UTF-8 기준 72바이트를 넘을 수 없습니다 (한글은 글자당 3바이트)")
        String password,

        @NotBlank
        @Size(max = 30)
        String nickname,

        // phone 컬럼 길이 20과 동일
        @Size(max = 20)
        String phone
) {
}
