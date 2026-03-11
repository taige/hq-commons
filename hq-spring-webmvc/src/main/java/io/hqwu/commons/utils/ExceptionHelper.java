package io.hqwu.commons.utils;

import io.hqwu.commons.util.StringUtil;
import jakarta.validation.ConstraintViolation;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

/**
 * 异常处理辅助工具类
 * <p>
 * 提供异常信息格式化、根异常消息提取等功能，支持根据运行环境的调试模式
 * 输出不同详细程度的异常信息。该类实现了{@link EnvironmentAware}接口，
 * 可自动获取Spring环境配置中的调试模式设置。
 * </p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>格式化Bean Validation约束违规信息</li>
 *   <li>提取异常链中根异常的消息</li>
 *   <li>根据调试模式控制异常信息的详细程度</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>统一处理应用程序中的异常信息格式化</li>
 *   <li>在开发环境输出详细异常信息，生产环境输出简化信息</li>
 *   <li>提取异常链中的关键错误信息</li>
 * </ul>
 *
 * @author taige (Wu, Hongqiang)
 * @see EnvironmentAware
 * @see ExceptionUtils
 * @see ConstraintViolationUtil
 * @see ConstraintViolation
 * @since 2020-05-10
 */
@Component
public class ExceptionHelper implements EnvironmentAware {

    /**
     * Spring环境配置对象
     * <p>用于获取应用程序的环境配置信息，如debug模式等</p>
     */
    private static Environment env;

    /**
     * 调试模式标志
     * <p>控制是否输出详细的异常和约束违规信息，默认为false</p>
     */
    private static Boolean debug = Boolean.FALSE;

    /**
     * 格式化约束违规信息
     * <p>
     * 将Bean Validation的约束违规对象转换为可读的字符串格式。
     * 输出的详细程度取决于当前的调试模式设置。
     * </p>
     *
     * @param violation 约束违规对象
     * @return 格式化后的约束违规信息字符串
     * @see ConstraintViolationUtil#toString(ConstraintViolation, boolean)
     */
    public static String formatConstraintViolation(ConstraintViolation<?> violation) {
        return ConstraintViolationUtil.toString(violation, debug());
    }

    /**
     * 批量格式化约束违规信息
     * <p>
     * 将一组约束违规对象转换为汇总的字符串描述，包含违规数量及详细列表。
     * </p>
     *
     * @param violations 约束违规对象集合
     * @return 格式化后的汇总违规信息字符串
     */
    public static String formatConstraintViolations(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(ExceptionHelper::formatConstraintViolation).collect(
                Collectors.joining(", ", String.format("parameter is not valid(%d): ", violations.size()), ""));
    }

    /**
     * 获取异常链中根异常的消息
     * <p>
     * 从异常链中提取最底层（根）异常的错误消息。如果消息包含多行，
     * 则只返回第一行内容，以便获取最关键的错误信息。
     * </p>
     *
     * @param throwable 异常对象
     * @return 根异常的消息文本，如果消息为空则返回null
     * @see ExceptionUtils#getRootCause(Throwable)
     */
    public static String getRootMessage(Throwable throwable) {
        String message = ExceptionUtils.getRootCause(throwable).getMessage();
        if (StringUtil.isNotBlank(message)) {
            StringTokenizer st = new StringTokenizer(message, "\r\n");
            return st.nextToken();
        }
        return message;
    }

    /**
     * 检查是否处于调试模式
     * <p>
     * 判断当前应用程序是否启用了调试模式。调试模式开启时，
     * 异常和约束违规信息会输出更详细的内容。
     * </p>
     *
     * @return 如果调试模式已启用且环境配置有效则返回true，否则返回false
     */
    public static boolean debug() {
        return env != null && debug != null && debug;
    }

    /**
     * 设置Spring环境配置
     * <p>
     * 此方法由Spring框架自动调用，用于注入环境配置对象。
     * 同时会从环境配置中读取debug属性来初始化调试模式标志。
     * </p>
     *
     * @param environment Spring环境配置对象
     */
    @Override
    public void setEnvironment(Environment environment) {
        env = environment;
        if (env.containsProperty("debug")) {
            debug = env.getProperty("debug", Boolean.class);
        }
    }
}