package com.odolog.app.common.exception.handler;

import com.odolog.app.common.dto.response.error.ErrorResponse;
import com.odolog.app.common.exception.type.AuthenticationFailedException;
import com.odolog.app.common.exception.type.ConflictException;
import com.odolog.app.common.exception.type.TooManyRequestsException;
import com.odolog.app.common.exception.type.ForbiddenAccessException;
import com.odolog.app.common.exception.type.InvalidRequestException;
import com.odolog.app.common.exception.type.ResourceNotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailed(AuthenticationFailedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(ForbiddenAccessException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenAccess(ForbiddenAccessException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage()));
    }

    /**
     * 중복 검사와 저장 사이에 끼어든 요청 대비. 진짜 방어선은 유니크 제약, existsBy 는 메시지용
     * 유니크 위반일 때만 409 — NOT NULL 위반 같은 우리 버그까지 감싸면 500 이 4xx 로 새어 나감
     *
     * 유니크가 아니면 **상태 코드는 그대로 500 이다.** 스키마와 엔티티가 어긋난 것은 서버 쪽
     * 문제이지 요청한 사람이 고칠 수 있는 일이 아니기 때문이다(규칙 11).
     * 다만 본문은 우리 모양으로 돌려준다 — 전에는 예외를 다시 던져 스프링 기본 응답이 나갔고,
     * 거기에는 message 가 없어 화면이 "요청에 실패했습니다 (HTTP 500)" 밖에 말하지 못했다.
     * 예외를 던지지 않으므로 스프링이 대신 찍어 주던 스택도 사라진다. 그래서 여기서 직접 남긴다
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException cause
                && cause.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("이미 등록된 값입니다. 새로고침 후 다시 시도해 주세요."));
        }

        log.error("데이터 제약 위반. 엔티티와 실제 스키마가 어긋났을 수 있다 "
                + "(ddl-auto 는 제약을 추가만 하고 지우지 않는다)", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("저장하지 못했습니다. 서버 로그를 확인해 주세요."));
    }

    /**
     * 본문을 읽다 실패한 경우 — 깨진 JSON, 없는 enum 값, 날짜 형식 오류, 타입 불일치
     *
     * 전에는 핸들러가 없어 스프링 기본 응답(timestamp/status/error/path)이 그대로 나갔다.
     * 거기에는 message 가 없어 화면이 "요청에 실패했습니다 (HTTP 400)" 로 떨어졌고,
     * 덤으로 내부 경로가 응답에 실렸다
     *
     * 원인 메시지를 그대로 내보내지 않는 이유: Jackson 의 메시지는 패키지 이름과 클래스 이름을
     * 그대로 담는다. 대신 어느 필드인지만 뽑아 준다 — 고치는 데 필요한 것은 그것뿐이다
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        String field = fieldOf(e);
        String message = (field == null)
                ? "요청 본문을 읽을 수 없습니다. 형식을 확인해 주세요."
                : field + ": 값의 형식이 올바르지 않습니다.";

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }

    /** 어느 필드에서 막혔는지. 알 수 없으면 null (깨진 JSON 은 필드를 특정할 수 없다) */
    private String fieldOf(HttpMessageNotReadableException e) {
        if (!(e.getCause() instanceof com.fasterxml.jackson.databind.JsonMappingException cause)) {
            return null;
        }

        return cause.getPath().stream()
                .map(com.fasterxml.jackson.databind.JsonMappingException.Reference::getFieldName)
                .filter(name -> name != null)
                .reduce((first, second) -> first + "." + second)
                .orElse(null);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "%s: 올바르지 않은 값입니다 (%s)".formatted(e.getName(), e.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message));
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handlePropertyReference(PropertyReferenceException e) {
        String message = "sort: 정렬할 수 없는 속성입니다 (%s)".formatted(e.getPropertyName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("잘못된 요청입니다.");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message));
    }
}
