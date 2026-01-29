package io.hqwu.commons.util;

import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.event.Level;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created with IntelliJ IDEA.
 *
 * Date: 13-10-29
 * Time: 下午4:31
 */
public class LoggerBaseTest {
    Logger logger = new Logger();

    @Test
    public void testLoggerBaseStringConstructor() {
        // Test constructor with string class name
        Logger logger1 = new Logger("io.hqwu.commons.util.LoggerBaseTest");
        assertEquals("io.hqwu.commons.util.LoggerBaseTest", logger1.getName());

        // Test with simple class name
        Logger logger2 = new Logger("TestLogger");
        assertEquals("TestLogger", logger2.getName());

        // Test with nested class name
        Logger logger3 = new Logger("io.hqwu.commons.util.LoggerBaseTest$InnerClass");
        assertEquals("io.hqwu.commons.util.LoggerBaseTest$InnerClass", logger3.getName());

        // Test that logger is functional
        logger1.info("Test message from string constructor");
        logger2.debug("Debug message");
        logger3.warn("Warning message");
    }

    @Test
    public void testIsEnabledMethods() {
        // Test all isXxxxEnabled() methods
        Logger testLogger = new Logger("TestLogger");

        // These methods should return boolean values without throwing exceptions
        // The actual values depend on logger configuration, we just verify they work
        boolean traceEnabled = testLogger.isTraceEnabled();
        boolean debugEnabled = testLogger.isDebugEnabled();
        boolean infoEnabled = testLogger.isInfoEnabled();
        boolean warnEnabled = testLogger.isWarnEnabled();
        boolean errorEnabled = testLogger.isErrorEnabled();

        // Verify they return boolean values (not null)
        // At minimum, error level should typically be enabled
        // We just ensure the methods execute without exception
        testLogger.info("Trace enabled: " + traceEnabled);
        testLogger.info("Debug enabled: " + debugEnabled);
        testLogger.info("Info enabled: " + infoEnabled);
        testLogger.info("Warn enabled: " + warnEnabled);
        testLogger.info("Error enabled: " + errorEnabled);
    }

    @Test
    public void testIsEnabledWithMarkerMethods() {
        // Test all isXxxxEnabled(Marker marker) methods
        Logger testLogger = new Logger("TestLogger");
        Marker testMarker = MarkerFactory.getMarker("TEST_MARKER");

        // These methods should return boolean values without throwing exceptions
        // The actual values depend on logger configuration, we just verify they work
        boolean traceEnabled = testLogger.isTraceEnabled(testMarker);
        boolean debugEnabled = testLogger.isDebugEnabled(testMarker);
        boolean infoEnabled = testLogger.isInfoEnabled(testMarker);
        boolean warnEnabled = testLogger.isWarnEnabled(testMarker);
        boolean errorEnabled = testLogger.isErrorEnabled(testMarker);

        // Verify they return boolean values (not null)
        // We just ensure the methods execute without exception
        testLogger.info("Trace enabled (with marker): " + traceEnabled);
        testLogger.info("Debug enabled (with marker): " + debugEnabled);
        testLogger.info("Info enabled (with marker): " + infoEnabled);
        testLogger.info("Warn enabled (with marker): " + warnEnabled);
        testLogger.info("Error enabled (with marker): " + errorEnabled);

        // Test with null marker (should not throw NPE)
        try {
            testLogger.isTraceEnabled(null);
            testLogger.isDebugEnabled(null);
            testLogger.isInfoEnabled(null);
            testLogger.isWarnEnabled(null);
            testLogger.isErrorEnabled(null);
        } catch (NullPointerException e) {
            // Some implementations may not support null markers
            testLogger.warn("Note: null marker not supported");
        }
    }

