package io.hqwu.commons.annotation.constraints.support;


import io.hqwu.commons.annotation.constraints.NumbersIn;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

/**
 * {@link NumbersIn} 注解的校验器实现,用于验证数字是否在指定的数值集合中
 * <p>
 * 该校验器使用数组匹配方式验证数字值是否存在于预定义的 long 类型数组中。
 * 校验时会将被校验的 {@link Number} 转换为 long 类型进行匹配。
 * null 值视为合法,通过校验。
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see NumbersIn
 * @see ConstraintValidator
 * @see ConstraintValidatorContext
 * Date: 2020/5/9
 * Time: 13:23
 */
public class NumbersInValidator implements ConstraintValidator<NumbersIn, Number> {

    private long[] values;

    @Override
    public void initialize(NumbersIn constraintAnnotation) {
        values = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(Number value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return Arrays.stream(values).anyMatch(v -> v == value.longValue());
    }
}
