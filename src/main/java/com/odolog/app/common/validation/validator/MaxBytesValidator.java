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
        // null 통과. 빈 값 판정은 @NotBlank 담당
        if (value == null) {
            return true;
        }

        return value.getBytes(StandardCharsets.UTF_8).length <= max;
    }
}
