package com.odolog.app.user.dto;

import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.domain.type.Language;
import com.odolog.app.user.domain.type.UnitSystem;

/** 숫자 id 미포함(규칙 9-1). 가입 순서 노출 방지 */
public record UserResponse(
        String email,
        String nickname,
        Language language,
        String timeZone,
        String currency,
        UnitSystem unitSystem
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getEmail(),
                user.getNickname(),
                user.getLanguage(),
                user.getTimeZone(),
                user.getCurrency(),
                user.getUnitSystem()
        );
    }
}
