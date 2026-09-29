package com.odolog.app.common.exception.type;

import com.odolog.app.common.exception.code.ErrorCode;

/** 403 전용. 지금 던지는 곳 없음(남의 자원은 404) */
public class ForbiddenAccessException extends ApiException {

    public ForbiddenAccessException(ErrorCode code, String message) {
        super(code, message, null);
    }
}
