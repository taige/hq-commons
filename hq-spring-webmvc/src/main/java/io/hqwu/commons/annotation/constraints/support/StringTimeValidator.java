package io.hqwu.commons.annotation.constraints.support;

import com.umpay.commons.util.Logger;
import com.umpay.commons.util.StringUtil;
import io.hqwu.commons.annotation.constraints.StringTime;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 * User: taige
 * Date: 2020/5/9
 * Time: 13:23
 */
public class StringTimeValidator implements ConstraintValidator<StringTime, String> {
    private static final Logger LOGGER = new Logger();

    private String pattern;
    private ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = ThreadLocal.withInitial(() ->
            new SimpleDateFormat(pattern));

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
            SimpleDateFormat fmt = dateFormatThreadLocal.get();
            return fmt.format(fmt.parse(value)).equals(value);
        } catch (RuntimeException | ParseException e) {
            LOGGER.info(e);
            return false;
        }
    }
}
