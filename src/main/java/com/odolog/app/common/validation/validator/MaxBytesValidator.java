package com.odolog.app.common.validation.validator;

import com.odolog.app.common.validation.annotation.MaxBytes;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class MaxBytesValidator implements ConstraintValidator<MaxBytes, String> {

    private int max;

    @Override
    public void initialize(MaxBytes constraint) {
        this.max = constraint.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null 은 통과. "비었는가"는 @NotBlank 가 따로 본다 — 부분 수정 DTO 와도 짝이 맞는다
        if (value == null) {
            return true;
        }

        return value.getBytes(StandardCharsets.UTF_8).length <= max;
    }
}
