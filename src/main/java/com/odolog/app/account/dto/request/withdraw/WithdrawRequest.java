package com.odolog.app.account.dto.request.withdraw;

import jakarta.validation.constraints.NotBlank;

/**
 * 되돌릴 수 없는 동작이라 비밀번호 필수
 * 체크박스로는 본인 확인 불가
 */
public record WithdrawRequest(

        @NotBlank
        String password
) {
}
