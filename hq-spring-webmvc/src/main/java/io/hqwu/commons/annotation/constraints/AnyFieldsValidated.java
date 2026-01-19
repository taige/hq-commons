package io.hqwu.commons.annotation.constraints;

import io.hqwu.commons.annotation.constraints.support.AnyFieldsValidatedValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.groups.Default;

import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 任意字段校验注解。<br/>
 * <p>
 * 用于标注在类上，校验该类中被 {@link AnyField} 标注的字段中，至少有一个字段满足指定的约束条件。<br/>
 * 支持的约束包括：{@link jakarta.validation.constraints.NotBlank}、
 * {@link jakarta.validation.constraints.NotEmpty}、{@link jakarta.validation.constraints.NotNull}。<br/>
 * <p>
 * 该注解支持通过 {@link Repeatable} 机制在同一个类上重复使用，以实现不同校验组的多重校验逻辑。<br/>
 * <p>
 * 示例：
 * <pre>
 * &#64;AnyFieldsValidated(constraint = NotBlank.class, message = "至少填写一个联系方式")
 * public class ContactInfo {
 *     &#64;AnyField
 *     private String phone;
 *
 *     &#64;AnyField
 *     private String email;
 * }
 * </pre>
 *
 * @author taige (Wu, Hongqiang)
 * @see AnyFieldsValidatedValidator
 * @see AnyField
 * @see jakarta.validation.constraints.NotBlank
 * @see jakarta.validation.constraints.NotEmpty
 * @see jakarta.validation.constraints.NotNull
 * @see Constraint
 * @see Repeatable
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
