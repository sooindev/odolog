package com.odolog.app.common.exception.type;

/**
 * 요청 값이 리소스의 현재 상태와 충돌. 409 로 매핑
 * 이메일·번호판 중복, 주행거리 감소
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
