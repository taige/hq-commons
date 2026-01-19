package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.annotation.constraints.StringTime;
import io.hqwu.commons.util.Logger;
import io.hqwu.commons.util.StringUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.format.DateTimeFormatter;

/**
 * {@link StringTime} 注解的校验器实现。<br/>
 * <p>
 * 用于校验字符串是否符合指定的日期时间格式模式。<br/>
 * 通过解析字符串并将其重新格式化，确保输入值与期望的格式完全匹配。<br/>
 *
 * @author taige (Wu, Hongqiang)
 * @see StringTime
 * @see ConstraintValidator
 * @see DateTimeFormatter
 * Date: 2020/5/9
 * Time: 13:23
 */
public class StringTimeValidator implements ConstraintValidator<StringTime, String> {
    private static final Logger LOGGER = new Logger();

    private String pattern;
    private DateTimeFormatter dateTimeFormatter;

    @Override
    public void initialize(StringTime constraintAnnotation) {
        pattern = constraintAnnotation.value();
        this.dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtil.isBlank(value)) {
            return true;
        }
        try {
            return dateTimeFormatter.format(dateTimeFormatter.parse(value)).equals(value);
        } catch (RuntimeException e) {
            LOGGER.trace("illegal datetime format: %s %s %s", pattern, value, e.toString());
            return false;
        }
    }
}
