package io.hqwu.commons.util;

import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.spi.LocationAwareLogger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.IllegalFormatException;
import java.util.LinkedList;
import java.util.List;
import java.util.MissingFormatArgumentException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志工具基础类，提供统一的日志记录功能。
 * <p>
 * 主要功能特性：
 * <ul>
 *   <li>支持多种日志格式：SLF4J 占位符 ({}) 和 Java Formatter (%s, %d 等)</li>
 *   <li>提供访问日志（ACCESS）和性能日志（PERFORMANCE）记录</li>
 *   <li>支持异常堆栈信息自动追加</li>
 *   <li>提供方法执行耗时统计和性能阈值检测</li>
 * </ul>
 * </p>
 *
 * @author hqwu
 * @see org.slf4j.Logger
 * @see org.slf4j.spi.LocationAwareLogger
 * @see Logger
 */
class LoggerBase {

    private static final org.slf4j.Logger testLogger = LoggerFactory.getLogger(LoggerBase.class);
    static {
        // if the logger is not a LocationAwareLogger instance, it can not get correct stack StackTraceElement
        // so ignore this implementation.
        if (!(testLogger instanceof LocationAwareLogger)) {
            throw new UnsupportedOperationException(testLogger.getClass() + " is not a suitable logger");
        }
    }

    private static final String NA = "UnknowClass";

    private static final org.slf4j.Logger ACCESS_LOGGER;
    private static final org.slf4j.Logger PERF_LOGGER;
    private static ThreadLocal<Long> tlAccessStart = new ThreadLocal<>();

    private static final String NULL = "NULL";

    public static final String LINE_SEP = System.getProperty("line.separator");
    public static final int LINE_SEP_LEN = LINE_SEP.length();

    private static final String formatSpecifier
            = "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";

    private static Pattern fsPattern = Pattern.compile(formatSpecifier);

    private static final String callerFQCN = LoggerBase.class.getName();

    protected final LocationAwareLogger delegate;

    static {
        ACCESS_LOGGER = LoggerFactory.getLogger(System.getProperty("logger.access.name", "ACCESS"));
        PERF_LOGGER = LoggerFactory.getLogger(System.getProperty("logger.performance.name", "PERFORMANCE"));
    }

    public LoggerBase(String cls) {
        delegate = (LocationAwareLogger) LoggerFactory.getLogger(cls);
    }

    public LoggerBase(Class<?> clazz) {
        if (clazz == io.hqwu.commons.util.LoggerFactory.class
                || clazz == Logger.class) {
            final String CLASSNAME = clazz.getName() + ".";
            String cls = getCallingClassName(CLASSNAME);
            delegate = (LocationAwareLogger) LoggerFactory.getLogger(cls);
        } else {
            delegate = (LocationAwareLogger) LoggerFactory.getLogger(clazz);
        }
    }

