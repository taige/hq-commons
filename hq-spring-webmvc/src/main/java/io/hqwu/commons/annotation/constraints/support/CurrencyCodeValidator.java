package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.annotation.constraints.CurrencyCode;
import io.hqwu.commons.util.StringUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link CurrencyCode} 注解的校验器实现,用于验证字符串是否为合法的 ISO 4217 货币代码
 * <p>
 * 该校验器使用 {@link java.util.Currency#getInstance(String)} 方法验证货币代码的有效性。
 * 支持通过 {@link CurrencyCode#allowEmpty()} 配置是否允许空字符串通过校验。
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see CurrencyCode
 * @see java.util.Currency
 * @see ConstraintValidator
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
