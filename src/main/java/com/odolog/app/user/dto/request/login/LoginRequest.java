package com.odolog.app.user.dto.request.login;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank
        String email,

        @NotBlank
        String password
) {
}
