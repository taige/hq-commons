package io.hqwu.commons.annotation.constraints;


import io.hqwu.commons.annotation.constraints.support.StringTimeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 日期时间格式校验注解。
 * <p>
 * 用于标注在字段或参数上，校验字符串是否符合指定的日期时间格式模式。
 * 该注解基于 Bean Validation 框架，通过 {@link StringTimeValidator} 实现具体的校验逻辑。
 * </p>
 * <p>
 * 校验规则：
 * <ul>
 *   <li>空字符串或 null 值视为校验通过</li>
 *   <li>非空字符串必须严格匹配指定的日期时间格式模式</li>
 *   <li>校验通过条件：字符串解析后重新格式化的结果与原字符串完全一致</li>
 * </ul>
 * </p>
 * <p>
 * 使用示例：
 * <pre>
 * public class OrderInfo {
 *     // 校验日期格式
 *     &#64;StringTime("yyyy-MM-dd")
 *     private String orderDate;  // 正确: "2024-01-15"  错误: "2024-1-15"
 *
 *     // 校验日期时间格式
 *     &#64;StringTime("yyyy-MM-dd HH:mm:ss")
 *     private String createTime;  // 正确: "2024-01-15 10:30:00"  错误: "2024-01-15 10:30:0"
 *
 *     // 校验时间格式
 *     &#64;StringTime("HH:mm")
 *     private String startTime;  // 正确: "09:30"  错误: "9:30"
 * }
 *
 * public void processOrder(&#64;StringTime("yyyy-MM-dd") String deliveryDate) {
 *     // 方法参数校验
 * }
 * </pre>
 *
 * @author taige (Wu, Hongqiang)
 * @see StringTimeValidator
 * @see Constraint
 * @see jakarta.validation.ConstraintValidator
 * @see java.time.format.DateTimeFormatter
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
