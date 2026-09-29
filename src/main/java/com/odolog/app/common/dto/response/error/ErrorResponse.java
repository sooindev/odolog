package com.odolog.app.common.dto.response.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.odolog.app.common.exception.code.ErrorCode;
import com.odolog.app.common.exception.type.ApiException;
import com.odolog.app.common.exception.type.TooManyRequestsException;

/**
 * 오류 응답. 화면은 code 로 문구를 고르고 message 는 대비책
 * field·retryAfterMinutes 는 해당할 때만 실림
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        ErrorCode code,
        String message,
        String field,
        Long retryAfterMinutes
) {

    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(code, message, null, null);
    }

    public static ErrorResponse of(ErrorCode code, String message, String field) {
        return new ErrorResponse(code, message, field, null);
    }

    public static ErrorResponse from(ApiException e) {
        Long minutes = (e instanceof TooManyRequestsException tooMany) ? tooMany.getRetryAfterMinutes() : null;
        return new ErrorResponse(e.getCode(), e.getMessage(), e.getField(), minutes);
    }
}
