package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.annotation.constraints.StringTime;
import io.hqwu.commons.util.Logger;
import io.hqwu.commons.util.StringUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.format.DateTimeFormatter;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 * User: taige
 * Date: 2020/5/9
 * Time: 13:23
 */
public class StringTimeValidator implements ConstraintValidator<StringTime, String> {
    private static final Logger LOGGER = new Logger();

    private String pattern;
    private ThreadLocal<DateTimeFormatter> dateFormatThreadLocal = ThreadLocal.withInitial(() ->
            DateTimeFormatter.ofPattern(pattern));

    @Override
    public void initialize(StringTime constraintAnnotation) {
        pattern = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtil.isBlank(value)) {
            return true;
        }
        try {
            DateTimeFormatter fmt = dateFormatThreadLocal.get();
            return fmt.format(fmt.parse(value)).equals(value);
        } catch (RuntimeException e) {
            LOGGER.trace("illegal datetime format: %s %s %s", pattern, value, e.toString());
            return false;
        }
    }
}
