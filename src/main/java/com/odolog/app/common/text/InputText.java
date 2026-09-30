package com.odolog.app.common.text;

import com.odolog.app.common.exception.code.ErrorCode;
import com.odolog.app.common.exception.type.InvalidRequestException;

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

    /**
     * 필수 입력용 strip. 지운 뒤 비면 400
     * @NotBlank(trim 기준)·\S 는 전각 공백을 글자로 보고 통과시킴. 그대로 두면 빈 문자열 저장
     */
    public static String required(String value, String field) {
        String stripped = strip(value);
        if (stripped != null && stripped.isEmpty()) {
            throw new InvalidRequestException(ErrorCode.VALIDATION_FAILED,
                    field + ": 공백만으로는 설정할 수 없습니다", field);
        }
        return stripped;
    }
}
