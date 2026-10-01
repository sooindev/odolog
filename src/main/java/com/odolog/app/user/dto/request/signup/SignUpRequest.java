package com.odolog.app.user.dto.request.signup;

import com.odolog.app.common.validation.annotation.MaxBytes;
import com.odolog.app.common.validation.limit.InputLimits;
import com.odolog.app.user.domain.type.Language;
import com.odolog.app.user.domain.type.UnitSystem;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(

        @NotBlank
        @Email
        @Size(max = InputLimits.MAX_EMAIL_LENGTH)
        @Pattern(regexp = InputLimits.EMAIL_CHARS)
        String email,

        // 바이트 기준 상한. BCrypt 72바이트, 글자 수 기준이면 한글에서 초과
        @NotBlank
        @Size(min = 8)
        @MaxBytes(value = 72, message = "비밀번호는 UTF-8 기준 72바이트를 넘을 수 없습니다 (한글은 글자당 3바이트)")
        String password,

        @NotBlank
        @Size(max = 30)
        String nickname,

        // 설정 넷은 선택. 안 보내면 엔티티 기본값. 화면이 브라우저 값으로 채움
        Language language,

        @Size(max = 64)
        String timeZone,

        @Size(max = 3)
        String currency,

        UnitSystem unitSystem
) {
}
