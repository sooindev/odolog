package com.odolog.app.user.dto.response.profile;

import com.odolog.app.user.domain.entity.User;

/** 숫자 id 미포함(규칙 9-1). 가입 순서 노출 방지 */
public record UserResponse(
        String email,
        String nickname,
        String phone
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getEmail(),
                user.getNickname(),
                user.getPhone()
        );
    }
}
