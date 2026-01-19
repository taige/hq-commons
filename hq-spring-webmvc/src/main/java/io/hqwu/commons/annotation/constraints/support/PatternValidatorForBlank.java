package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.util.StringUtil;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.internal.constraintvalidators.bv.PatternValidator;

/**
 * 针对空白字符串的正则表达式校验器。
 * <p>
 * 继承自 Hibernate Validator 的 {@link PatternValidator}，用于在正则表达式校验时允许空白字符串通过校验。
 * 当被校验的字符序列为空白（null、空字符串或仅包含空白字符）时，直接返回 true；
 * 否则调用父类的校验逻辑进行正则表达式匹配。
 * </p>
 *
 * <p>
 * 典型应用场景：表单字段既允许为空，又要求非空时必须符合特定格式（如手机号、邮箱等）。
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see PatternValidator
 * @see StringUtil#isBlank(CharSequence)
 * @see jakarta.validation.constraints.Pattern
 * Date: 2020/5/19
 * Time: 15:32
 */
public class PatternValidatorForBlank extends PatternValidator {

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext constraintValidatorContext) {
        return StringUtil.isBlank(value) || super.isValid(value, constraintValidatorContext);
    }

}
