package io.hqwu.commons.annotation.constraints;


import io.hqwu.commons.annotation.constraints.support.StringTimeValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 * User: taige
 * Date: 2020/5/9
 * Time: 13:21
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = StringTimeValidator.class)
@Documented
public @interface StringTime {

    String value();

    String message() default "{io.hqwu.commons.annotation.constraints.StringTime.message}";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
