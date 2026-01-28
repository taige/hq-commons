package io.hqwu.commons.util;

import org.slf4j.Marker;
import org.slf4j.event.Level;

/**
 * 增强型日志记录器,扩展了SLF4J标准日志接口功能。
 * <p>
 * 该类继承自 {@link LoggerBase} 并实现了 {@link org.slf4j.Logger} 接口,
 * 在保持与SLF4J完全兼容的同时,提供了以下增强特性:
 * </p>
 *
 * <p><b>⚠️ 实例化建议</b>: 推荐使用 {@link LoggerFactory} 进行日志记录器的创建和管理，
 * 以获得更好的性能和统一的日志管理。详见下文使用示例。</p>
 *
 * <h3>相对于标准SLF4J的增强点:</h3>
 * <ul>
 *   <li><b>支持Object类型参数</b>: 除了标准的String消息外,还提供了接受Object参数的重载方法,
 *       如 {@code debug(Object)}, {@code info(Object)} 等,可以直接记录任意对象</li>
 *   <li><b>双重格式化支持</b>: 同时支持SLF4J占位符格式和String.format格式(详见下文)</li>
 *   <li><b>Marker支持</b>: 完整实现了SLF4J的Marker机制,用于日志分类和过滤</li>
 * </ul>
 *
 * <h3>支持的日志消息格式化方式:</h3>
 * <ol>
 *   <li><b>SLF4J占位符格式(推荐)</b>: 使用 {@code {}} 作为占位符
 *     <pre>{@code
 *     logger.info("User {} logged in from {}", username, ipAddress);
 *     logger.debug("Processing {} items in {} ms", count, duration);
 *     logger.error("Failed to connect to {} on port {}", host, port);
 *     }</pre>
 *   </li>
 *   <li><b>String.format格式</b>: 使用 {@code %s}, {@code %d} 等格式说明符
 *     <pre>{@code
 *     logger.info("User %s logged in from %s", username, ipAddress);
 *     logger.debug("Processing %d items in %d ms", count, duration);
 *     logger.error("Failed to connect to %s on port %d", host, port);
 *     }</pre>
 *   </li>
 * </ol>
 *
 * <h3>使用示例:</h3>
 * <pre>{@code
 * // 推荐方式 - 使用 LoggerFactory 获取日志记录器(性能更优,统一管理)
 * private static final Logger LOGGER = LoggerFactory.getLogger(MyClass.class);
 *
 * // 或者使用 LoggerFactory 的便捷方法
 * private static final Logger LOGGER = LoggerFactory.getLogger();
 *
 * // 基本用法 - 直接使用构造方法(不推荐,仅用于特殊场景)
 * // 使用不带参数的构造方法可以自动识别所在类
 * Logger logger = new Logger();
 * // 或显式指定类
 * Logger logger = new Logger(MyClass.class);
 *
 * // SLF4J格式
 * LOGGER.info("User {} logged in", username);
 * LOGGER.error("Error processing order {}", orderId, exception);
 *
 * // String.format格式
 * LOGGER.debug("Cache hit rate: %.2f%%", hitRate * 100);
 *
 * // Object参数(增强功能)
 * LOGGER.info(userObject);
 * LOGGER.debug(requestDto);
 *
 * // 使用Marker
 * Marker securityMarker = MarkerFactory.getMarker("SECURITY");
 * LOGGER.warn(securityMarker, "Failed login attempt for user {}", username);
 * }</pre>
 *
 * <h3>日志输出模式配置:</h3>
 * <pre>
 * <h3>日志输出模式配置:</h3>
 * <p>本节说明日志框架(如Logback/Log4j)的配置模式符号和格式修饰符。</p>
 *
 * <h4>模式符号说明:</h4>
 * <table border="1" cellpadding="5" cellspacing="0">
 *   <tr>
 *     <th>符号</th>
 *     <th>用途</th>
 *     <th>可选选项</th>
 *     <th>性能说明</th>
 *   </tr>
 *   <tr>
 *     <td>%c</td>
 *     <td>日志名称(通常为类的全限定名)</td>
 *     <td>{数字} - 输出最后n段,如: "a.b.c" 使用 %c{2} 输出 "b.c"</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td>%C</td>
 *     <td>调用者的完整类名</td>
 *     <td>{数字} - 同上</td>
 *     <td>⚠️ 速度慢,不推荐</td>
 *   </tr>
 *   <tr>
 *     <td>%d</td>
 *     <td>日志时间戳</td>
 *     <td>{日期格式} - 如: %d{yyyy-MM-dd HH:mm:ss.SSS}</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td>%F</td>
 *     <td>调用者的源文件名</td>
 *     <td>无</td>
 *     <td>⚠️⚠️ 速度极慢,不推荐</td>
 *   </tr>
 *   <tr>
 *     <td>%l</td>
 *     <td>调用位置(类名.方法名:文件名:行号)</td>
 *     <td>无</td>
 *     <td>⚠️⚠️⚠️ 速度极其慢,不推荐</td>
 *   </tr>
 *   <tr>
 *     <td>%L</td>
 *     <td>调用者的行号</td>
 *     <td>无</td>
 *     <td>⚠️⚠️ 速度极慢,不推荐</td>
 *   </tr>
 *   <tr>
 *     <td>%m</td>
 *     <td>日志消息内容</td>
 *     <td>无</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td>%M</td>
 *     <td>调用者的方法名</td>
 *     <td>无</td>
 *     <td>⚠️⚠️ 速度极慢,不推荐</td>
 *   </tr>
 *   <tr>
 *     <td>%n</td>
 *     <td>平台相关的换行符</td>
 *     <td>无</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td>%p</td>
 *     <td>日志级别</td>
 *     <td>输出: TRACE/DEBUG/INFO/WARN/ERROR</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td>%r</td>
 *     <td>从应用启动到日志输出的毫秒数</td>
 *     <td>无</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td>%t</td>
 *     <td>生成日志事件的线程名</td>
 *     <td>无</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td>%x</td>
 *     <td>NDC(嵌套诊断上下文)</td>
 *     <td>输出与当前线程关联的NDC</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td>%X</td>
 *     <td>MDC(映射诊断上下文)</td>
 *     <td>%X{key} - 输出特定键的值</td>
 *     <td>-</td>
 *   </tr>
 * </table>
 *
 * <h4>格式修饰符说明:</h4>
 * <table border="1" cellpadding="5" cellspacing="0">
 *   <tr>
 *     <th>格式</th>
 *     <th>对齐方式</th>
 *     <th>最小宽度</th>
 *     <th>最大宽度</th>
 *     <th>示例说明</th>
 *   </tr>
 *   <tr>
 *     <td>%20c</td>
 *     <td>右对齐</td>
 *     <td>20</td>
 *     <td>无限制</td>
 *     <td>不足20字符时左侧补空格</td>
 *   </tr>
 *   <tr>
 *     <td>%-20c</td>
 *     <td>左对齐</td>
 *     <td>20</td>
 *     <td>无限制</td>
 *     <td>不足20字符时右侧补空格</td>
 *   </tr>
 *   <tr>
 *     <td>%.30c</td>
 *     <td>默认</td>
 *     <td>无</td>
 *     <td>30</td>
 *     <td>超过30字符时截断</td>
 *   </tr>
 *   <tr>
 *     <td>%20.30c</td>
 *     <td>右对齐</td>
 *     <td>20</td>
 *     <td>30</td>
 *     <td>最少20字符,最多30字符,右对齐</td>
 *   </tr>
 *   <tr>
 *     <td>%-20.30c</td>
 *     <td>左对齐</td>
 *     <td>20</td>
 *     <td>30</td>
 *     <td>最少20字符,最多30字符,左对齐</td>
 *   </tr>
 * </table>
 *
 * <h4>配置示例:</h4>
 * <pre>{@code
 * <!-- Console 输出格式 -->
 * %d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5p %c{36} - %m%n
 *
 * <!-- 详细调试格式(包含位置信息,性能较低) -->
 * %d{HH:mm:ss.SSS} [%t] %-5p %C.%M(%F:%L) - %m%n
 *
 * <!-- 简洁格式 -->
 * %d{HH:mm:ss} %-5p %c{1} - %m%n
 * }</pre>
 *
 * @author Wu, Hongqiang
 * @see LoggerBase
 * @see org.slf4j.Logger
 * @see org.slf4j.Marker
 * @see org.slf4j.event.Level
 * @since 1.0
 */
