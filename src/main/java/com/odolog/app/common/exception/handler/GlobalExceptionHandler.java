package com.odolog.app.common.exception.handler;

import com.odolog.app.common.dto.response.error.ErrorResponse;
import com.odolog.app.common.exception.code.ErrorCode;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * ResponseEntityExceptionHandler 상속. 405·415·404 는 부모가 상태 코드 유지
 * 나머지는 맨 아래 handleUnexpected 가 500 처리
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.from(e));
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailed(AuthenticationFailedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.from(e));
    }

    @ExceptionHandler(ForbiddenAccessException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenAccess(ForbiddenAccessException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.from(e));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.from(e));
    }

    /**
     * 중복 검사와 저장 사이에 끼어든 요청 대비. 최종 방어선은 유니크 제약
     * UNIQUE 위반만 409. 그 밖의 제약 위반은 서버 문제라 500 + ErrorResponse(규칙 11)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException cause
                && cause.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.of(ErrorCode.DUPLICATE_VALUE,
                            "이미 등록된 값입니다. 새로고침 후 다시 시도해 주세요."));
        }

        log.error("데이터 제약 위반. 엔티티와 실제 스키마가 어긋났을 수 있다 "
                + "(ddl-auto 는 제약을 추가만 하고 지우지 않는다)", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.SAVE_FAILED, "저장하지 못했습니다. 서버 로그를 확인해 주세요."));
    }

    /**
     * 본문 해석 실패(깨진 JSON·없는 enum·날짜 형식) → 400
     * Jackson 원문 대신 필드 이름만. 패키지·클래스 이름 노출 방지
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        // 덮어쓰기 방식. 부모가 같은 예외를 이미 처리해 @ExceptionHandler 중복 시 기동 실패
        String field = fieldOf(e);
        String message = (field == null)
                ? "요청 본문을 읽을 수 없습니다. 형식을 확인해 주세요."
                : field + ": 값의 형식이 올바르지 않습니다.";

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.MALFORMED_BODY, message, field));
    }

    /** 실패한 필드 경로. 깨진 JSON 은 null */
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
                .body(ErrorResponse.from(e));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "%s: 올바르지 않은 값입니다 (%s)".formatted(e.getName(), e.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_PARAMETER, message, e.getName()));
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handlePropertyReference(PropertyReferenceException e) {
        String message = "sort: 정렬할 수 없는 속성입니다 (%s)".formatted(e.getPropertyName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_SORT, message, "sort"));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ErrorResponse.from(e));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        // 첫 오류만. 기본 문구는 JVM 로케일을 따라 화면은 code·field 로 번역
        var first = e.getBindingResult().getFieldErrors().stream().findFirst();
        String message = first
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("잘못된 요청입니다.");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, message,
                        first.map(error -> error.getField()).orElse(null)));
    }

    /** 부모가 처리한 4xx 의 본문도 ErrorResponse 로. ProblemDetail 에는 message 없음 */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        ErrorResponse error = switch (statusCode.value()) {
            case 404 -> ErrorResponse.of(ErrorCode.NOT_FOUND, "요청한 주소를 찾을 수 없습니다.");
            case 405 -> ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED, "허용되지 않는 요청 방식입니다.");
            case 415 -> ErrorResponse.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다.");
            default -> statusCode.is4xxClientError()
                    ? ErrorResponse.of(ErrorCode.BAD_REQUEST, "잘못된 요청입니다.")
                    : ErrorResponse.of(ErrorCode.SERVER_ERROR, "요청을 처리하지 못했습니다.");
        };
        return ResponseEntity.status(statusCode).headers(headers).body(error);
    }

    /**
     * 처리하지 못한 예외 = 우리 버그. 500 유지, 본문만 ErrorResponse
     * 예외 원문 비공개, 원인은 로그로
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("처리하지 못한 예외", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.SERVER_ERROR,
                        "서버에서 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."));
    }
}
