package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.annotation.constraints.Base64;
import io.hqwu.commons.util.StringUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class Base64Validator implements ConstraintValidator<Base64, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtil.isBlank(value)) {
            return false;
        }
        try {
            java.util.Base64.getDecoder().decode(value);
        } catch (RuntimeException e) {
            return false;
        }
        return true;
    }
}
