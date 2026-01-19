package io.hqwu.commons.jackson;

/**
 * JSON 对象校验标识接口。
 * <p>
 * 该接口用于标识需要进行 Bean Validation 校验的 JSON 对象。
 * 实现此接口的类在通过 Jackson 进行 JSON 反序列化时，会被 {@link ValidatedDeserializer}
 * 自动执行 JSR-380 Bean Validation 校验。
 * </p>
 *
 * <p>
 * 功能特性：
 * <ul>
 *   <li>支持 Bean Validation 校验分组（Validation Groups）功能</li>
 *   <li>通过 {@link #validateGroups()} 方法可指定额外的校验分组</li>
 *   <li>与 {@link ValidatedDeserializer} 配合使用，实现 JSON 反序列化时的自动校验</li>
 * </ul>
 * </p>
 *
 * <p>
 * 使用示例：
 * <pre>
 * public class UserRequest implements ValidatedJson {
 *     &#64;NotBlank
 *     private String username;
 *
 *     &#64;NotBlank(groups = UpdateGroup.class)
 *     private String userId;
 *
 *     &#64;Override
 *     public Class&lt;?&gt;[] validateGroups() {
 *         return new Class&lt;?&gt;[]{ UpdateGroup.class };
 *     }
 * }
 * </pre>
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see ValidatedDeserializer
 * @see ValidatedJsonResponse
 * @see ViolationExceptionFactory
 * @see jakarta.validation.Validator
 * Date: 2021-05-04
 * Time: 4:48 p.m.
 */
public interface ValidatedJson {

    default Class<?>[] validateGroups() {
        return null;
    }

}
