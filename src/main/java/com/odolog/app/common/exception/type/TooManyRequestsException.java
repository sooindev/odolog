package com.odolog.app.common.exception.type;

import com.odolog.app.common.exception.code.ErrorCode;

/** 429 전용. 시도 횟수 초과 */
public class TooManyRequestsException extends ApiException {

    private final long retryAfterMinutes;

    public TooManyRequestsException(ErrorCode code, String message, long retryAfterMinutes) {
        super(code, message, null);
        this.retryAfterMinutes = retryAfterMinutes;
    }

    public long getRetryAfterMinutes() {
        return retryAfterMinutes;
    }
}
