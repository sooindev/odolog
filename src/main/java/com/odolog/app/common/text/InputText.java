package com.odolog.app.common.text;

/**
 * 입력 앞뒤 공백 정리. 서비스가 저장 직전 호출
 * DB 는 뒤 공백 무시, 자바 equals 는 구분. 그 차이로 생기는 자기 중복 409 방지
 */
public final class InputText {

    private InputText() {
    }

    /** strip() 사용. 전각 공백(U+3000)까지 제거 */
    public static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
