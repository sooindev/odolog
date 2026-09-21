package com.odolog.app.user.dto.request.password;

import com.odolog.app.common.validation.annotation.MaxBytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 새 비밀번호 제한은 가입·변경과 같아야 한다 — 느슨하면 여기로 우회가 된다 */
public record PasswordResetConfirmRequest(

        @NotBlank
        String token,

        @NotBlank
        @Size(min = 8)
        @MaxBytes(value = 72, message = "비밀번호는 UTF-8 기준 72바이트를 넘을 수 없습니다 (한글은 글자당 3바이트)")
        String newPassword
) {
}