    @Test
    public void testSimpleLoggingMethods() {
        // Test xxx(Object object) methods - Logger.java lines 66-84
        // These methods accept any Object and call log(Level, object)
        AlwaysEnabledLogger alwaysLogger = new AlwaysEnabledLogger(LoggerBaseTest.class);

        // Test xxx(Object object) with String objects
        alwaysLogger.trace((Object) "trace message");
        alwaysLogger.debug((Object) "debug message");
        alwaysLogger.info((Object) "info message");
        alwaysLogger.warn((Object) "warn message");
        alwaysLogger.error((Object) "error message");

        // Test with null object
        alwaysLogger.trace((Object) null);
        alwaysLogger.debug((Object) null);
        alwaysLogger.info((Object) null);

        // Test with Exception objects
        Exception testException = new Exception("test exception");
        alwaysLogger.trace(testException);
        alwaysLogger.debug(testException);
        alwaysLogger.warn(testException);
        alwaysLogger.error(testException);

        // Test with other object types
        alwaysLogger.info(Integer.valueOf(123));
        alwaysLogger.warn(Boolean.TRUE);
        alwaysLogger.debug(new Object[] {"array", "test"});
    }


    @Test
    public void testSlf4jStandardMethods() {
        // Test SLF4J standard methods that were not covered in other tests
        AlwaysEnabledLogger alwaysLogger = new AlwaysEnabledLogger(LoggerBaseTest.class);
        Exception testException = new Exception("test exception for slf4j methods");

        // Test xxx(String fmt, Object arg) - single argument methods
        alwaysLogger.trace("trace with single arg: {}", "arg1");
        alwaysLogger.debug("debug with single arg: {}", 123);
        alwaysLogger.info("info with single arg: {}", true);
        alwaysLogger.warn("warn with single arg: {}", 45.67);
        alwaysLogger.error("error with single arg: {}", "error-value");

        // Test xxx(String msg, Throwable throwable) - message with exception
        alwaysLogger.trace("trace message with throwable", testException);
        alwaysLogger.debug("debug message with throwable", testException);
        alwaysLogger.info("info message with throwable", testException);
        alwaysLogger.warn("warn message with throwable", testException);
        alwaysLogger.error("error message with throwable", testException);

        // Test edge cases
        alwaysLogger.error("error with null arg: {}", (Object) null);
        alwaysLogger.trace("trace with null throwable", (Throwable) null);
        alwaysLogger.warn("warn message", new RuntimeException("runtime exception"));
    }

