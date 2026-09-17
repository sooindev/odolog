package com.odolog.app.user.dto.request.signup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(

        @NotBlank
        @Email
        @Size(max = 100)
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @NotBlank
        @Size(max = 30)
        String nickname,

        // phone 컬럼이 length = 20. 여기서 안 막으면 DB 까지 가서 500
        @Size(max = 20)
        String phone
) {
}
