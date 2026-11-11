package com.py2java.cloudide.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;

public class CodePointSizeValidator implements ConstraintValidator<CodePointSize, CharSequence> {
    private int min;
    private int max;

    @Override
    public void initialize(CodePointSize constraintAnnotation) {
        min = constraintAnnotation.min();
        max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (Objects.isNull(value)) {
            return true;
        }
        int codePointLength = Character.codePointCount(value, 0, value.length());
        return codePointLength >= min && codePointLength <= max;
    }
}
