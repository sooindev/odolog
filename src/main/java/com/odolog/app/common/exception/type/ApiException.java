package com.odolog.app.common.exception.type;

import com.odolog.app.common.exception.ErrorCode;

/**
 * 우리가 던지는 예외의 공통 부모. 코드는 필수
 * message 는 로그·개발자용 한국어. 화면 문구는 코드로 결정
 */
public abstract class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final String field;

    protected ApiException(ErrorCode code, String message, String field) {
        super(message);
        this.code = code;
        this.field = field;
    }

    public ErrorCode getCode() {
        return code;
    }

    /** 문제가 된 입력 칸. 없으면 null */
    public String getField() {
        return field;
    }
}
