package io.hqwu.commons.annotation.constraints.support;


import io.hqwu.commons.annotation.constraints.Cron;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.scheduling.support.CronExpression;

/**
 * Created with IntelliJ IDEA for bp-public-api
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-04-26
 * Time: 11:33 a.m.
 */
public class CronValidator implements ConstraintValidator<Cron, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return CronExpression.isValidExpression(value);
    }

}
