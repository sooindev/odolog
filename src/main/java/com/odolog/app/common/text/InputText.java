package com.odolog.app.common.text;

/**
 * 사용자가 친 글자의 앞뒤 공백 정리. 서비스가 저장 직전에 부른다(규칙 14-1 과 같은 자리)
 *
 * 필요한 이유: DB(utf8mb4_unicode_ci)는 뒤 공백을 무시하고 비교하는데 자바 equals 는 구분한다.
 * "12가3456 " 이 저장돼 있으면 공백만 지운 수정이 자기 자신과 중복으로 잡혀 409 가 났다
 */
public final class InputText {

    private InputText() {
    }

    /** 앞뒤 공백 제거. trim() 이 아니라 strip() — 전각 공백(U+3000)까지 지운다 */
    public static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
