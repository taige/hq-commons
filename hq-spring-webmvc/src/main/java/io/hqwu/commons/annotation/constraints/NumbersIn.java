package io.hqwu.commons.annotation.constraints;


import io.hqwu.commons.annotation.constraints.support.NumbersInArrayValidator;
import io.hqwu.commons.annotation.constraints.support.NumbersInValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 数值范围校验注解。
 * <p>
 * 用于标注在字段或参数上，校验数值（或数值数组）是否在指定的候选值集合中。
 * 该注解基于 Bean Validation 框架，支持单个 {@link Number} 类型值和 {@link Number} 数组的校验。
 * </p>
 * <p>
 * 校验规则：
 * <ul>
 *   <li>单个数值：校验该数值是否存在于注解指定的候选值集合中</li>
 *   <li>数值数组：校验数组中是否至少有一个元素存在于候选值集合中</li>
 *   <li>null 值视为校验通过</li>
 *   <li>校验时将被校验的 {@link Number} 转换为 long 类型进行匹配</li>
 * </ul>
 * </p>
 * <p>
 * 使用示例：
 * <pre>
 * public class OrderInfo {
 *     // 校验订单状态是否在指定范围内
 *     &#64;NumbersIn({1, 2, 3, 4, 5})
 *     private Integer status;  // 正确: 1, 2, 3, 4, 5  错误: 6, 0
 *
 *     // 校验支付方式数组是否包含至少一个指定的支付方式
 *     &#64;NumbersIn({1, 2, 3})
 *     private Integer[] paymentMethods;  // 正确: [1, 4], [2], [1, 2, 3]  错误: [4, 5]
 * }
 *
 * public void processOrder(&#64;NumbersIn({10, 20, 30}) Long amount) {
 *     // 方法参数校验
 * }
 * </pre>
 *
 * @author taige (Wu, Hongqiang)
 * @see NumbersInValidator
 * @see NumbersInArrayValidator
 * @see Constraint
 * @see jakarta.validation.ConstraintValidator
 * Date: 2020/5/9
 * Time: 13:21
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {NumbersInValidator.class, NumbersInArrayValidator.class})
@Documented
public @interface NumbersIn {

    long[] value();

    String message() default "{io.hqwu.commons.annotation.constraints.NumbersIn.message}";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
