package com.odolog.app.user.dto.request.password;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 부분 수정이 아니라 둘 다 필수라 NotBlank
 * 새 비밀번호 길이 제한은 가입과 동일해야 함 — 느슨하면 가입으로 못 만드는 것이 변경으로 통과
 */
public record ChangePasswordRequest(

        @NotBlank
        String currentPassword,

        @NotBlank
        @Size(min = 8, max = 100)
        String newPassword
) {
}
