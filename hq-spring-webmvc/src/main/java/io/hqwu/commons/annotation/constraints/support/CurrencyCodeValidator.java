package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.annotation.constraints.CurrencyCode;
import io.hqwu.commons.util.StringUtil;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Created with IntelliJ IDEA for hq-commons-parent
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-10-12
 * Time: 10:20
 */
public class CurrencyCodeValidator implements ConstraintValidator<CurrencyCode, String> {

    private boolean allowEmpty = false;

    @Override
    public void initialize(CurrencyCode constraintAnnotation) {
        allowEmpty = constraintAnnotation.allowEmpty();
    }

    @Override
    public boolean isValid(String currencyCode, ConstraintValidatorContext context) {
        if (StringUtil.isBlank(currencyCode)) {
            return allowEmpty;
        }
        try {
            java.util.Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

}
