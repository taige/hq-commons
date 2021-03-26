package com.umpay.commons.util;

import org.slf4j.Marker;
import org.slf4j.event.Level;

/**
 * 模式符号 - 用途(附加说明);{可选附加选项}(附加选项说明)
 * c - 日志名称(通常是构造函数的参数);{数字}("a.b.c" 的名称使用 %c{2} 会输出 "b.c")
 * C - 调用者的类名(速度慢,不推荐使用);{数字}(同上)
 * d - 日志时间;{SimpleDateFormat所能使用的格式}
 * F - 调用者的文件名(速度极慢,不推荐使用)
 * l - 调用者的函数名、文件名、行号(速度极其极其慢,不推荐使用)
 * L - 调用者的行号(速度极慢,不推荐使用)
 * m - 日志
 * M - 调用者的函数名(速度极慢,不推荐使用)
 * n - 换行符号
 * p - 日志优先级别(DEBUG, INFO, WARN, ERROR)
 * r - 输出日志所用毫秒数
 * t - 调用者的进程名
 * x - Used to output the NDC (nested diagnostic context) associated with the thread that generated the logging event.
 * X - Used to output the MDC (mapped diagnostic context) associated with the thread that generated the logging event.
 * ************************************************************************************************************************************************************
 * 模式修饰符 - 对齐 - 最小长度 - 最大长度 - 说明 %20c 右 20 ~ %-20c 左 20 ~ %.30c ~ ~ 30 %20.30c 右 20 30 %-20.30c 左 20 30
 * ************************************************************************************************************************************************************
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
