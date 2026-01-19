package io.hqwu.commons.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.hqwu.commons.util.StringUtil;
import jakarta.validation.ConstraintViolation;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Field;
import java.util.stream.StreamSupport;

/**
 * Created with IntelliJ IDEA for hq-commons-parent
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-11-16
 * Time: 21:07
 */
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
