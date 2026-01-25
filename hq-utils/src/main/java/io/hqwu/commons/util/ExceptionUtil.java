package io.hqwu.commons.util;

import lombok.experimental.UtilityClass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * 异常工具类。
 * <p>
 * 该工具类提供异常处理相关的实用方法，主要用于解包嵌套的异常对象，
 * 以获取真实的异常根源。在反射调用和代理场景中，异常通常会被包装在
 * {@link InvocationTargetException} 或 {@link UndeclaredThrowableException} 中，
 * 此工具类可以递归解包这些包装异常，返回最底层的原始异常。
 * </p>
 *
 * <p>
 * 主要功能特性：
 * <ul>
 *   <li>支持解包 {@link InvocationTargetException} 异常。</li>
 *   <li>支持解包 {@link UndeclaredThrowableException} 异常。</li>
 *   <li>递归处理多层嵌套的包装异常，直至获取真实异常。</li>
 * </ul>
 * </p>
 *
 * <p>
 * 使用示例：
 * <pre>{@code
 * try {
 *     // 反射调用可能抛出 InvocationTargetException
 *     method.invoke(obj, args);
 * } catch (Exception e) {
 *     Throwable realException = ExceptionUtil.unwrapThrowable(e);
 *     // 处理真实的异常
 * }
 * }</pre>
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see InvocationTargetException
 * @see UndeclaredThrowableException
 * @since 2021-03-30
 */
@UtilityClass
public class ExceptionUtil {

    public static Throwable unwrapThrowable(Throwable wrapped) {
        Throwable unwrapped = wrapped;
        while (true) {
            if (unwrapped instanceof InvocationTargetException) {
                unwrapped = ((InvocationTargetException) unwrapped).getTargetException();
            } else if (unwrapped instanceof UndeclaredThrowableException) {
                unwrapped = ((UndeclaredThrowableException) unwrapped).getUndeclaredThrowable();
            } else {
                return unwrapped;
            }
        }
    }

}
