package com.umpay.commons.util;

import com.umpay.commons.annotation.SourceProperty;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.cglib.beans.BeanCopier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationConfigurationException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 * User: taige
 * Date: 2020/5/4
 * Time: 21:25
 */
@Component
public class BeanConverter implements ApplicationContextAware {
    private static final Logger LOGGER = new Logger();

    private static ApplicationContext applicationContext;

    private static ConcurrentMap<String, ValueOf<?, ?>> valueOfCache = new ConcurrentHashMap<>();

    private static ConcurrentMap<String, ConvertibleCopier<?, ?>> converterMappings = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    private static <F, T> ConvertibleCopier<F, T> getConverterMapping(Class<?> srcClass, Class<?> targetClass) {
        String mapKey = srcClass.getName() + ":" + targetClass.getName();
        return (ConvertibleCopier<F, T>) converterMappings.computeIfAbsent(mapKey,
                (Function<String, ConvertibleCopier<F, T>>) key -> {
            ConvertibleCopier<F, T> convertibleCopier = new ConvertibleCopier<>();
            Class<?>[] sourceClasses = null;
            if (targetClass.isAnnotationPresent(SourceProperty.class)) {
                SourceProperty sourceProperty = targetClass.getAnnotation(SourceProperty.class);
                if (sourceProperty.sourceClasses().length > 0) {
                    sourceClasses = sourceProperty.sourceClasses();
                }
            }
            for (Field field: ClassUtil.getAllFields(targetClass, new ArrayList<>())) {
                // if same classes, no convert or mapping, simply do copy
                boolean doConvertOrMapping = srcClass != targetClass;
                if (doConvertOrMapping && field.isAnnotationPresent(SourceProperty.class)) {
                    introspectField(field, srcClass, targetClass, convertibleCopier, sourceClasses);
                } else {
                    // temporarily save all properties mapping, will be remove if copied by BeanCopier
                    String targFieldName = field.getName();
                    PropertyDescriptor targPropDesc = BeanUtils.getPropertyDescriptor(targetClass, targFieldName);
                    PropertyDescriptor srcPropDesc = BeanUtils.getPropertyDescriptor(srcClass, targFieldName);
                    if (targPropDesc != null && srcPropDesc != null) {
                        convertibleCopier.nameMapping.put(targPropDesc, srcPropDesc);
                    }
                }
            }
            return convertibleCopier;
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <F, T> void introspectField(Field field, Class<?> srcClass, Class<?> targetClass,
                                               ConvertibleCopier<F, T> convertibleCopier, Class<?>[] defaultSourceClasses) {
        SourceProperty sourceProperty = field.getAnnotation(SourceProperty.class);
        if (! StringUtil.equals(sourceProperty.name(), sourceProperty.value())
                && StringUtil.areNotEmpty(sourceProperty.name(), sourceProperty.value())) {
            throw new AnnotationConfigurationException(String.format(
                    "Different @AliasFor mirror values for annotation [%s]declared on %s.%s; attribute " +
                            "'name' and its alias 'value' are declared with values of [%s] and [%s].",
                    SourceProperty.class.getName(), field.getDeclaringClass().getTypeName(), field.getName(),
                    ObjectUtils.nullSafeToString(sourceProperty.name()),
                    ObjectUtils.nullSafeToString(sourceProperty.value())));
        }
        String targFieldName = field.getName();
        String srcFieldName = targFieldName;
        if (StringUtil.isNotBlank(sourceProperty.name()) || StringUtil.isNotBlank(sourceProperty.value())) {
            srcFieldName = StringUtil.isNotBlank(sourceProperty.name()) ? sourceProperty.name() : sourceProperty.value();
            if ('A' <= srcFieldName.charAt(0) && srcFieldName.charAt(0) <= 'Z') {
                srcFieldName = srcFieldName.substring(0, 1).toLowerCase() + srcFieldName.substring(1);
            }
        }
        if (sourceProperty.sourceClasses().length > 0) {
            if (Arrays.stream(sourceProperty.sourceClasses())
                    .noneMatch(sourceClass -> sourceClass.isAssignableFrom(srcClass))) {
                return;
            }
        } else if (defaultSourceClasses != null) {
            if (Arrays.stream(defaultSourceClasses)
                    .noneMatch(defaultClass -> defaultClass.isAssignableFrom(srcClass))) {
                return;
            }
        }
        try {
            PropertyDescriptor targPropDesc = BeanUtils.getPropertyDescriptor(targetClass, targFieldName);
            if (targPropDesc == null) {
                if (sourceProperty.ignore()) {
                    return;
                }
                throw new IntrospectionException(
                        String.format("The target property `%s` is not defined correctly in class `%s`.",
                                targFieldName, targetClass.getName()));
            }
            String setterName = targPropDesc.getWriteMethod().getName();
            if (sourceProperty.ignore()) {
                convertibleCopier.converterMapping.put(setterName,
                        (srcValue, srcBean, srcProperty, targetBean, targetProperty) -> null);
                return;
            }
            PropertyDescriptor srcPropDesc = BeanUtils.getPropertyDescriptor(srcClass, srcFieldName);
            convertibleCopier.nameMapping.put(targPropDesc, srcPropDesc);
            if (sourceProperty.valueOf().length > 0) {
                Class<? extends ValueOf> cvtClass = sourceProperty.valueOf()[0];
                String[] params = sourceProperty.params();
                ValueOf<F, T> converter = (ValueOf<F, T>) valueOfCache.computeIfAbsent(cvtClass.getName(), s -> {
                    if (applicationContext != null) {
                        try {
                            if (StringUtil.isNotBlank(sourceProperty.qualifier())) {
                                return applicationContext.getBean(sourceProperty.qualifier(), cvtClass);
                            } else {
                                return applicationContext.getBean(cvtClass);
                            }
                        } catch (BeansException ignored) {
                        }
                    }
                    return BeanUtils.instantiateClass(cvtClass);
                });
                if (srcPropDesc == null && ! (converter instanceof ValueOfContext)) {
                    throw new IntrospectionException(
                            String.format("The source property `%s` is not defined correctly in class `%s`.",
                                    srcFieldName, srcClass.getName()));
                }
                convertibleCopier.converterMapping.put(setterName,
                        (srcValue, srcBean, srcProperty, targetBean, targetProperty) ->
                                converter.valueOf(srcValue, srcBean, srcProperty, targetBean, targetProperty, params));
            } else if (srcPropDesc == null) {
                throw new IntrospectionException(
                        String.format("The source property `%s` is not defined correctly in class `%s`.",
                                srcFieldName, srcClass.getName()));
            }
        } catch (IntrospectionException e) {
            throw new RuntimeIntrospectionException(e);
        }
    }

    /**
     * convert bean from srcBean to targetBean of class T:
     *    1. instantiate targetBean of class T;
     *    2. copy same name & type properties from srcBean to targetBean, see {@link BeanCopier}
     *    3. convert properties with annotation {@link SourceProperty} on class T,
     *          which specific the source property name {@link SourceProperty#name} and/or
     *          convert method {@link SourceProperty#valueOf} (class of {@link ValueOf}).
     *    4. other properties (not copied or converted by 2 & 3):
     *          will use {@link #toString()} as value if target property type is String,
     *          else will not be copied.
     * @param srcBean     Conversion source bean
     * @param targetClass Class of conversion target
     * @param <T>         Target bean type
     * @return            Conversion target bean
     * @throws RuntimeException when invoking target property setter,
     *                          properties/methods access denied
     *                          or properties definition incorrect
     */
    public static <T> T convert(Object srcBean, Class<T> targetClass) throws RuntimeException {
        if (srcBean == null) {
            return null;
        }
        Class<?> srcClass = srcBean.getClass();
        T targetBean = BeanUtils.instantiateClass(targetClass);
        try {
            ConvertibleCopier<?, T> converter = getConverterMapping(srcClass, targetClass);
            converter.copy(srcBean, targetBean);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return targetBean;
    }

    public static <T> T copy(Object srcBean, T targetBean) throws RuntimeException {
        if (srcBean == null) {
            return targetBean;
        }
        Class<?> srcClass = srcBean.getClass();
        Class<T> targetClass = (Class<T>) targetBean.getClass();
        try {
            ConvertibleCopier<?, T> converter = getConverterMapping(srcClass, targetClass);
            converter.copy(srcBean, targetBean);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return targetBean;
    }

    /**
     * try to get source property name if the property has {@link SourceProperty} annotation.
     *
     * @param srcClass      the source class
     * @param targetClass   the property name containing class (assumed)
     * @param propertyName  the property name querying
     * @return              the property name in source class if it defined, else null
     */
    @Nullable
    public static <F, T> String getSourcePropertyName(Class<F> srcClass, Class<T> targetClass, String propertyName) {
        ConvertibleCopier<F, T> converter = getConverterMapping(srcClass, targetClass);
        Optional<PropertyDescriptor> optional = converter.nameMapping.entrySet().stream()
                .filter(e -> e.getKey().getName().equals(propertyName))
                .findFirst().map(Map.Entry::getValue);
        if (! optional.isPresent()) {
            PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(srcClass, propertyName);
            if (pd == null) {
                return null;
            } else {
                return propertyName;
            }
        }
        return optional.get().getName();
    }

    @Override
    public void setApplicationContext(ApplicationContext appContext) throws BeansException {
        applicationContext = appContext;
        valueOfCache.clear();
        converterMappings.clear();
    }

    public static class RuntimeIntrospectionException extends RuntimeException {
        private final IntrospectionException introspectionException;
        public RuntimeIntrospectionException(IntrospectionException cause) {
            super(cause);
            this.introspectionException = cause;
        }

        public IntrospectionException getIntrospectionException() {
            return introspectionException;
        }
    }

    private static class ConvertibleCopier<F, T> {
        private BeanCopier copier;
        // target -> src
        final Map<PropertyDescriptor, PropertyDescriptor> nameMapping = new HashMap<>();
        final Map<String, ParamsValueOf<F, T>> converterMapping = new HashMap<>();
        final Set<Object> settersByCopier = new HashSet<>();

        public void copy(Object srcBean, Object targetBean) throws InvocationTargetException, IllegalAccessException {
            if (converterMapping.size() == 0 && nameMapping.size() == 0) {
                if (copier == null || settersByCopier.size() > 0) {
                    synchronized (this) {
                        if (copier == null || settersByCopier.size() > 0) {
                            // re-create `BeanCopier` because all copy-things finished without a `converter`
                            copier = BeanCopier.create(srcBean.getClass(), targetBean.getClass(), false);
                            settersByCopier.clear();
                        }
                    }
                }
                // copy bean using `BeanCopier` without converter
                copier.copy(srcBean, targetBean, null);
                return;
            } else if (copier == null) {
                synchronized (this) {
                    if (copier == null) {
                        copier = BeanCopier.create(srcBean.getClass(), targetBean.getClass(), true);
                    }
                }
            }
            // copy same name properties using `BeanCopier`
            copier.copy(srcBean, targetBean, (origValue, targetClass, targSetterName) -> {
                Object obj = convert(origValue, targetClass, targSetterName, targetBean, null, srcBean);
                if (! settersByCopier.contains(targSetterName)) {
                    synchronized (this) {
                        settersByCopier.add(targSetterName);
                        if (targSetterName instanceof String) {
                            // remove properties mapping because handled by BeanCopier
                            PropertyDescriptor propertyDescriptor =
                                    getPropertyDescriptor(targetBean, (String) targSetterName, targetClass);
                            nameMapping.remove(propertyDescriptor);
                        }
                    }
                }
                return obj;
            });
            // handle properties with name mapping
            for (Map.Entry<PropertyDescriptor, PropertyDescriptor> entry: nameMapping.entrySet()) {
                PropertyDescriptor targProperty = entry.getKey();
                PropertyDescriptor srcProperty = entry.getValue();
                Class<?> targetClass = targProperty.getPropertyType();
                targProperty.getWriteMethod().invoke(targetBean,
                        convert(srcProperty == null ? null : srcProperty.getReadMethod().invoke(srcBean),
                                targetClass, targProperty.getWriteMethod().getName(), targetBean,
                                srcProperty == null ? null : srcProperty.getName(), srcBean)
                );
            }
        }

        @NonNull
        private PropertyDescriptor getPropertyDescriptor(Object targetBean, String targSetterName, Class<?> targetClass) {
            Method method = BeanUtils.findMethod(targetBean.getClass(), targSetterName, targetClass);
            assert method != null;  // Shouldn't happen
            PropertyDescriptor propertyDescriptor = BeanUtils.findPropertyForMethod(method);
            assert propertyDescriptor != null; // Shouldn't happen ?
            return propertyDescriptor;
        }

        @SuppressWarnings("unchecked")
        private Object convert(Object origValue, Class<?> targetClass, Object targSetterName, Object targetBean,
                               String srcPropertyName, Object srcBean) {
            if (! (targSetterName instanceof String)) {
                if (origValue == null || targetClass.isAssignableFrom(origValue.getClass())) {
                    return origValue;
                }
                LOGGER.warn("targSetterName(%s) expected a String, but instanceof %s, converter can't work",
                        targSetterName, targSetterName.getClass().getName());
                return null;
            }
            PropertyDescriptor propertyDescriptor = getPropertyDescriptor(targetBean, (String) targSetterName, targetClass);
            String targFieldName = propertyDescriptor.getName();
            LOGGER.trace("converting property=`%s`, origValue='%s'(%s), targetClass=%s, targSetterName=%s",
                    targFieldName, origValue, origValue == null ? "" : origValue.getClass().getName(), targetClass.getName(), targSetterName);
            if (converterMapping.containsKey(targSetterName)) {
                return valueOf(origValue, targetClass, (String) targSetterName, targetBean, srcPropertyName, srcBean, targFieldName);
            } else if (origValue == null) {
                return null;
            } else if (targetClass.isAssignableFrom(origValue.getClass())) {
                if (Collection.class.isAssignableFrom(targetClass)) {
                    return convertCollection((Collection<Object>) origValue, srcBean,
                            srcPropertyName == null ? targFieldName : srcPropertyName,
                            targetBean, targFieldName, (Class<Collection<Object>>) targetClass);
                }
                // TODO supoort Map
                return origValue;
            } else if (Number.class.isAssignableFrom(targetClass)
                    && Number.class.isAssignableFrom(origValue.getClass())) {
                // todo support primitive type
                // todo test
                return number2SpecificClass((Number) origValue, (Class<Number>) targetClass);
            } else if (targetClass.isAssignableFrom(String.class)) {
                LOGGER.trace("default convert everything to String(%s) for property `%s`", origValue, targFieldName);
                return origValue.toString();
            } else {
                try {
                    return BeanConverter.convert(origValue, targetClass);
                } catch (Exception e) {
                    LOGGER.warn("convert property `%s` from %s to %s with value '%s' failed: ", targFieldName,
                            origValue.getClass().getName(), targetClass.getName(), origValue, e);
                }
            }
            LOGGER.info("didn't convert property `%s` from %s to %s with value '%s'", targFieldName,
                    origValue.getClass().getName(), targetClass.getName(), origValue);
            return null;
        }

        @SuppressWarnings("unchecked")
        private Object valueOf(@Nullable Object origValue, Class<?> targetClass, String targSetterName,
                               Object targetBean, String srcPropertyName, Object srcBean, String targFieldName) {
            Object targValue;
            ParamsValueOf<F, T> valueOf = converterMapping.get(targSetterName);
            try {
                targValue = valueOf.paramsValueOf((F) origValue, srcBean,
                        srcPropertyName == null ? targFieldName : srcPropertyName, targetBean, targFieldName);
            } catch (ClassCastException e) {
                LOGGER.warn("FROM Cast Error: call %s for property `%s` fail: %s",
                        valueOf.getClass().getName(), targFieldName, e.getMessage());
                return null;
            } catch (NullPointerException e) {
                if (origValue == null) {
                    return null;
                }
                throw e;
            }
            if (targValue != null && Number.class.isAssignableFrom(targetClass)
                    && Number.class.isAssignableFrom(targValue.getClass())) {
                targValue = number2SpecificClass((Number) targValue, (Class<Number>) targetClass);
            }
            if (targValue != null && ! targetClass.isAssignableFrom(targValue.getClass())) {
                LOGGER.warn("TO Case Error: property `%s` is %s, but got %s(%s)",
                        targFieldName, targetClass.getName(), targValue.getClass().getName(), targValue);
                return null;
            }
            return targValue;
        }

    }

    @SuppressWarnings("unchecked")
    private static Collection<Object> convertCollection(Collection<Object> srcValue, Object srcBean, String srcProperty, Object targetBean, String targetProperty, Class<Collection<Object>> targetClass) {
        try {
            Type srcTypes = ((ParameterizedType) srcBean.getClass().getDeclaredField(
                    srcProperty == null ? targetProperty : srcProperty).getGenericType()).getActualTypeArguments()[0];
            Type targTypes = ((ParameterizedType) targetBean.getClass().getDeclaredField(
                    targetProperty).getGenericType()).getActualTypeArguments()[0];
            if (! (srcTypes instanceof Class) || ! (targTypes instanceof Class)) {
                return srcValue;
            }
            Collection<Object> targCollection = instantiateCollection(targetClass); //BeanUtils.instantiateClass(targetClass);
            for (Object src: srcValue) {
                Object targ = BeanConverter.convert(src, (Class<Object>) targTypes);
                targCollection.add(targ);
            }
            return targCollection;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private static Collection<Object> instantiateCollection(Class<Collection<Object>> collectionClass) {
        if (Modifier.isInterface(collectionClass.getModifiers())) {
            if (List.class.isAssignableFrom(collectionClass)) {
                return new ArrayList<>();
            } else if (Set.class.isAssignableFrom(collectionClass)) {
                return new HashSet<>();
            }
            // TODO support Queue
            throw new UnsupportedOperationException(String.format("unsupported collection interface: %s", collectionClass.getName()));
        } else if (Modifier.isAbstract(collectionClass.getModifiers())) {
            // TODO ?
            throw new UnsupportedOperationException(String.format("unsupported abstract collection: %s", collectionClass.getName()));
        }
        return BeanUtils.instantiateClass(collectionClass);
    }

    private static Number number2SpecificClass(Number n, Class<? extends Number> nClass) {
        if (nClass == Double.class) {
            return n.doubleValue();
        } else if (nClass == Float.class) {
            return n.floatValue();
        } else if (nClass == Long.class) {
            return n.longValue();
        } else if (nClass == Integer.class) {
            return n.intValue();
        } else if (nClass == Short.class) {
            return n.shortValue();
        } else if (nClass == Byte.class) {
            return n.byteValue();
        } else if (nClass == BigDecimal.class) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        LOGGER.warn("BeanConverter.number2SpecificClass didn't really convert %s(%s) -> %s, may cause exception",
                n.getClass().getName(), n, nClass.getName());
        return n;
    }

    @FunctionalInterface
    interface ParamsValueOf<F, T> {
        T paramsValueOf(F srcValue, Object srcBean, String srcProperty, Object targetBean, String targetProperty);
    }

}
