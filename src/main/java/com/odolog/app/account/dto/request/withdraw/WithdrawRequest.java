package com.odolog.app.account.dto.request.withdraw;

import jakarta.validation.constraints.NotBlank;

/**
 * 탈퇴에 비밀번호를 요구하는 이유는 되돌릴 수 없기 때문이다.
 * 로그인된 세션만으로 계정을 지울 수 있으면, 자리를 비운 사이 누가 눌러도 막을 방법이 없다.
 *
 * 체크박스("정말 삭제합니다")로 대신하지 않는다. 그건 실수를 막을 뿐 본인 확인이 아니다.
 */
public record WithdrawRequest(

        @NotBlank
        String password
) {
}
