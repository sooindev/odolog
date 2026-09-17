package com.odolog.app.account.dto.request.withdraw;

import jakarta.validation.constraints.NotBlank;

/**
 * 되돌릴 수 없는 동작이라 비밀번호 필수
 * 체크박스로 대신하지 않음 — 그건 실수 방지일 뿐 본인 확인이 아님
 */
public record WithdrawRequest(

        @NotBlank
        String password
) {
}
