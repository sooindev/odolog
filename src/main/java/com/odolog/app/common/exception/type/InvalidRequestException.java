package com.odolog.app.common.exception.type;

/**
 * 400 전용. 우리가 직접 판단한 "요청이 잘못됐다"
 * 지금까지 400 은 전부 프레임워크(검증 애노테이션·타입 불일치)가 만들었는데,
 * 그것으로 표현할 수 없는 규칙이 생기면 이걸 던진다 (상태 코드 하나당 예외 하나 — 규칙 12)
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
