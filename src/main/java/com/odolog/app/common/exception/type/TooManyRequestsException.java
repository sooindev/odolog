package com.odolog.app.common.exception.type;

/** 429 전용. 시도 횟수 초과 */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
