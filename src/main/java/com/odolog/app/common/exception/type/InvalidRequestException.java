package com.odolog.app.common.exception.type;

import com.odolog.app.common.exception.ErrorCode;

/** 400 전용. 검증 애노테이션으로 표현할 수 없는 규칙용 */
public class InvalidRequestException extends ApiException {

    public InvalidRequestException(ErrorCode code, String message) {
        super(code, message, null);
    }

    public InvalidRequestException(ErrorCode code, String message, String field) {
        super(code, message, field);
    }
}
