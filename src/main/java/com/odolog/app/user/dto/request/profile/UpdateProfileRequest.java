package com.odolog.app.user.dto.request.profile;

import com.odolog.app.user.domain.type.Language;
import com.odolog.app.user.domain.type.UnitSystem;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 전부 선택. null 은 유지 */
public record UpdateProfileRequest(

        @Size(min = 1, max = 30)
        @Pattern(regexp = ".*\\S.*", message = "공백만으로는 설정할 수 없습니다")
        String nickname,

        Language language,

        // 목록 검사는 엔티티. 여기서는 컬럼 길이만
        @Size(max = 64)
        String timeZone,

        @Size(max = 3)
        String currency,

        UnitSystem unitSystem
) {
}
