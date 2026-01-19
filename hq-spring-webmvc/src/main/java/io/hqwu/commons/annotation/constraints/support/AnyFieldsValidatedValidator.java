package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.annotation.constraints.AnyFieldsValidated;
import io.hqwu.commons.util.ClassUtil;
import io.hqwu.commons.util.Logger;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static io.hqwu.commons.annotation.constraints.AnyFieldsValidated.AnyField;


/**
 * {@link AnyFieldsValidated} 注解的校验器实现。<br/>
 *
 * 用于校验对象中被 {@link AnyField} 标注的字段：至少有一个字段满足指定约束 <br/>
 * （{@link NotBlank}、{@link NotEmpty} 或 {@link NotNull}）。<br/>
 *
 * 支持按校验组（groups）过滤参与校验的字段；若所有相关字段均不满足约束，则构造相应的违例信息。<br/>
 *
 * @author taige (Wu, Hongqiang)
 * @see AnyFieldsValidated
 * @see AnyField
 * @see ConstraintValidator
 * @see NotBlank
 * @see NotEmpty
 * @see NotNull
 * Date: 2021-11-16
 * Time: 11:11
 */
public class AnyFieldsValidatedValidator implements ConstraintValidator<AnyFieldsValidated, Object> {
    private static final Logger LOGGER = new Logger();

    private static ConcurrentMap<Class<?>, List<Field>> fieldCache = new ConcurrentHashMap<>();

    private Class<? extends Annotation> constraintsClass;

    @Override
    public void initialize(AnyFieldsValidated constraintAnnotation) {
        constraintsClass = constraintAnnotation.constraint();
        if (NotBlank.class != constraintsClass && NotEmpty.class != constraintsClass && NotNull.class != constraintsClass) {
            throw new UnsupportedOperationException("unsupported validFor class: " + constraintsClass);
        }
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext context) {
        if (object == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("this object requires not null")
                    .addConstraintViolation();
            return false;
        }
        try {
            Set<Class<?>> activeGroups = ((ConstraintValidatorContextImpl) context).getConstraintDescriptor().getGroups();
            List<Field> fields = fieldCache.computeIfAbsent(object.getClass(), clazz -> {
                List<Field> field = ClassUtil.getAllFields(clazz, new ArrayList<>());
                return field.stream().filter(f -> f.isAnnotationPresent(AnyField.class)).collect(Collectors.toList());
            });
            List<String> violFields = new ArrayList<>();
            for (Field field : fields) {
                AnyField anyField = field.getAnnotation(AnyField.class);
                if (Arrays.stream(anyField.groups()).noneMatch(activeGroups::contains)) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(object);
                if (NotBlank.class == constraintsClass) {
                    if (value instanceof CharSequence && value.toString().trim().length() > 0) {
                        return true;
                    }
                } else if (constraintsClass == NotEmpty.class) {
                    if (value instanceof CharSequence && ((CharSequence) value).length() > 0) {
                        return true;
                    } else if (value instanceof Collection && ((Collection) value).size() > 0) {
                        return true;
                    } else if (value instanceof Map && ((Map) value).size() > 0) {
                        return true;
                    }
                } else if (value != null) {
                    return true;
                }
                violFields.add(field.getName());
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(violFields.stream().collect(
                            Collectors.joining(",", "one of fields: '", "' must " + constraintsClass.getSimpleName())))
                    .addBeanNode()
                    .addConstraintViolation();
        } catch (Exception e) {
            LOGGER.warn("unexpected error on AnyFieldsValidatedValidator.isValid: %s", e.toString());
        }
        return false;
    }

}
