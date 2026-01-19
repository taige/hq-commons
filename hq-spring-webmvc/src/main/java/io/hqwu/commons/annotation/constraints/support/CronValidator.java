package io.hqwu.commons.annotation.constraints.support;


import io.hqwu.commons.annotation.constraints.Cron;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.scheduling.support.CronExpression;

/**
 * {@link Cron} 注解的校验器实现,用于验证字符串是否为合法的 Cron 表达式
 * <p>
 * 该校验器使用 {@link CronExpression#isValidExpression(String)} 方法验证 Cron 表达式的有效性。
 * 校验器实现了 {@link ConstraintValidator} 接口,配合 {@link Cron} 注解使用,
 * 可在 Bean Validation 框架中自动进行 Cron 表达式格式校验。
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see Cron
 * @see CronExpression
 * @see ConstraintValidator
 * @see ConstraintValidatorContext
 * Date: 2021-04-26
 * Time: 11:33 a.m.
 */
public class CronValidator implements ConstraintValidator<Cron, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return CronExpression.isValidExpression(value);
    }

}
