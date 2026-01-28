package io.hqwu.commons.util;

import lombok.experimental.UtilityClass;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 日志工厂类。
 * <p>
 * 该工具类提供了获取 {@link Logger} 实例的静态方法，支持通过类名、{@link Class} 对象或默认方式创建日志记录器。
 * 主要功能特性：
 * <ul>
 *   <li>缓存管理：使用 {@link ConcurrentHashMap} 缓存已创建的 Logger 实例，避免重复创建。</li>
 *   <li>线程安全：基于 {@link java.util.concurrent.ConcurrentMap} 实现的缓存机制保证并发环境下的线程安全。</li>
 *   <li>工具类设计：使用 {@link lombok.experimental.UtilityClass} 注解标注为工具类，禁止实例化。</li>
 * </ul>
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see Logger
 * @see java.util.concurrent.ConcurrentHashMap
 * @since 2013-08-20
 */
@UtilityClass
public class LoggerFactory {

    private static final ConcurrentMap<String, Logger> LOGGERS = new ConcurrentHashMap<>();

    public static Logger getLogger() {
        return new Logger(LoggerFactory.class);
    }

    public static Logger getLogger(String className) {
        return LOGGERS.computeIfAbsent(className, Logger::new);
    }

    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

}
