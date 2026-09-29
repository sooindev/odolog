package com.odolog.app.common.exception.type;

import com.odolog.app.common.exception.code.ErrorCode;

/** 409 전용. 이메일·번호판 중복, 주행거리 감소 */
public class ConflictException extends ApiException {

    public ConflictException(ErrorCode code, String message) {
        super(code, message, null);
    }
}
