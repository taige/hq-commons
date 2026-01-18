package io.hqwu.commons.annotation.constraints;


import io.hqwu.commons.annotation.constraints.support.Base64Validator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = Base64Validator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Base64 {

    String message() default "{io.hqwu.commons.annotation.constraints.Base64.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
