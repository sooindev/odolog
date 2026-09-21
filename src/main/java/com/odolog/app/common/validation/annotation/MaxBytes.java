package com.odolog.app.common.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;

/**
 * UTF-8 바이트 길이 상한. @Size 는 글자 수라 한글에서 3배로 벌어진다
 * BCrypt 가 72바이트를 넘기면 예외를 던지는데, @Size(max = 100) 은 한글 25자를 통과시킨다
 */
@Documented
@Constraint(validatedBy = com.odolog.app.common.validation.validator.MaxBytesValidator.class)
@Target({FIELD, PARAMETER, RECORD_COMPONENT, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxBytes {

    String message() default "너무 깁니다";

    /** UTF-8 기준 최대 바이트 수 */
    int value();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