    public String getName() {
        return delegate.getName();
    }

    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }

    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    public boolean isWarnEnabled() {
        return delegate.isWarnEnabled();
    }

    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }

    /**
     * 标记access的开始
     */
    public static void accessStart() {
        tlAccessStart.set(System.nanoTime());
    }

    /**
     * 记录access日志
     *
     * @param args
     */
    public static void access(Object... args) {
        if (args == null) {
            args = new Object[]{NULL};
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(unull(args[i]));
        }
        //如果标记了开始时间，则在access最后追加耗时
        Long startNS = tlAccessStart.get();
        if (startNS != null) {
            sb.append(',').append(Formatter.formatNS(System.nanoTime() - startNS)).append("ns");
            tlAccessStart.remove();
        }
        ACCESS_LOGGER.info(sb.toString());
    }

    private static Object unull(Object obj) {
        return obj == null ? NULL : obj;
    }

    private static String _concat(Object... objects) {
        if (objects.length == 1) {
            return objects[0].toString();
        }
        StringBuilder sb = new StringBuilder();
        _concat(sb, objects);
        return sb.toString();
    }

    private static void _concat(StringBuilder sb, Object... objects) {
        for (int i = 0; i < objects.length; i++) {
            if ((i + 1) == objects.length && objects[i] instanceof Throwable) {
                //最后一个对象如果是Throwable，则留给上层进行堆栈打印
                break;
            }
            //如果objects[i]是Array，则进入递归concat by wuhq 2014.2.19
            if (objects[i] != null && objects[i].getClass().isArray()) {
                _concat(sb, (Object[]) objects[i]);
            } else {
                sb.append(unull(objects[i]));
            }
        }
    }

    private static String _format(String format, Object... args) {
        StringBuilder sb = new StringBuilder();
        java.util.Formatter formatter = new java.util.Formatter(sb);
        try {
            formatter.format(format, args);
        } catch (MissingFormatArgumentException e) {
            List<Integer> specifiers = _parse(format);
            if (specifiers.size() == 0) {
                throw e;
            }
            System.err.println(e.toString() + ", format: '" + format + "', args.length: " + args.length);
            StringBuilder alterFormat = new StringBuilder(format);
            for (int i = 0; i < specifiers.size() - args.length; i++) {
                int s = specifiers.get(i);
                alterFormat.insert(s, '%');
            }
            return _format(alterFormat.toString(), args);
        }
        return sb.toString();
    }

    /**
     * Finds format specifiers in the format string.
     */
    private static List<Integer> _parse(String s) {
        LinkedList<Integer> al = new LinkedList<>();
        Matcher m = fsPattern.matcher(s);
        for (int i = 0, len = s.length(); i < len; ) {
            if (m.find(i)) {
                String fmt = s.substring(m.start(), m.end());
                if (! fmt.equalsIgnoreCase("%%") && ! fmt.equalsIgnoreCase("%n")) {
                    al.offerFirst(m.start());
                }
                i = m.end();
            } else {
                break;
            }
        }
        return al;
    }

    private void callLog(Marker marker, Level level, String format, Object... objects) {
        Object[] args = new Object[objects.length + 1];
        args[0] = format;
        System.arraycopy(objects, 0, args, 1, objects.length);
        log(marker, level, args);
    }

    protected void logf(Marker marker, Level level, String format, Object... objects) {
        if (! isLevelEnabled(level)) {
            return;
        }
        String message;
        try {
            message = _format(format, objects);
        } catch (IllegalFormatException e) {
            System.err.println(e.toString());
            callLog(marker, level, format, objects);
            return;
        }
        if (objects.length >= 1 && objects[objects.length - 1] instanceof Throwable) {
            delegate.log(marker, callerFQCN, level.toInt(), message, null, (Throwable) objects[objects.length - 1]);
        } else {
            log(marker, level, message);
        }
    }

    protected void log(Level level, String format, Object... objects) {
        this.log(null, level, format, objects);
    }

    protected void log(Marker marker, Level level, String format, Object... objects) {
        if (! isLevelEnabled(level)) {
            return;
        }
        if (format.contains("{}")) {
            // slf4j 占位符
            delegate.log(marker, callerFQCN, level.toInt(), format, objects, null);
        } else {
            Matcher m = fsPattern.matcher(format);
            if (m.find()) {
                // java formatter %...
                logf(marker, level, format, objects);
            } else {
                callLog(marker, level, format, objects);
            }
        }
    }

    protected void log(Marker marker, Level level, Object...  objects) {
        if (! isLevelEnabled(level)) {
            return;
        }
        String message = _concat(objects);
        if (objects.length >= 1 && objects[objects.length - 1] instanceof Throwable) {
            delegate.log(marker, callerFQCN, level.toInt(), message, null, (Throwable) objects[objects.length - 1]);
        } else {
            log(marker, level, message);
        }
    }

    protected void log(Level level, Object object) {
        if (! isLevelEnabled(level)) {
            return;
        }
        if (object instanceof Throwable) {
            delegate.log(null, callerFQCN, level.toInt(), "StackTrace:", null, (Throwable) object);
        } else {
            delegate.log(null, callerFQCN, level.toInt(), object == null ? NULL : object.toString(), null, null);
        }
    }

    protected void log(Level level, String message) {
        this.log(null, level, message);
    }

    protected void log(Marker marker, Level level, String message) {
        if (! isLevelEnabled(level)) {
            return;
        }
        delegate.log(marker, callerFQCN, level.toInt(), message, null, null);
    }

    protected boolean isLevelEnabled(Level level) {
        switch (level) {
            case TRACE:
                return delegate.isTraceEnabled();
            case DEBUG:
                return delegate.isDebugEnabled();
            case INFO:
                return delegate.isInfoEnabled();
            case WARN:
                return delegate.isWarnEnabled();
            case ERROR:
                return delegate.isErrorEnabled();
        }
        return false;
    }

    private String getCallingClassName(String CLASSNAME) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        (new Throwable()).printStackTrace(pw);
        String stack = sw.toString();

        return resolveClassName(CLASSNAME, stack);
    }

    static String resolveClassName(String CLASSNAME, String stack) {
        // Given the current structure of the package, the line
        // containing "org.apache.log4j.Category." should be printed just
        // before the caller.

        // This method of searching may not be fastest but it's safer
        // than counting the stack depth which is not guaranteed to be
        // constant across JVM implementations.
        int ibegin = stack.lastIndexOf(CLASSNAME);
        if (ibegin == -1) {
            return NA;
        }

        ibegin = stack.indexOf(LINE_SEP, ibegin);
        if (ibegin == -1) {
            return NA;
        }
        ibegin += LINE_SEP_LEN;

        // determine end of line
        int iend = stack.indexOf(LINE_SEP, ibegin);
        if (iend == -1) {
            return NA;
        }

        // VA has a different stack trace format which doesn't
        // need to skip the inital 'at'
        // back up to first blank character
        ibegin = stack.lastIndexOf("at ", iend);
        if (ibegin == -1) {
            return NA;
        }
        // Add 3 to skip "at ";
        ibegin += 3;
        // everything between is the requested stack item
        String fullInfo = stack.substring(ibegin, iend);
        // modify by shenjl never null
//        if (fullInfo == null) {
//            return NA;
//        }

        // Starting the search from '(' is safer because there is
        // potentially a dot between the parentheses.
        iend = fullInfo.lastIndexOf('(');
        if (iend == -1) {
            return NA;
        }
        iend = fullInfo.lastIndexOf('.', iend);

        // This is because a stack trace in VisualAge looks like:
        //java.lang.RuntimeException
        //  java.lang.Throwable()
        //  java.lang.Exception()
        //  java.lang.RuntimeException()
        //  void test.test.B.print()
        //  void test.test.A.printIndirect()
        //  void test.test.Run.main(java.lang.String [])
        if (iend == -1) {
            return NA;
        } else {
            return fullInfo.substring(0, iend);
        }
    }

    private static String getShortName(TimeUnit unit) {
        if (unit == TimeUnit.NANOSECONDS) {
            return "ns";
        } else if (unit == TimeUnit.MICROSECONDS) {
            return "micro";
        } else if (unit == TimeUnit.MILLISECONDS) {
            return "ms";
        } else if (unit == TimeUnit.SECONDS) {
            return "sec";
        } else if (unit == TimeUnit.MINUTES) {
            return "min";
        } else if (unit == TimeUnit.HOURS) {
            return "hour";
//        } else if (unit == TimeUnit.DAYS) {
//            return "day";
        }
        return "day";
    }

    /**
     * 输出性能日志
     *
     * @param info      输出信息
     * @param startTime 开始时间
     * @param unit      时间单位
     * @param threshold 阈值
     * @param delta     增量,超过一倍增量输出info日志，超过2倍增量以上输出warn
     */
    public static final void timeSpent(String info, long startTime, TimeUnit unit, long threshold, long delta) {
        long now;
        if (unit == TimeUnit.NANOSECONDS) { //计算方法不一样
            now = System.nanoTime();
        } else {
            now = unit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }
        long spent = now - startTime;
        if (spent >= threshold) {
            if (delta > 0) { //按级别输入日志
                int multiple = (int) ((spent - threshold) / delta);
                if (multiple == 0) {
                    PERF_LOGGER.debug(_concat(info, " spent ", spent, getShortName(unit),", but expect in ", threshold, getShortName(unit)));
                } else if (multiple == 1) {
                    PERF_LOGGER.info(_concat(info, " spent ", spent, getShortName(unit),", but expect in ", threshold, getShortName(unit)));
                } else {
                    PERF_LOGGER.warn(_concat(info, " spent ", spent, getShortName(unit),", but expect in ", threshold, getShortName(unit)));
                }
            } else {      //统一debug
                PERF_LOGGER.debug(_concat(info, " spent ", spent, getShortName(unit),", but expect in ", threshold, getShortName(unit)));
            }
        }
    }

    /**
     * @see #timeSpent(String, long, TimeUnit, long, long)
     */
    public static final void timeSpent(String info, long startTime, TimeUnit unit, long threshold) {
        timeSpent(info, startTime, unit, threshold, threshold);
    }

    /**
     * @see #timeSpent(String, long, TimeUnit, long, long)
     */
    public static final void timeSpentNan(String info, long startTime, long threshold) {
        timeSpent(info, startTime, TimeUnit.NANOSECONDS, threshold, threshold);
    }

    /**
     * @see #timeSpent(String, long, TimeUnit, long, long)
     */
    public static final void timeSpentNan(String info, long startTime, long threshold, long delta) {
        timeSpent(info, startTime, TimeUnit.NANOSECONDS, threshold, delta);
    }

    /**
     * @see #timeSpent(String, long, TimeUnit, long, long)
     */
    public static final void timeSpentMillSec(String info, long startTime, long threshold) {
        timeSpent(info, startTime, TimeUnit.MILLISECONDS, threshold, threshold);
    }

    /**
     * @see #timeSpent(String, long, TimeUnit, long, long)
     */
    public static final void timeSpentMillSec(String info, long startTime, long threshold, long delta) {
        timeSpent(info, startTime, TimeUnit.MILLISECONDS, threshold, delta);
    }
}
