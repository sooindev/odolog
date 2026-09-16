package com.odolog.app.user.dto.request.password;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 부분 수정이 아니라 둘 다 반드시 필요하므로 @NotBlank 를 쓴다.
 * (UpdateProfileRequest 가 @NotBlank 를 피하는 이유는 "안 보냄"을 허용해야 해서다 — 여기는 반대다.)
 *
 * 새 비밀번호의 길이 제한은 가입 때와 같아야 한다. 여기만 느슨하면 가입으로는 못 만드는
 * 비밀번호를 변경으로는 만들 수 있게 된다.
 */
public record ChangePasswordRequest(

        @NotBlank
        String currentPassword,

        @NotBlank
        @Size(min = 8, max = 100)
        String newPassword
) {
}
