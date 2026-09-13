package com.odolog.app.user.dto.response.profile;

import com.odolog.app.user.domain.entity.User;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        String phone
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getPhone()
        );
    }
}
