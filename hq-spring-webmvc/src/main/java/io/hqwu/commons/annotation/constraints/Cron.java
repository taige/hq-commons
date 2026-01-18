package io.hqwu.commons.annotation.constraints;


import io.hqwu.commons.annotation.constraints.support.CronValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CronValidator.class)
public @interface Cron {

    String message() default "{io.hqwu.commons.annotation.constraints.Cron.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
