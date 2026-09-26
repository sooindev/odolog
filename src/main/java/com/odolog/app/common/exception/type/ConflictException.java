package com.odolog.app.common.exception.type;

/** 409 전용. 이메일·번호판 중복, 주행거리 감소 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