    @Test
    public void testMarkerLoggingMethods() {
        // Test all Marker-based logging methods
        AlwaysEnabledLogger alwaysLogger = new AlwaysEnabledLogger(LoggerBaseTest.class);
        Marker testMarker = MarkerFactory.getMarker("TEST_MARKER");
        Exception testException = new Exception("test exception for marker logging");

        // 1. Test xxxx(Marker marker, String msg)
        alwaysLogger.trace(testMarker, "trace message with marker");
        alwaysLogger.debug(testMarker, "debug message with marker");
        alwaysLogger.info(testMarker, "info message with marker");
        alwaysLogger.warn(testMarker, "warn message with marker");
        alwaysLogger.error(testMarker, "error message with marker");

        // 2. Test xxxx(Marker marker, String fmt, Object arg)
        alwaysLogger.trace(testMarker, "trace with arg: {}", "value1");
        alwaysLogger.debug(testMarker, "debug with arg: {}", 123);
        alwaysLogger.info(testMarker, "info with arg: {}", true);
        alwaysLogger.warn(testMarker, "warn with arg: {}", 45.67);
        alwaysLogger.error(testMarker, "error with arg: {}", "error-value");

        // 3. Test xxxx(Marker marker, String fmt, Object arg1, Object arg2)
        alwaysLogger.trace(testMarker, "trace with 2 args: {} and {}", "arg1", "arg2");
        alwaysLogger.debug(testMarker, "debug with 2 args: {} and {}", 1, 2);
        alwaysLogger.info(testMarker, "info with 2 args: {} and {}", true, false);
        alwaysLogger.warn(testMarker, "warn with 2 args: {} and {}", "w1", "w2");
        alwaysLogger.error(testMarker, "error with 2 args: {} and {}", "e1", "e2");

        // 4. Test xxxx(Marker marker, String fmt, Object... objects)
        // Note: Must pass 0, 1, or 3+ arguments to call varargs version
        // (2 arguments will call the arg1, arg2 overload)
        alwaysLogger.trace(testMarker, "trace with varargs: {} {} {} {}", "a", "b", "c", "d");
        alwaysLogger.debug(testMarker, "debug with varargs: {} {} {}", 1, 2, 3);
        alwaysLogger.info(testMarker, "info with varargs: {} {} {} {} {}", 1, 2, 3, 4, 5);
        alwaysLogger.warn(testMarker, "warn with varargs: {} {} {}", "w1", "w2", "w3");  // Fixed: 3 args
        alwaysLogger.error(testMarker, "error with varargs: {} {} {}", "e1", "e2", "e3");

        // Also test with different argument counts for varargs
        alwaysLogger.trace(testMarker, "trace varargs single: {}", new Object[]{"single"});
        alwaysLogger.warn(testMarker, "warn varargs 4 args: {} {} {} {}", "a", "b", "c", "d");
        alwaysLogger.info(testMarker, "info varargs 6 args: {} {} {} {} {} {}", 1, 2, 3, 4, 5, 6);

        // 5. Test xxxx(Marker marker, String msg, Throwable throwable)
        alwaysLogger.trace(testMarker, "trace with throwable", testException);
        alwaysLogger.debug(testMarker, "debug with throwable", testException);
        alwaysLogger.info(testMarker, "info with throwable", testException);
        alwaysLogger.warn(testMarker, "warn with throwable", testException);
        alwaysLogger.error(testMarker, "error with throwable", testException);

        // 6. Test with null marker (explicit cast to avoid ambiguity)
        alwaysLogger.info((Marker) null, "info with null marker");
        alwaysLogger.warn((Marker) null, "warn with null marker and arg: {}", "value");
        alwaysLogger.error((Marker) null, "error with null marker and throwable", testException);

        // 7. Test edge cases
        alwaysLogger.info(testMarker, "null arg: {}", (Object) null);
        alwaysLogger.warn(testMarker, "empty message");
        alwaysLogger.error(testMarker, "message with throwable as null", (Throwable) null);
    }

    @Test
    public void testAllf() throws Exception {
        logger.trace("%s", "allf");
    }

    @Test
    public void testAll1() throws Exception {
        logger.trace("all1");
    }

    @Test
    public void testAll2() throws Exception {
        logger.trace("all", 2);
    }

    @Test
    public void testDebugf() throws Exception {
        logger.debug("debugf %s-%s", "a", 4);
    }

    @Test
    public void testDebug1() throws Exception {
        logger.debug("debug1");
    }

    @Test
    public void testDebug2() throws Exception {
        logger.debug("debug", 2);
    }

    @Test
    public void testInfof() throws Exception {
        logger.info("infof %s-%b", "is", false);
    }

    @Test
    public void testInfo1() throws Exception {
        logger.info("info1");
    }

    @Test
    public void testInfo2() throws Exception {
        logger.info("info", 2);
    }

    @Test
    public void testWarnf() throws Exception {
        logger.warn("warnf %s", new Exception());
    }

    @Test
    public void testWarn1() throws Exception {
        logger.warn(new Exception());
    }

    @Test
    public void testWarn2() throws Exception {
        logger.warn("E: ", new Exception());
    }

    @Test
    public void testErrorf() throws Exception {
        logger.error("errorf %s", new Exception());
    }

    @Test
    public void testError1() throws Exception {
        logger.error(new Exception());
    }

    @Test
    public void testError2() throws Exception {
        logger.error("E: " + new Exception());
    }

    @Test
    public void testAccess() throws Exception {
        Logger.accessStart();
        Logger.access("abc");
    }

    @Test
    public void testAccess1() throws Exception {
        Logger.accessStart();
        Logger.access("abc", "abc");
    }

    @Test
    public void testAccess2() throws Exception {
        Logger.access("abc", "abc");
    }

    static ThreadLocal<Long> ss = new ThreadLocal<Long>();

