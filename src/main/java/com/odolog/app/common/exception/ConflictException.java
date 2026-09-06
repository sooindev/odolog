package com.odolog.app.common.exception;

/**
 * 요청 값이 리소스의 현재 상태와 충돌할 때. 409 Conflict 로 매핑된다.
 * 예: 이미 가입된 이메일, 이미 등록된 번호판, 지금보다 작은 주행거리.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
