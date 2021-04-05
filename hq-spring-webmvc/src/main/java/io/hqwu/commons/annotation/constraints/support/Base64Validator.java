package io.hqwu.commons.annotation.constraints.support;

import com.umpay.commons.util.StringUtil;
import io.hqwu.commons.annotation.constraints.Base64;
import org.springframework.util.Base64Utils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class Base64Validator implements ConstraintValidator<Base64, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtil.isBlank(value)) {
            return false;
        }
        try {
            Base64Utils.decodeFromString(value);
        } catch (RuntimeException e) {
            return false;
        }
        return true;
    }
}
