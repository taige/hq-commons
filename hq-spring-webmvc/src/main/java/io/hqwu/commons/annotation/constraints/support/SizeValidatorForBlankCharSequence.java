package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.util.StringUtil;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.internal.constraintvalidators.bv.size.SizeValidatorForCharSequence;

/**
 * 用于字符序列的自定义尺寸校验器，支持空白字符序列的校验。
 * <p>
 * 该校验器继承自 {@link SizeValidatorForCharSequence}，在原有尺寸校验的基础上，
 * 对空白字符序列（null、空字符串或仅包含空白字符）进行特殊处理，使其直接通过校验。
 * </p>
 * <p>
 * 主要用于需要对字符序列长度进行限制，但允许字段为空或仅包含空白字符的场景。
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see SizeValidatorForCharSequence
 * @see StringUtil#isBlank(CharSequence)
 * @see jakarta.validation.constraints.Size
 * @since 2020/5/19
 */
public class SizeValidatorForBlankCharSequence extends SizeValidatorForCharSequence {

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext constraintValidatorContext) {
        return StringUtil.isBlank(value) || super.isValid(value, constraintValidatorContext);
    }

}
