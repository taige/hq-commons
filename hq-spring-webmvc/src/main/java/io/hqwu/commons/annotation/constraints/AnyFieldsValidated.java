package io.hqwu.commons.annotation.constraints;

import io.hqwu.commons.annotation.constraints.support.AnyFieldsValidatedValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.groups.Default;

import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created with IntelliJ IDEA for hq-commons-parent
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-11-16
 * Time: 11:10
 */
@Target({ElementType.TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {AnyFieldsValidatedValidator.class})
@Repeatable(AnyFieldsValidated.List.class)
@Documented
public @interface AnyFieldsValidated {

    Class<? extends Annotation> constraint();

    String message() default "none of fields valid";

    /**
     * should specify at most one group class.
     * 最多指定一个group class，标注多个 {@link AnyFieldsValidated} 来实现多个group
     * @return
     */
    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    @Target({ElementType.TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface List {
        AnyFieldsValidated[] value();
    }

    @Target({ElementType.FIELD})
    @Retention(RUNTIME)
    @Documented
    @interface AnyField {
        Class<?>[] groups() default { Default.class };
    }
}