public class Logger extends LoggerBase implements org.slf4j.Logger {

    public Logger() {
        super(Logger.class);
    }

    public Logger(String cls) {
        super(cls);
    }

    public Logger(Class<?> cls) {
        super(cls);
    }

    @Override
    public void trace(String msg) {
        log(Level.TRACE, msg);
    }

    @Override
    public void debug(String msg) {
        log(Level.DEBUG, msg);
    }

    @Override
    public void info(String msg) {
        log(Level.INFO, msg);
    }

    @Override
    public void warn(String msg) {
        log(Level.WARN, msg);
    }

    @Override
    public void error(String msg) {
        log(Level.ERROR, msg);
    }

    public void trace(Object object) {
        log(Level.TRACE, object);
    }

    public void debug(Object object) {
        log(Level.DEBUG, object);
    }

    public void info(Object object) {
        log(Level.INFO, object);
    }

    public void warn(Object object) {
        log(Level.WARN, object);
    }

    public void error(Object object) {
        log(Level.ERROR, object);
    }

    @Override
    public void trace(String fmt, Object arg) {
        log(Level.TRACE, fmt, arg);
    }

    @Override
    public void debug(String fmt, Object arg) {
        log(Level.DEBUG, fmt, arg);
    }

    @Override
    public void info(String fmt, Object arg) {
        log(Level.INFO, fmt, arg);
    }

    @Override
    public void warn(String fmt, Object arg) {
        log(Level.WARN, fmt, arg);
    }

