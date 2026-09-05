package com.cartree.app.dto;

import com.cartree.app.domain.User;

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
