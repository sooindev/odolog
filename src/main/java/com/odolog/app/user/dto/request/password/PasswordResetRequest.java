package com.odolog.app.user.dto.request.password;

import com.odolog.app.common.validation.InputLimits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 재설정 링크 요청. 가입 여부와 무관하게 같은 응답 */
public record PasswordResetRequest(

        @NotBlank
        @Email
        @Size(max = InputLimits.MAX_EMAIL_LENGTH)
        @Pattern(regexp = InputLimits.EMAIL_CHARS)
        String email
) {
}
