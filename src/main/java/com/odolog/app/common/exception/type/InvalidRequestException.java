package com.odolog.app.common.exception.type;

/** 400 전용. 검증 애노테이션으로 표현할 수 없는 규칙용 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
