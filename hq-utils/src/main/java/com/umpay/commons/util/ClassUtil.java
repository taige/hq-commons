package com.umpay.commons.util;

import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: wyshenjianlin
 * Date: 13-9-11
 * Time: 下午3:22
 */
public class ClassUtil extends ClassUtils {
    private static final String[] SDK_PACKAGES_PREFIX = {"java", "sun.", "oracle.", "org.springframework."};

    public static String getPackageAsPath(Class<?> cls, String fileName) {
        String packagePath = getPackageAsPath(cls);
        if (StringUtil.isEmpty(fileName)) {
            return packagePath;
        } else {
            return new StringBuilder(getPackageAsPath(cls)).append("/").append(fileName).toString();
        }
    }

    public static String getPackageAsPath(Class<?> cls) {
        return cls == null ? "" : StringUtil.replace(cls.getPackage().getName(), ".", "/");
    }

    /**
     * 类的包名是否是JDK内置的
     *
     * @param clazz 请求参数类型
     * @return true 如果类的包名是否是JDK内置的
     */
    public static boolean isSdkPackage(Class<?> clazz) {
        // add by shenjl at 2014-7-24 针对int、long等基础类型
        if(clazz == null || clazz.getPackage() == null){
            return true;
        }
        final String packageName = clazz.getPackage().getName();
        for (String prefix : SDK_PACKAGES_PREFIX) {
            if (packageName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static List<Field> getAllFields(Class<?> targetClass, List<Field> fields) {
        for (Field field: targetClass.getDeclaredFields()) {
            if (fields.stream().anyMatch(f -> f.getName().equals(field.getName()))) {
                continue;
            }
            fields.add(field);
        }
        if (targetClass.getSuperclass() != Object.class) {
            return getAllFields(targetClass.getSuperclass(), fields);
        } else {
            return fields;
        }
    }

}
