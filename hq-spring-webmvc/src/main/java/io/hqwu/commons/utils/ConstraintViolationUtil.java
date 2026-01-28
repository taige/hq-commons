package io.hqwu.commons.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.hqwu.commons.util.StringUtil;
import jakarta.validation.ConstraintViolation;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Field;
import java.util.stream.StreamSupport;

/**
 * Bean Validation 约束校验工具类。
 * <p>
 * 该工具类用于将 {@link ConstraintViolation} 格式化为易读的错误消息字符串。
 * 主要功能特性：
 * <ul>
 *   <li>属性名映射：尝试解析字段上的 {@link JsonProperty} 注解，优先返回 JSON 属性名而非 Java 字段名。</li>
 *   <li>数组可视化：支持对数组类型的非法值进行字符串转换（使用 {@link ArrayUtils#toString}）。</li>
 *   <li>调试支持：通过 {@code debug} 参数控制是否在输出中包含完整的类路径信息。</li>
 * </ul>
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see ConstraintViolation
 * @see JsonProperty
 * @see StringUtil
 * @since 2021-11-16
 */
@UtilityClass
public class ConstraintViolationUtil {

    public static String toString(ConstraintViolation<?> violation, boolean debug) {
        StringBuilder msg = new StringBuilder();
        StreamSupport.stream(violation.getPropertyPath().spliterator(), false)
                .reduce((first, second) -> second).ifPresent(node -> {
                    Class<?> clz = violation.getLeafBean().getClass();
                    String fieldName = node.getName();
                    if (StringUtil.isNotBlank(fieldName)) {
                        try {
                            Field field = clz.getDeclaredField(fieldName);
                            JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
                            if (jsonProperty != null) {
                                String jv = jsonProperty.value();
                                if (jv.trim().length() > 0) {
                                    fieldName = jv;
                                }
                            }
                        } catch (NoSuchFieldException ignored) {
                        }
                        if (debug) {
                            fieldName = clz.getName() + "." + fieldName;
                        }
                    } else {
                        fieldName = debug ? clz.getName() : clz.getSimpleName();
                    }
                    Object invalidValue = violation.getInvalidValue();
                    if (invalidValue != null && invalidValue.getClass().isArray()) {
                        invalidValue = ArrayUtils.toString(invalidValue);
                    }
                    msg.append(String.format("`%s` %s (got:%s)",
                            fieldName, violation.getMessage(), invalidValue));
                });
        return msg.toString();
    }

}
