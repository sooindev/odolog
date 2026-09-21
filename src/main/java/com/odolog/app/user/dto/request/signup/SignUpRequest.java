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

        // max 를 글자 수로 두지 않는 이유: BCrypt 는 72바이트가 상한이고,
        // @Size(max = 100) 은 한글 25자(75바이트)를 통과시켜 인코딩에서 500 이 났다
        @NotBlank
        @Size(min = 8)
        @MaxBytes(value = 72, message = "비밀번호는 UTF-8 기준 72바이트를 넘을 수 없습니다 (한글은 글자당 3바이트)")
        String password,

        @NotBlank
        @Size(max = 30)
        String nickname,

        // phone 컬럼이 length = 20. 여기서 안 막으면 DB 까지 가서 500
        @Size(max = 20)
        String phone
) {
}
