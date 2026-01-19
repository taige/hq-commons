package io.hqwu.commons.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.io.IOException;
import java.util.Set;

/**
 * 带有验证功能的 JSON 反序列化器。
 * <p>
 * 该类扩展了 Jackson 的 {@link BeanDeserializer}，在反序列化 JSON 对象后自动执行 Bean Validation 校验。
 * 主要用于在 JSON 数据转换为 Java 对象时，确保对象满足指定的约束条件（如 {@code @NotNull}、{@code @Size} 等）。
 * </p>
 *
 * <p>
 * 功能特性：
 * <ul>
 *   <li>在反序列化完成后自动触发 JSR-380 Bean Validation 校验</li>
 *   <li>支持校验分组（Validation Groups）功能，通过 {@link ValidatedJson#validateGroups()} 指定</li>
 *   <li>对于 {@link ValidatedJsonResponse} 类型的对象，当其标识为错误状态时会跳过校验</li>
 *   <li>校验失败时通过 {@link ViolationExceptionFactory} 生成自定义异常</li>
 * </ul>
 * </p>
 *
 * <p>
 * 使用示例：
 * <pre>
 * // 配置 ObjectMapper 使用该反序列化器
 * ObjectMapper mapper = new ObjectMapper();
 * // ... 注册 ValidatedDeserializer ...
 *
 * // JSON 反序列化时会自动校验
 * MyValidatedObject obj = mapper.readValue(jsonString, MyValidatedObject.class);
 * </pre>
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see BeanDeserializer
 * @see ValidatedJson
 * @see ValidatedJsonResponse
 * @see ViolationExceptionFactory
 * @see jakarta.validation.Validator
 * Date: 2020/5/31
 * Time: 11:19
 */
public class ValidatedDeserializer extends BeanDeserializer {

    private final Validator validator;
    private final ViolationExceptionFactory exceptionFactory;

    public ValidatedDeserializer(BeanDeserializerBase deserializer,
                                 Validator validator,
                                 ViolationExceptionFactory exceptionFactory) {
        super(deserializer);
        this.validator = validator;
        this.exceptionFactory = exceptionFactory;
    }

    @Override
    public ValidatedJson deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ValidatedJson request = (ValidatedJson) rawDeserialize(jp, ctxt);
        // validate response
        validate(request);
        return request;
    }

    protected Object rawDeserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        return super.deserialize(jp, ctxt);
    }

    protected void validate(ValidatedJson object) {
        if (object instanceof ValidatedJsonResponse) {
            if (((ValidatedJsonResponse) object).hasError()) {
                // 不成功的response，skip validate
                return;
            }
        }
        Set<ConstraintViolation<Object>> violations = validator.validate(object);
        if (violations.size() > 0) {
            throw exceptionFactory.newViolationException(violations, object);
        }
        Class<?>[] groups = object.validateGroups();
        if (groups != null && groups.length > 0) {
            violations = validator.validate(object, groups);
            if (violations.size() > 0) {
                throw exceptionFactory.newViolationException(violations, object);
            }
        }
    }

}
