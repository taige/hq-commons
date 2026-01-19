package io.hqwu.commons.jackson;

/**
 * JSON 响应对象校验标识接口。
 * <p>
 * 该接口继承自 {@link ValidatedJson}，专门用于标识需要进行 Bean Validation 校验的 JSON 响应对象。
 * 在通过 Jackson 进行 JSON 反序列化时，会被 {@link ValidatedDeserializer} 自动执行 JSR-380 Bean Validation 校验。
 * </p>
 *
 * <p>
 * 功能特性：
 * <ul>
 *   <li>继承 {@link ValidatedJson} 的所有校验功能，包括支持校验分组</li>
 *   <li>通过 {@link #hasError()} 方法标识响应对象是否处于错误状态</li>
 *   <li>当响应对象处于错误状态时，{@link ValidatedDeserializer} 会跳过 Bean Validation 校验</li>
 *   <li>适用于需要区分成功/失败状态的 API 响应对象</li>
 * </ul>
 * </p>
 *
 * <p>
 * 使用示例：
 * <pre>
 * public class ApiResponse implements ValidatedJsonResponse {
 *     private boolean success;
 *
 *     &#64;NotNull
 *     private String data;
 *
 *     &#64;Override
 *     public boolean hasError() {
 *         return !success;
 *     }
 * }
 * </pre>
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see ValidatedJson
 * @see ValidatedDeserializer
 * @see ViolationExceptionFactory
 * @see jakarta.validation.Validator
 * Date: 2021-05-05
 * Time: 9:49 a.m.
 */
public interface ValidatedJsonResponse extends ValidatedJson {

    boolean hasError();

}
