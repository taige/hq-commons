package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.util.StringUtil;
import org.hibernate.validator.internal.constraintvalidators.bv.PatternValidator;

import javax.validation.ConstraintValidatorContext;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2020/5/19
 * Time: 15:32
 */
public class PatternValidatorForBlank extends PatternValidator {

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext constraintValidatorContext) {
        return StringUtil.isBlank(value) || super.isValid(value, constraintValidatorContext);
    }

}
