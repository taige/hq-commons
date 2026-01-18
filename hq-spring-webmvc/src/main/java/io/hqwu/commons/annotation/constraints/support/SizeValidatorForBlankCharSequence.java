package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.util.StringUtil;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.internal.constraintvalidators.bv.size.SizeValidatorForCharSequence;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2020/5/19
 * Time: 12:39
 */
public class SizeValidatorForBlankCharSequence extends SizeValidatorForCharSequence {

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext constraintValidatorContext) {
        return StringUtil.isBlank(value) || super.isValid(value, constraintValidatorContext);
    }

}
