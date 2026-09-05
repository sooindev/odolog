package com.odolog.app.user.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @Size(max = 30)
        String nickname,

        @Size(max = 20)
        String phone
) {
}
