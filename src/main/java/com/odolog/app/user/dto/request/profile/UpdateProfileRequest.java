package com.odolog.app.user.dto.request.profile;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @Size(min = 1, max = 30)
        @Pattern(regexp = ".*\\S.*", message = "공백만으로는 설정할 수 없습니다")
        String nickname,

        @Size(max = 20)
        String phone
) {
}
