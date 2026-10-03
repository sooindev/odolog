package com.odolog.app.common.exception;

import com.odolog.app.common.dto.response.ErrorResponse;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static ConstraintViolationException unique(String constraintName) {
        return new ConstraintViolationException("Duplicate entry", new SQLException("Duplicate entry"),
                ConstraintViolationException.ConstraintKind.UNIQUE, constraintName);
    }

    @Test
    @DisplayName("경합으로 중복 검사를 지나친 가입은 DUPLICATE_VALUE 가 아니라 EMAIL_DUPLICATE")
    void emailRaceKeepsItsCode() {
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("x", unique("uk_users_email")));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.EMAIL_DUPLICATE);
    }

    @Test
    @DisplayName("커밋 시점 위반처럼 한 겹 더 감싸여 와도 번호판 제약을 알아본다")
    void plateRaceThroughWrappedCause() {
        // 번호판 수정은 커밋 때 flush 되어 원인이 한 겹 더 감싸임
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("x",
                        new PersistenceException("commit", unique("uk_vehicles_user_plate_number"))));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.PLATE_DUPLICATE);
    }

    @Test
    @DisplayName("데드락으로 진 쪽은 500 이 아니라 409 CONCURRENT_UPDATE — 같은 차량에 주유 두 건을 동시에 저장할 때")
    void deadlockIsConcurrentUpdate() {
        // 둘 다 자식 행 INSERT 로 차량 행 공유 잠금 → 차량 주행거리 UPDATE 에서 서로 대기
        ResponseEntity<ErrorResponse> response = handler.handleConcurrencyFailure(
                new CannotAcquireLockException("Deadlock found when trying to get lock"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.CONCURRENT_UPDATE);
    }

    @Test
    @DisplayName("이름을 모르는 유니크 제약은 그대로 DUPLICATE_VALUE")
    void unknownUniqueConstraint() {
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("x", unique("uk_something_else")));

        assertThat(response.getBody().code()).isEqualTo(ErrorCode.DUPLICATE_VALUE);
    }
}
