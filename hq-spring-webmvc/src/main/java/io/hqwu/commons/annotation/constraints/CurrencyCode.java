package io.hqwu.commons.annotation.constraints;


import io.hqwu.commons.annotation.constraints.support.CurrencyCodeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

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
@Constraint(validatedBy = CurrencyCodeValidator.class)
@Documented
public @interface CurrencyCode {

    boolean allowEmpty() default false;

    String message() default "{io.hqwu.commons.annotation.constraints.CurrencyCode.message}";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
