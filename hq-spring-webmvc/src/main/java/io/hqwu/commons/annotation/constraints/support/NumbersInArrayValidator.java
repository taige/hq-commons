package io.hqwu.commons.annotation.constraints.support;


import io.hqwu.commons.annotation.constraints.NumbersIn;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 * User: taige
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
