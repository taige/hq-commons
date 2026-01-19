package io.hqwu.commons.annotation.constraints;


import io.hqwu.commons.annotation.constraints.support.Base64Validator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Base64 编码格式校验注解。
 * <p>
 * 用于标注在字段上，校验字段值是否为合法的 Base64 编码字符串。
 * 该注解基于 Bean Validation 框架，通过 {@link Base64Validator} 实现具体的校验逻辑。
 * </p>
 * <p>
 * 示例：
 * <pre>
 * public class ImageData {
 *     &#64;Base64
 *     private String imageBase64;
 * }
 * </pre>
 *
 * @author taige (Wu, Hongqiang)
 * @see Base64Validator
 * @see Constraint
 * @see jakarta.validation.ConstraintValidator
 */
@Constraint(validatedBy = Base64Validator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Base64 {

    String message() default "{io.hqwu.commons.annotation.constraints.Base64.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