    @Test
    public void testThreadLocal() throws Exception {
        final CountDownLatch cdl = new CountDownLatch(100);
        final CyclicBarrier barrier = new CyclicBarrier(100);
        final AtomicLong totalUse = new AtomicLong(0);
        final int tc = 100, count = 100000;
        for (int i = 0; i < tc; i++) {
            new Thread() {
                public void run() {
                    try {
                        barrier.await();
                    } catch (InterruptedException e) {
                    } catch (BrokenBarrierException e) {
                    }
                    long start = System.nanoTime();
                    for (int jj = 0; jj < count; jj++) {
                        ss.set(System.nanoTime());
                        ss.get();
                        ss.remove();
                    }
                    long use = System.nanoTime() - start;
                    totalUse.addAndGet(use);
                    logger.info("用时: " + (use));
                    cdl.countDown();
                }
            }.start();
        }
        cdl.await();
        logger.info("平均用时: " + (totalUse.get() / tc / count));
    }

    @Test
    public void testTimeSpentMillSec() {
        for (int i = 0; i < 10; i++) {
            long startTime = System.currentTimeMillis();
            TimeUtil.sleepMilliSec(i);
            Logger.timeSpentMillSec("test", startTime, 2);
        }
//        System.out.println("===================================");
        for (int i = 0; i < 10; i++) {
            long startTime = System.currentTimeMillis();
            TimeUtil.sleepMilliSec(i);
            Logger.timeSpentMillSec("test", startTime, 2, 2);
        }
//        System.out.println("===================================");
    }

    @Test
    public void testTimeSpentNanSec() {
        final long sleepNanSec = 1000000;
        for (int i = 0; i < 10; i++) {
            long startTime = System.nanoTime();
            TimeUtil.sleepNanoSec(sleepNanSec);
            Logger.timeSpentNan("test", startTime, sleepNanSec);
        }
//        System.out.println("===================================");
        for (int i = 0; i < 10; i++) {
            long startTime = System.nanoTime();
            String s = "";
            for (int j = 0; j < 24; j++) {
                 s += j;
            }
            if (s.length()>100) {

            }
            Logger.timeSpentNan("test", startTime, sleepNanSec, sleepNanSec);
        }
//        System.out.println("===================================");
    }

    @Test
    public void testLogfCoverage() {
        Logger logger = new AlwaysEnabledLogger(LoggerBaseTest.class);
        Marker marker = MarkerFactory.getMarker("TEST");

        // 1. Test MissingFormatArgumentException recovery in _format
        logger.logf(marker, Level.INFO, "MissingArg: %s %s", "val");

        // 2. Test _parse logic with %% and %n
        logger.logf(marker, Level.INFO, "ParseSkip: %s %% %n %s", "val");

        // 3. Test IllegalFormatException (e.g. IllegalFormatConversionException) in logf
        logger.logf(marker, Level.INFO, "ConversionError: %d", "string");

        // 4. Test logf with Throwable as last argument
        logger.logf(marker, Level.ERROR, "WithThrowable: %s", "error", new RuntimeException("oops"));

        // 5. Test logf with no args but with specifier
        logger.logf(marker, Level.INFO, "NoArgs: %s");
    }

    @Test
    public void testLogObjectCoverage() {
        Logger logger = new AlwaysEnabledLogger(LoggerBaseTest.class);

        // Test log(Level level, Object object) with non-Throwable object
        logger.log(Level.INFO, "This is a simple object message");
        logger.log(Level.INFO, (Object) null);
    }

    @Test
    public void testTimeSpentCoverage() {
        long startTime = System.currentTimeMillis() - 100;

        // 1. Test timeSpent(String info, long startTime, TimeUnit unit, long threshold)
        Logger.timeSpent("testOverload", startTime, TimeUnit.MILLISECONDS, 10);

        // 2. Test timeSpent with delta <= 0 to hit "统一debug" branch
        Logger.timeSpent("testDebugBranch", startTime, TimeUnit.MILLISECONDS, 10, 0);
    }

