package com.odolog.app.common.exception.type;

import com.odolog.app.common.exception.code.ErrorCode;

/** 401 전용 */
public class AuthenticationFailedException extends ApiException {

    public AuthenticationFailedException(ErrorCode code, String message) {
        super(code, message, null);
    }
}
