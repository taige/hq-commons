package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.util.StringUtil;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator;

/**
 * 多邮箱地址校验器实现,用于验证以逗号分隔的多个邮箱地址字符串是否合法。
 * <p>
 * 该校验器继承自 {@link EmailValidator},支持对逗号分隔的多个邮箱地址进行批量校验。
 * 每个邮箱地址会被单独提取并去除前后空格后,使用父类的邮箱校验逻辑进行验证。
 * 只有当所有邮箱地址均合法时,整体校验才会通过。
 * </p>
 * <p>
 * 校验规则:
 * <ul>
 *   <li>空字符串或 null 值视为合法</li>
 *   <li>使用逗号 "," 作为邮箱地址分隔符</li>
 *   <li>每个邮箱地址前后的空格会被自动去除</li>
 *   <li>任意一个邮箱地址不合法,则整体校验失败</li>
 * </ul>
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see EmailValidator
 * @see ConstraintValidatorContext
 * @see StringUtil
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
