package io.hqwu.commons.annotation.constraints.support;


import io.hqwu.commons.annotation.constraints.NumbersIn;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

/**
 * {@link NumbersIn} 注解的数组校验器实现,用于验证数字数组中是否至少包含一个指定的数值。
 * <p>
 * 该校验器用于校验 {@link Number} 类型的数组,验证数组中是否至少有一个元素的值在注解指定的候选值集合中。
 * 校验器实现了 {@link ConstraintValidator} 接口,配合 {@link NumbersIn} 注解使用,
 * 可在 Bean Validation 框架中自动进行数值范围校验。
 * </p>
 * <p>
 * 校验规则:
 * <ul>
 *   <li>如果待校验数组为 null,则校验通过</li>
 *   <li>如果数组中至少有一个元素的 long 值与注解指定的候选值集合中的某个值相等,则校验通过</li>
 *   <li>否则校验失败</li>
 * </ul>
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see NumbersIn
 * @see NumbersInValidator
 * @see ConstraintValidator
 * @see ConstraintValidatorContext
 * Date: 2020/5/9
 * Time: 13:23
 */
public class NumbersInArrayValidator implements ConstraintValidator<NumbersIn, Number[]> {

    private long[] values;

    @Override
    public void initialize(NumbersIn constraintAnnotation) {
        values = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(Number[] value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return Arrays.stream(values).anyMatch(v -> Arrays.stream(value).anyMatch(vv -> vv.longValue() == v));
    }
}