    @Test
    public void testGetShortNameCoverage() {
        // Test all TimeUnit branches in getShortName via timeSpent
        // Each test ensures spent >= threshold to trigger getShortName usage

        // Test NANOSECONDS -> "ns"
        long nanoStart = System.nanoTime() - 1000000; // 1ms ago in nanos
        Logger.timeSpent("testNS", nanoStart, TimeUnit.NANOSECONDS, 100);

        // Test MICROSECONDS -> "micro"
        long microStart = TimeUnit.MICROSECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS) - 1000;
        Logger.timeSpent("testMicro", microStart, TimeUnit.MICROSECONDS, 100);

        // Test MILLISECONDS -> "ms"
        long msStart = System.currentTimeMillis() - 1000;
        Logger.timeSpent("testMS", msStart, TimeUnit.MILLISECONDS, 100);

        // Test SECONDS -> "sec"
        long secStart = TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS) - 10;
        Logger.timeSpent("testSec", secStart, TimeUnit.SECONDS, 1);

        // Test MINUTES -> "min"
        long minStart = TimeUnit.MINUTES.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS) - 10;
        Logger.timeSpent("testMin", minStart, TimeUnit.MINUTES, 1);

        // Test HOURS -> "hour"
        long hourStart = TimeUnit.HOURS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS) - 10;
        Logger.timeSpent("testHours", hourStart, TimeUnit.HOURS, 1);

        // Test DAYS -> "day"
        long dayStart = TimeUnit.DAYS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS) - 10;
        Logger.timeSpent("testDays", dayStart, TimeUnit.DAYS, 1);
    }

    @Test
    public void testSlf4jPlaceholderCoverage() {
        AlwaysEnabledLogger alwaysLogger = new AlwaysEnabledLogger(LoggerBaseTest.class);
        Marker testMarker = MarkerFactory.getMarker("TEST_MARKER");

        // Test slf4j placeholder "{}" branch - without marker
        alwaysLogger.trace("slf4j trace: {} - {}", "arg1", 123);
        alwaysLogger.debug("slf4j debug: {} - {}", "arg1", 123);
        alwaysLogger.info("slf4j info: {} - {}", "arg1", 123);
        alwaysLogger.warn("slf4j warn: {} - {}", "arg1", 123);
        alwaysLogger.error("slf4j error: {} - {}", "arg1", 123);

        // Test slf4j placeholder "{}" branch - with marker
        alwaysLogger.trace(testMarker, "slf4j trace with marker: {} - {}", "arg1", 123);
        alwaysLogger.debug(testMarker, "slf4j debug with marker: {} - {}", "arg1", 123);
        alwaysLogger.info(testMarker, "slf4j info with marker: {} - {}", "arg1", 123);
        alwaysLogger.warn(testMarker, "slf4j warn with marker: {} - {}", "arg1", 123);
        alwaysLogger.error(testMarker, "slf4j error with marker: {} - {}", "arg1", 123);

        // Test slf4j placeholder with single argument
        alwaysLogger.info("single arg: {}", "value");

        // Test slf4j placeholder with multiple arguments
        alwaysLogger.info("multiple args: {} {} {} {}", "a", "b", "c", "d");

        // Test slf4j placeholder with null argument
        alwaysLogger.info("null arg: {}", (Object) null);

        // Test slf4j placeholder with mixed types
        alwaysLogger.info("mixed types: {} {} {} {}", 123, true, 45.67, "string");

        // Test slf4j placeholder at different positions
        alwaysLogger.info("{} at start", "value");
        alwaysLogger.info("at end {}", "value");
        alwaysLogger.info("{} both {}", "start", "end");

        // Test empty format with placeholder
        alwaysLogger.info("{}", "just placeholder");
    }

    @Test
    public void testNonPlaceholderConcatMode() {
        // 测试模式 3：无占位符拼接模式
        // 当格式字符串既不包含 {} 也不包含 %s 等格式符时，会执行拼接逻辑
        AlwaysEnabledLogger alwaysLogger = new AlwaysEnabledLogger(LoggerBaseTest.class);
        Marker testMarker = MarkerFactory.getMarker("TEST_MARKER");

        // 1. Basic concat without marker - simple strings
        alwaysLogger.trace("Prefix", "arg1", "arg2");  // 应输出: Prefixarg1arg2
        alwaysLogger.debug("Debug:", "value1", "value2");
        alwaysLogger.info("Info", "concatenated");
        alwaysLogger.warn("Warn", "msg1", "msg2", "msg3");
        alwaysLogger.error("Error", "details");

        // 2. Concat with marker
        alwaysLogger.trace(testMarker, "Marker", "trace", "concat");
        alwaysLogger.debug(testMarker, "Marker", "debug", "concat");
        alwaysLogger.info(testMarker, "Marker", "info", "concat");
        alwaysLogger.warn(testMarker, "Marker", "warn", "concat");
        alwaysLogger.error(testMarker, "Marker", "error", "concat");

        // 3. Concat with different data types
        alwaysLogger.info("Number:", 123, "Boolean:", true, "Double:", 45.67);

        // 4. Concat with null values
        alwaysLogger.info("Before", null, "After");
        alwaysLogger.warn("Multiple", null, null, "values");

        // 5. Single argument concat (edge case)
        alwaysLogger.info("SingleValue");

        // 6. Concat with Throwable at the end
        Exception ex = new Exception("test exception in concat mode");
        alwaysLogger.error("Error occurred", "with", "context", ex);
        alwaysLogger.warn(testMarker, "Marker", "with", "exception", ex);

        // 7. Empty string concat
        alwaysLogger.info("", "starts", "empty");
        alwaysLogger.debug("Ends", "empty", "");

        // 8. Special characters (应该不触发格式化)
        alwaysLogger.info("Special", "chars:", "!@#$^&*()");

        // 9. Numbers and special objects
        alwaysLogger.info("ID:", 12345L, "Status:", Boolean.FALSE);

        // 10. Verify concat mode is NOT triggered by {} or %
        // (这些应该走 slf4j 或 formatter 模式，不是拼接模式)
        // 注意：这里只是确认边界，不应该在拼接模式测试中
    }

    @Test
    public void testConcatRecursiveBranch() {
        AlwaysEnabledLogger alwaysLogger = new AlwaysEnabledLogger(LoggerBaseTest.class);

        // Test 1: Basic array recursion - single level array
        Object[] simpleArray = new Object[]{"a", "b", "c"};
        alwaysLogger.info("Simple array: ", simpleArray);

        // Test 2: Nested array recursion - 2 levels
        Object[] innerArray = new Object[]{"inner1", "inner2"};
        Object[] outerArray = new Object[]{"outer1", innerArray, "outer2"};
        alwaysLogger.info("Nested array: ", outerArray);

        // Test 3: Deeply nested array - 3 levels
        Object[] level1 = new Object[]{"L1-a", "L1-b"};
        Object[] level2 = new Object[]{"L2-start", level1, "L2-end"};
        Object[] level3 = new Object[]{"L3-start", level2, "L3-end"};
        alwaysLogger.info("Deep nested: ", level3);

        // Test 4: Mixed objects and arrays
        Object[] mixedArray = new Object[]{"str", 123, true};
        alwaysLogger.info("Prefix", mixedArray, "Suffix");

        // Test 5: Multiple arrays in arguments
        Object[] array1 = new Object[]{"a1", "a2"};
        Object[] array2 = new Object[]{"b1", "b2"};
        alwaysLogger.info("Multiple: ", array1, " and ", array2);

        // Test 6: Array with null elements
        Object[] arrayWithNull = new Object[]{"before", null, "after"};
        alwaysLogger.info("Array with null: ", arrayWithNull);

        // Test 7: Empty array
        Object[] emptyArray = new Object[]{};
        alwaysLogger.info("Empty array: ", emptyArray);

        // Test 8: Array ending with Throwable (should stop before Throwable)
        Exception ex = new Exception("test exception");
        Object[] arrayWithException = new Object[]{"msg1", "msg2", ex};
        alwaysLogger.info("Array with exception: ", arrayWithException);

        // Test 9: Nested array with Throwable at end
        Object[] innerWithEx = new Object[]{"inner", ex};
        Object[] outerWithEx = new Object[]{"outer", innerWithEx};
        alwaysLogger.info("Nested with exception: ", outerWithEx);

        // Test 10: Array of different types (Integer[], String[], etc.)
        Integer[] intArray = new Integer[]{1, 2, 3};
        String[] strArray = new String[]{"x", "y", "z"};
        alwaysLogger.info("Type arrays: ", intArray, " - ", strArray);

        // Test 11: Complex nested structure
        Object[] complex = new Object[]{
            "start",
            new Object[]{"level2-a", new Object[]{"level3"}},
            123,
            new Object[]{"level2-b"},
            "end"
        };
        alwaysLogger.info("Complex: ", complex);
    }

    @Test
    public void testResolveClassNameCoverage() {
        String NA = "UnknowClass";
        String LINE_SEP = System.getProperty("line.separator");
        String CLASSNAME = "io.hqwu.commons.util.Logger.";

        // 1. CLASSNAME not found
        String stack1 = "some random stack trace";
        assertEquals(NA, LoggerBase.resolveClassName(CLASSNAME, stack1));

        // 2. LINE_SEP after CLASSNAME not found
        String stack2 = "at io.hqwu.commons.util.Logger.method(Logger.java:10)";
        assertEquals(NA, LoggerBase.resolveClassName(CLASSNAME, stack2));

        // 3. End of line not found
        String stack3 = "at io.hqwu.commons.util.Logger.method(Logger.java:10)" + LINE_SEP + "at caller";
        // Note: The logic searches for LINE_SEP starting from ibegin (which is after the first LINE_SEP)
        // So we need at least two lines after the match? Let's trace the code.
        // ibegin = stack.lastIndexOf(CLASSNAME);
        // ibegin = stack.indexOf(LINE_SEP, ibegin); -> finds first newline
        // ibegin += LINE_SEP_LEN;
        // iend = stack.indexOf(LINE_SEP, ibegin); -> finds second newline
        // if iend == -1 return NA
        assertEquals(NA, LoggerBase.resolveClassName(CLASSNAME, stack3));

        // 4. "at " not found
        String stack4 = "io.hqwu.commons.util.Logger.method(Logger.java:10)" + LINE_SEP +
                "invalid line format" + LINE_SEP;
        assertEquals(NA, LoggerBase.resolveClassName(CLASSNAME, stack4));

        // 5. '(' not found
        String stack5 = "at io.hqwu.commons.util.Logger.method(Logger.java:10)" + LINE_SEP +
                "at com.example.Caller.method" + LINE_SEP;
        assertEquals(NA, LoggerBase.resolveClassName(CLASSNAME, stack5));

        // 6. '.' before '(' not found (VisualAge style check?)
        // iend = fullInfo.lastIndexOf('(');
        // iend = fullInfo.lastIndexOf('.', iend);
        String stack6 = "at io.hqwu.commons.util.Logger.method(Logger.java:10)" + LINE_SEP +
                "at method(Caller.java:10)" + LINE_SEP;
        assertEquals(NA, LoggerBase.resolveClassName(CLASSNAME, stack6));

        // 7. Success case
        String stack7 = "at io.hqwu.commons.util.Logger.method(Logger.java:10)" + LINE_SEP +
                "at com.example.Caller.method(Caller.java:20)" + LINE_SEP;
        assertEquals("com.example.Caller", LoggerBase.resolveClassName(CLASSNAME, stack7));
    }

    // ...existing code...

    /**
     * 测试新增的 xxxf() 方法（显式格式化方法）
     * 这些方法是为了明确使用 printf 风格格式化而新增的
     */
    @Test
    public void testTracefMethod() {
        // Test tracef with printf style format
        logger.tracef("tracef: %s %d %b", "test", 123, true);
        logger.tracef("tracef with single arg: %s", "value");
        logger.tracef("tracef with multiple format: %s-%d-%.2f", "str", 42, 3.14);

        // Test with exception at the end
        Exception ex = new Exception("test exception");
        logger.tracef("tracef with exception: %s", ex);
    }

    @Test
    public void testDebugfMethod() {
        // Test debugf with printf style format
        logger.debugf("debugf: %s %d %b", "test", 123, true);
        logger.debugf("debugf with single arg: %s", "value");
        logger.debugf("debugf with multiple format: %s-%d-%.2f", "str", 42, 3.14);

        // Test with exception at the end
        Exception ex = new Exception("test exception");
        logger.debugf("debugf with exception: %s", ex);
    }

    @Test
    public void testInfofMethod() {
        // Test infof with printf style format
        logger.infof("infof: %s %d %b", "test", 123, true);
        logger.infof("infof with single arg: %s", "value");
        logger.infof("infof with multiple format: %s-%d-%.2f", "str", 42, 3.14);

        // Test with exception at the end
        Exception ex = new Exception("test exception");
        logger.infof("infof with exception: %s", ex);
    }

    @Test
    public void testWarnfMethod() {
        // Test warnf with printf style format
        logger.warnf("warnf: %s %d %b", "test", 123, true);
        logger.warnf("warnf with single arg: %s", "value");
        logger.warnf("warnf with multiple format: %s-%d-%.2f", "str", 42, 3.14);

        // Test with exception at the end
        Exception ex = new Exception("test exception");
        logger.warnf("warnf with exception: %s", ex);
    }

    @Test
    public void testErrorfMethod() {
        // Test errorf with printf style format
        logger.errorf("errorf: %s %d %b", "test", 123, true);
        logger.errorf("errorf with single arg: %s", "value");
        logger.errorf("errorf with multiple format: %s-%d-%.2f", "str", 42, 3.14);

        // Test with exception at the end
        Exception ex = new Exception("test exception");
        logger.errorf("errorf with exception: %s", ex);
    }

    @Test
    public void testAllFormatMethodsWithVariousTypes() {
        // Test all xxxf() methods with various printf format specifiers
        AlwaysEnabledLogger alwaysLogger = new AlwaysEnabledLogger(LoggerBaseTest.class);

        // String format
        alwaysLogger.tracef("String: %s", "hello");
        alwaysLogger.debugf("String: %s", "world");
        alwaysLogger.infof("String: %s", "test");
        alwaysLogger.warnf("String: %s", "warning");
        alwaysLogger.errorf("String: %s", "error");

        // Integer format
        alwaysLogger.tracef("Integer: %d", 123);
        alwaysLogger.debugf("Hex: %x", 255);
        alwaysLogger.infof("Octal: %o", 8);

        // Float format
        alwaysLogger.warnf("Float: %.2f", 3.14159);
        alwaysLogger.errorf("Scientific: %e", 1234.5);

        // Boolean format
        alwaysLogger.tracef("Boolean: %b", true);
        alwaysLogger.debugf("Boolean: %b", false);

        // Multiple arguments
        alwaysLogger.infof("Multiple: %s %d %.2f %b", "test", 42, 3.14, true);
        alwaysLogger.warnf("Args: %s-%d-%s", "a", 1, "b");
        alwaysLogger.errorf("Triple: %s %s %s", "one", "two", "three");

        // Edge cases
        alwaysLogger.infof("Null: %s", (Object) null);
        alwaysLogger.debugf("Empty string: %s", "");
        alwaysLogger.tracef("Special chars: %s", "!@#$%^&*()");
    }

    @Test
    public void testFormatMethodsVsStandardMethods() {
        // Verify that xxxf() methods behave consistently with standard xxx() methods
        // when using printf format
        AlwaysEnabledLogger alwaysLogger = new AlwaysEnabledLogger(LoggerBaseTest.class);

        // Both should produce similar output for printf format
        alwaysLogger.debugf("Printf via debugf: %s-%d", "test", 123);
        alwaysLogger.debug("Printf via debug: %s-%d", "test", 123);

        alwaysLogger.infof("Printf via infof: %s", "value");
        alwaysLogger.info("Printf via info: %s", "value");

        // xxxf() methods make the intent clearer that printf formatting is being used
    }

    static class AlwaysEnabledLogger extends Logger {
        public AlwaysEnabledLogger(Class<?> clazz) {
            super(clazz);
        }

        @Override
        protected boolean isLevelEnabled(Level level) {
            return true;
        }
    }
}
