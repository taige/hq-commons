package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.annotation.constraints.Base64;
import io.hqwu.commons.util.StringUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link Base64} 注解的校验器实现,用于验证字符串是否为合法的 Base64 编码格式。
 * <p>
 * 该校验器使用 {@link java.util.Base64#getDecoder()} 方法验证字符串的有效性。
 * 如果字符串为空或无法解码,则校验失败。
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see Base64
 * @see java.util.Base64
 * @see ConstraintValidator
 */
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