    @Override
    public void error(String fmt, Object arg) {
        log(Level.ERROR, fmt, arg);
    }

    @Override
    public void trace(String fmt, Object arg1, Object arg2) {
        log(Level.TRACE, fmt, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... objects) {
        log(Level.TRACE, format, objects);
    }

    @Override
    public void info(String format, Object... objects) {
        log(Level.INFO, format, objects);
    }

    @Override
    public void debug(String format, Object... objects) {
        log(Level.DEBUG, format, objects);
    }

    @Override
    public void warn(String format, Object... objects) {
        log(Level.WARN, format, objects);
    }

    @Override
    public void error(String format, Object... objects) {
        log(Level.ERROR, format, objects);
    }

    @Override
    public void debug(String fmt, Object arg1, Object arg2) {
        log(Level.DEBUG, fmt, arg1, arg2);
    }

    @Override
    public void info(String fmt, Object arg1, Object arg2) {
        log(Level.INFO, fmt, arg1, arg2);
    }

    @Override
    public void warn(String fmt, Object arg1, Object arg2) {
        log(Level.WARN, fmt, arg1, arg2);
    }

    @Override
    public void error(String fmt, Object arg1, Object arg2) {
        log(Level.ERROR, fmt, arg1, arg2);
    }

    @Override
    public void trace(String msg, Throwable throwable) {
        log(Level.TRACE, msg, throwable);
    }

    @Override
    public void info(String msg, Throwable throwable) {
        log(Level.INFO, msg, throwable);
    }

    @Override
    public void debug(String msg, Throwable throwable) {
        log(Level.DEBUG, msg, throwable);
    }

    @Override
    public void warn(String msg, Throwable throwable) {
        log(Level.WARN, msg, throwable);
    }

    @Override
    public void error(String msg, Throwable throwable) {
        log(Level.ERROR, msg, throwable);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return delegate.isTraceEnabled(marker);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return delegate.isDebugEnabled(marker);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return delegate.isInfoEnabled(marker);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return delegate.isWarnEnabled(marker);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return delegate.isErrorEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        log(marker, Level.TRACE, msg);
    }

    @Override
    public void debug(Marker marker, String msg) {
        log(marker, Level.DEBUG, msg);
    }

    @Override
    public void info(Marker marker, String msg) {
        log(marker, Level.INFO, msg);
    }

    @Override
    public void warn(Marker marker, String msg) {
        log(marker, Level.WARN, msg);
    }

    @Override
    public void error(Marker marker, String msg) {
        log(marker, Level.ERROR, msg);
    }

    @Override
    public void trace(Marker marker, String fmt, Object arg) {
        log(marker, Level.TRACE, fmt, arg);
    }

    @Override
    public void debug(Marker marker, String fmt, Object arg) {
        log(marker, Level.DEBUG, fmt, arg);
    }

    @Override
    public void info(Marker marker, String fmt, Object arg) {
        log(marker, Level.INFO, fmt, arg);
    }

    @Override
    public void warn(Marker marker, String fmt, Object arg) {
        log(marker, Level.WARN, fmt, arg);
    }

    @Override
    public void error(Marker marker, String fmt, Object arg) {
        log(marker, Level.ERROR, fmt, arg);
    }

    @Override
    public void trace(Marker marker, String fmt, Object arg1, Object arg2) {
        log(marker, Level.TRACE, fmt, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String fmt, Object arg1, Object arg2) {
        log(marker, Level.DEBUG, fmt, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String fmt, Object arg1, Object arg2) {
        log(marker, Level.INFO, fmt, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String fmt, Object arg1, Object arg2) {
        log(marker, Level.WARN, fmt, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String fmt, Object arg1, Object arg2) {
        log(marker, Level.ERROR, fmt, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String fmt, Object... objects) {
        log(marker, Level.TRACE, fmt, objects);
    }

    @Override
    public void debug(Marker marker, String fmt, Object... objects) {
        log(marker, Level.DEBUG, fmt, objects);
    }

    @Override
    public void info(Marker marker, String fmt, Object... objects) {
        log(marker, Level.INFO, fmt, objects);
    }

    @Override
    public void warn(Marker marker, String fmt, Object... objects) {
        log(marker, Level.WARN, fmt, objects);
    }

    @Override
    public void error(Marker marker, String fmt, Object... objects) {
        log(marker, Level.ERROR, fmt, objects);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable throwable) {
        log(marker, Level.TRACE, (Object) msg, throwable);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable throwable) {
        log(marker, Level.DEBUG, (Object) msg, throwable);
    }

    @Override
    public void info(Marker marker, String msg, Throwable throwable) {
        log(marker, Level.INFO, (Object) msg, throwable);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable throwable) {
        log(marker, Level.WARN, (Object) msg, throwable);
    }

    @Override
    public void error(Marker marker, String msg, Throwable throwable) {
        log(marker, Level.ERROR, (Object) msg, throwable);
    }

}
