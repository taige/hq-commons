package io.hqwu.commons.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 类工具类。
 * <p>
 * 该工具类继承自 {@link org.apache.commons.lang3.ClassUtils}，提供了类操作的扩展功能，
 * 包括类包路径转换、SDK包判断、字段获取等功能。
 * </p>
 * <p>
 * 主要功能特性：
 * <ul>
 *   <li>包路径转换：将类的包名转换为文件系统路径格式（使用 "/" 分隔）。</li>
 *   <li>SDK包判断：判断指定类是否属于JDK内置包或框架核心包。</li>
 *   <li>字段获取：递归获取类及其父类的所有声明字段。</li>
 * </ul>
 * </p>
 *
 * @author wyshenjianlin
 * @see org.apache.commons.lang3.ClassUtils
 * @see java.lang.reflect.Field
 * @see io.hqwu.commons.util.StringUtil
 * @since 2013-09-11
 */
@UtilityClass
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
