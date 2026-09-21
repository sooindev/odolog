package com.odolog.app.common.exception.type;

/** 429 전용. 같은 자격증명으로 너무 자주 시도했을 때 */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
