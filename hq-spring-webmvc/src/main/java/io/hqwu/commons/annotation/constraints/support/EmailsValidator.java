package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.util.StringUtil;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2020/7/2
 * Time: 20:26
 */
public class EmailsValidator extends EmailValidator {

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (StringUtil.isBlank(value)) {
            return true;
        }
        String[] emails = value.toString().split(",");
        for(String email : emails) {
            if(! super.isValid(email.trim(), context)) {
                return false;
            }
        }
        return true;
    }

}
