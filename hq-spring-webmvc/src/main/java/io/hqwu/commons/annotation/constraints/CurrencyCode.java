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
 * ISO 4217 货币代码格式校验注解。
 * <p>
 * 用于标注在字段或方法参数上，校验字段值是否为合法的 ISO 4217 货币代码（如 USD、EUR、CNY 等）。
 * 该注解基于 Bean Validation 框架，通过 {@link CurrencyCodeValidator} 实现具体的校验逻辑。
 * </p>
 * <p>
 * 支持通过 {@link #allowEmpty()} 配置是否允许空字符串通过校验。
 * </p>
 * <p>
 * 示例：
 * <pre>
 * public class PaymentRequest {
 *     &#64;CurrencyCode
 *     private String currency;
 *
 *     &#64;CurrencyCode(allowEmpty = true)
 *     private String optionalCurrency;
 * }
 * </pre>
 *
 * @author taige (Wu, Hongqiang)
 * @see CurrencyCodeValidator
 * @see java.util.Currency
 * @see Constraint
 * @see jakarta.validation.ConstraintValidator
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
