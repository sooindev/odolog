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
 * UTF-8 바이트 상한. @Size 는 글자 수 기준이라 한글에서 3배 차이
 * BCrypt 72바이트 상한 대응
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
