package io.hqwu.commons.jackson;

import jakarta.validation.ConstraintViolation;

import java.util.Set;

/**
 * 校验异常工厂接口。
 * <p>
 * 该接口用于创建 Bean Validation 校验失败时抛出的运行时异常。
 * 当 {@link ValidatedDeserializer} 在反序列化 JSON 对象时检测到校验约束违规，
 * 会通过此工厂接口生成相应的异常对象。
 * </p>
 *
 * <p>
 * 功能特性：
 * <ul>
 *   <li>作为函数式接口，支持 Lambda 表达式和方法引用</li>
 *   <li>接收校验违规信息集合 ({@link ConstraintViolation}) 和被校验的 JSON 对象</li>
 *   <li>允许开发者自定义校验失败时的异常类型和错误信息格式</li>
 *   <li>与 {@link ValidatedDeserializer} 配合使用，实现灵活的异常处理策略</li>
 * </ul>
 * </p>
 *
 * <p>
 * 使用示例：
 * <pre>
 * ViolationExceptionFactory factory = (violations, json) -&gt; {
 *     String message = violations.stream()
 *         .map(ConstraintViolation::getMessage)
 *         .collect(Collectors.joining(", "));
 *     return new IllegalArgumentException("校验失败: " + message);
 * };
 * </pre>
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see ValidatedDeserializer
 * @see ValidatedJson
 * @see ValidatedJsonResponse
 * @see jakarta.validation.ConstraintViolation
 * @see jakarta.validation.Validator
 * Date: 2021-09-23
 * Time: 20:43
 */
@FunctionalInterface
public interface ViolationExceptionFactory {

    RuntimeException newViolationException(Set<? extends ConstraintViolation<?>> constraintViolations, ValidatedJson violatedJson);

}
