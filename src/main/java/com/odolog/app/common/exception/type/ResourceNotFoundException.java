package com.odolog.app.common.exception.type;

import com.odolog.app.common.exception.code.ErrorCode;

/** 404 전용. 없는 자원과 남의 자원을 같은 응답으로 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(ErrorCode code, String message) {
        super(code, message, null);
    }
}
