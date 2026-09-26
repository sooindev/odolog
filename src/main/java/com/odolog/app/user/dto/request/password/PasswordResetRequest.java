package com.odolog.app.user.dto.request.password;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 재설정 링크 요청. 가입 여부와 무관하게 같은 응답 */
public record PasswordResetRequest(

        @NotBlank
        @Email
        @Size(max = 100)
        String email
) {
}
