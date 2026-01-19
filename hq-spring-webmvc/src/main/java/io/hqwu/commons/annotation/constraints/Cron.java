package io.hqwu.commons.annotation.constraints;


import io.hqwu.commons.annotation.constraints.support.CronValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cron 表达式格式校验注解。
 * <p>
 * 用于标注在字段上，校验字段值是否为合法的 Cron 表达式。
 * 该注解基于 Bean Validation 框架，通过 {@link CronValidator} 实现具体的校验逻辑。
 * </p>
 * <p>
 * 示例：
 * <pre>
 * public class ScheduledTask {
 *     &#64;Cron
 *     private String cronExpression;
 * }
 * </pre>
 *
 * @author taige (Wu, Hongqiang)
 * @see CronValidator
 * @see Constraint
 * @see jakarta.validation.ConstraintValidator
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CronValidator.class)
public @interface Cron {

    String message() default "{io.hqwu.commons.annotation.constraints.Cron.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
