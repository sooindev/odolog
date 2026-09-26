package com.odolog.app.user.dto.response.profile;

import com.odolog.app.user.domain.entity.User;

/**
 * 숫자 id 는 담지 않는다(규칙 9-1) — 가입 순서가 곧 "몇 번째 사용자인가" 다.
 * 화면은 id 를 쓰지 않고, 사용자는 URL 에 나오지 않아 공개 id 도 필요 없다
 */
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
