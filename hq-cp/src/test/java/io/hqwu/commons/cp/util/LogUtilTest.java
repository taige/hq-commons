package io.hqwu.commons.cp.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import static org.mockito.Mockito.*;

abstract class AbstractLogger implements Logger {
     private Level level = Level.ERROR; // 默认启用最高级别阈值（仅 ERROR）

     // 日志启用状态检查，确保级别依赖
     @Override
     public boolean isDebugEnabled() {
         return level.toInt() <= Level.DEBUG.toInt();
     }

     @Override
     public boolean isInfoEnabled() {
         return level.toInt() <= Level.INFO.toInt();
     }

     @Override
     public boolean isWarnEnabled() {
         return level.toInt() <= Level.WARN.toInt();
     }

     public void setLevel(Level level) {
         this.level = level;
     }

}

@ExtendWith(MockitoExtension.class)
public class LogUtilTest {

    @Mock
    private AbstractLogger logger;

    private static final String MESSAGE_TEMPLATE = "Operation took {} ms";
    private static final Object[] MESSAGE_PARTS = new Object[]{"123"};

    @BeforeEach
    public void setUp() {
        reset(logger);
        // 配置 setLevel 调用真实方法
        doCallRealMethod().when(logger).setLevel(any(Level.class));
        // 配置级别检查方法调用真实方法
        lenient().when(logger.isDebugEnabled()).thenCallRealMethod();
        lenient().when(logger.isInfoEnabled()).thenCallRealMethod();
        lenient().when(logger.isWarnEnabled()).thenCallRealMethod();
    }

    /**
     * 测试 {@link LogUtil#logBasedOnThreshold} 方法，通过参数化输入验证日志记录行为。
     * 本测试覆盖了多种启用日志级别、时间阈值和预期日志输出的组合，
     * 包括常规阈值以及 {@code infoThreshold} 和/或 {@code warnThreshold} 设置为 {@code Long.MAX_VALUE} 的场景。
     *
     * @param levelEnabled    启用的日志级别 ({@link Level#ERROR}, {@link Level#WARN}, {@link Level#INFO}, {@link Level#DEBUG})
     * @param usedTimeMillis  用于与阈值比较的时间值
     * @param infoThresholdStr INFO阈值，字符串形式（"Long.MAX_VALUE" 或数值）
     * @param warnThresholdStr WARN阈值，字符串形式（"Long.MAX_VALUE" 或数值）
     * @param expectedLog     预期的日志级别 ({@link Level#WARN}, {@link Level#INFO}, {@link Level#DEBUG}, {@link Level#ERROR}，其中 ERROR 表示无日志输出)
     */
    @ParameterizedTest
    @CsvSource({
            // Conventional thresholds (infoThreshold=500, warnThreshold=1000)
            // Time > warnThreshold (1500)
            "WARN,  1500, 500, 1000, WARN",   // WARN enabled, expect WARN
            "INFO,  1500, 500, 1000, WARN",   // INFO enabled (WARN also enabled), expect WARN
            "DEBUG, 1500, 500, 1000, WARN",   // DEBUG enabled (WARN also enabled), expect WARN
            "ERROR, 1500, 500, 1000, ERROR",  // All disabled, expect no log (ERROR)
            // infoThreshold < Time <= warnThreshold (750)
            "INFO,  750,  500, 1000, INFO",   // INFO enabled, expect INFO
            "DEBUG, 750,  500, 1000, INFO",   // DEBUG enabled (INFO also enabled), expect INFO
            "WARN,  750,  500, 1000, ERROR",  // WARN enabled, expect no log (ERROR)
            "ERROR, 750,  500, 1000, ERROR",  // All disabled, expect no log (ERROR)
            // Time <= infoThreshold (400)
            "DEBUG, 400,  500, 1000, DEBUG",  // DEBUG enabled, expect DEBUG
            "INFO,  400,  500, 1000, ERROR",  // INFO enabled, expect no log (ERROR)
            "WARN,  400,  500, 1000, ERROR",  // WARN enabled, expect no log (ERROR)
            "ERROR, 400,  500, 1000, ERROR",  // All disabled, expect no log (ERROR)

            // warnThreshold = Long.MAX_VALUE, infoThreshold < Long.MAX_VALUE (500)
            // infoThreshold < Time <= Long.MAX_VALUE (1000)
            "INFO,  1000, 500, Long.MAX_VALUE, INFO",   // INFO enabled, expect INFO
            "DEBUG, 1000, 500, Long.MAX_VALUE, INFO",   // DEBUG enabled (INFO also enabled), expect INFO
            "WARN,  1000, 500, Long.MAX_VALUE, ERROR",  // WARN enabled, expect no log (ERROR)
            "ERROR, 1000, 500, Long.MAX_VALUE, ERROR",  // All disabled, expect no log (ERROR)
            // Time <= infoThreshold (400)
            "DEBUG, 400,  500, Long.MAX_VALUE, DEBUG",  // DEBUG enabled, expect DEBUG
            "INFO,  400,  500, Long.MAX_VALUE, ERROR",  // INFO enabled, expect no log (ERROR)
            "WARN,  400,  500, Long.MAX_VALUE, ERROR",  // WARN enabled, expect no log (ERROR)
            "ERROR, 400,  500, Long.MAX_VALUE, ERROR",  // All disabled, expect no log (ERROR)

            // infoThreshold = Long.MAX_VALUE, warnThreshold < Long.MAX_VALUE (1000)
            // Time > warnThreshold (1500)
            "WARN,  1500, Long.MAX_VALUE, 1000, WARN",   // WARN enabled, expect WARN
            "INFO,  1500, Long.MAX_VALUE, 1000, WARN",   // INFO enabled (WARN also enabled), expect WARN
            "DEBUG, 1500, Long.MAX_VALUE, 1000, WARN",   // DEBUG enabled (WARN also enabled), expect WARN
            "ERROR, 1500, Long.MAX_VALUE, 1000, ERROR",  // All disabled, expect no log (ERROR)
            // Time <= warnThreshold (500)
            "DEBUG, 500,  Long.MAX_VALUE, 1000, DEBUG",  // DEBUG enabled, expect DEBUG
            "INFO,  500,  Long.MAX_VALUE, 1000, ERROR",  // INFO enabled, expect no log (ERROR)
            "WARN,  500,  Long.MAX_VALUE, 1000, ERROR",  // WARN enabled, expect no log (ERROR)
            "ERROR, 500,  Long.MAX_VALUE, 1000, ERROR",  // All disabled, expect no log (ERROR)

            // warnThreshold = Long.MAX_VALUE, infoThreshold = Long.MAX_VALUE
            // Time <= Long.MAX_VALUE (1000)
            "DEBUG, 1000, Long.MAX_VALUE, Long.MAX_VALUE, DEBUG",  // DEBUG enabled, expect DEBUG
            "INFO,  1000, Long.MAX_VALUE, Long.MAX_VALUE, ERROR",  // INFO enabled, expect no log (ERROR)
            "WARN,  1000, Long.MAX_VALUE, Long.MAX_VALUE, ERROR",  // WARN enabled, expect no log (ERROR)
            "ERROR, 1000, Long.MAX_VALUE, Long.MAX_VALUE, ERROR"   // All disabled, expect no log (ERROR)
    })
    public void testLogBasedOnThreshold(Level levelEnabled, long usedTimeMillis, String infoThresholdStr, String warnThresholdStr, Level expectedLog)  {
        long infoThreshold = infoThresholdStr.equals("Long.MAX_VALUE") ? Long.MAX_VALUE : Long.parseLong(infoThresholdStr);
        long warnThreshold = warnThresholdStr.equals("Long.MAX_VALUE") ? Long.MAX_VALUE : Long.parseLong(warnThresholdStr);
        logger.setLevel(levelEnabled);

        LogUtil.logBasedOnThreshold(logger, usedTimeMillis, infoThreshold, warnThreshold, MESSAGE_TEMPLATE, MESSAGE_PARTS);

        switch (expectedLog) {
            case WARN:
                verify(logger).warn(MESSAGE_TEMPLATE, MESSAGE_PARTS);
                verify(logger, never()).info(anyString(), any(Object[].class));
                verify(logger, never()).debug(anyString(), any(Object[].class));
                break;
            case INFO:
                verify(logger, never()).warn(anyString(), any(Object[].class));
                verify(logger).info(MESSAGE_TEMPLATE, MESSAGE_PARTS);
                verify(logger, never()).debug(anyString(), any(Object[].class));
                break;
            case DEBUG:
                verify(logger, never()).warn(anyString(), any(Object[].class));
                verify(logger, never()).info(anyString(), any(Object[].class));
                verify(logger).debug(MESSAGE_TEMPLATE, MESSAGE_PARTS);
                break;
            case ERROR:
                verify(logger, never()).warn(anyString(), any(Object[].class));
                verify(logger, never()).info(anyString(), any(Object[].class));
                verify(logger, never()).debug(anyString(), any(Object[].class));
                break;
            default:
                throw new IllegalArgumentException("Unexpected expectedLog value: " + expectedLog);
        }
    }

    /**
     * 测试 {@link LogUtil#isEnabled} 方法，通过参数化输入验证是否应启用日志记录。
     * 本测试覆盖了多种启用日志级别、时间阈值和预期结果的组合，
     * 包括常规阈值以及 {@code infoThreshold} 和/或 {@code warnThreshold} 设置为 {@code Long.MAX_VALUE} 的场景。
     *
     * @param levelEnabled    启用的日志级别 ({@link Level#ERROR}, {@link Level#WARN}, {@link Level#INFO}, {@link Level#DEBUG})
     * @param usedTimeMillis  用于与阈值比较的时间值
     * @param infoThresholdStr INFO阈值，字符串形式（"Long.MAX_VALUE" 或数值）
     * @param warnThresholdStr WARN阈值，字符串形式（"Long.MAX_VALUE" 或数值）
     * @param expectedEnabled 预期的返回值（true 表示应启用日志，false 表示不应启用）
     */
    @ParameterizedTest
    @CsvSource({
            // Conventional thresholds (infoThreshold=500, warnThreshold=1000)
            // Time > warnThreshold (1500)
            "WARN,  1500, 500, 1000, true",   // WARN enabled, expect true
            "INFO,  1500, 500, 1000, true",   // INFO enabled (WARN also enabled), expect true
            "DEBUG, 1500, 500, 1000, true",   // DEBUG enabled (WARN also enabled), expect true
            "ERROR, 1500, 500, 1000, false",  // All disabled, expect false
            // infoThreshold < Time <= warnThreshold (750)
            "INFO,  750,  500, 1000, true",   // INFO enabled, expect true
            "DEBUG, 750,  500, 1000, true",   // DEBUG enabled (INFO also enabled), expect true
            "WARN,  750,  500, 1000, false",  // WARN enabled, expect false
            "ERROR, 750,  500, 1000, false",  // All disabled, expect false
            // Time <= infoThreshold (400)
            "DEBUG, 400,  500, 1000, true",   // DEBUG enabled, expect true
            "INFO,  400,  500, 1000, false",  // INFO enabled, expect false
            "WARN,  400,  500, 1000, false",  // WARN enabled, expect false
            "ERROR, 400,  500, 1000, false",  // All disabled, expect false

            // warnThreshold = Long.MAX_VALUE, infoThreshold < Long.MAX_VALUE (500)
            // infoThreshold < Time <= Long.MAX_VALUE (1000)
            "INFO,  1000, 500, Long.MAX_VALUE, true",   // INFO enabled, expect true
            "DEBUG, 1000, 500, Long.MAX_VALUE, true",   // DEBUG enabled (INFO also enabled), expect true
            "WARN,  1000, 500, Long.MAX_VALUE, false",  // WARN enabled, expect false
            "ERROR, 1000, 500, Long.MAX_VALUE, false",  // All disabled, expect false
            // Time <= infoThreshold (400)
            "DEBUG, 400,  500, Long.MAX_VALUE, true",   // DEBUG enabled, expect true
            "INFO,  400,  500, Long.MAX_VALUE, false",  // INFO enabled, expect false
            "WARN,  400,  500, Long.MAX_VALUE, false",  // WARN enabled, expect false
            "ERROR, 400,  500, Long.MAX_VALUE, false",  // All disabled, expect false

            // infoThreshold = Long.MAX_VALUE, warnThreshold < Long.MAX_VALUE (1000)
            // Time > warnThreshold (1500)
            "WARN,  1500, Long.MAX_VALUE, 1000, true",   // WARN enabled, expect true
            "INFO,  1500, Long.MAX_VALUE, 1000, true",   // INFO enabled (WARN also enabled), expect true
            "DEBUG, 1500, Long.MAX_VALUE, 1000, true",   // DEBUG enabled (WARN also enabled), expect true
            "ERROR, 1500, Long.MAX_VALUE, 1000, false",  // All disabled, expect false
            // Time <= warnThreshold (500)
            "DEBUG, 500,  Long.MAX_VALUE, 1000, true",   // DEBUG enabled, expect true
            "INFO,  500,  Long.MAX_VALUE, 1000, false",  // INFO enabled, expect false
            "WARN,  500,  Long.MAX_VALUE, 1000, false",  // WARN enabled, expect false
            "ERROR, 500,  Long.MAX_VALUE, 1000, false",  // All disabled, expect false

            // warnThreshold = Long.MAX_VALUE, infoThreshold = Long.MAX_VALUE
            // Time <= Long.MAX_VALUE (1000)
            "DEBUG, 1000, Long.MAX_VALUE, Long.MAX_VALUE, true",   // DEBUG enabled, expect true
            "INFO,  1000, Long.MAX_VALUE, Long.MAX_VALUE, false",  // INFO enabled, expect false
            "WARN,  1000, Long.MAX_VALUE, Long.MAX_VALUE, false",  // WARN enabled, expect false
            "ERROR, 1000, Long.MAX_VALUE, Long.MAX_VALUE, false"   // All disabled, expect false
    })
    public void testIsEnabled(Level levelEnabled, long usedTimeMillis, String infoThresholdStr, String warnThresholdStr, boolean expectedEnabled) {
        long infoThreshold = infoThresholdStr.equals("Long.MAX_VALUE") ? Long.MAX_VALUE : Long.parseLong(infoThresholdStr);
        long warnThreshold = warnThresholdStr.equals("Long.MAX_VALUE") ? Long.MAX_VALUE : Long.parseLong(warnThresholdStr);
        logger.setLevel(levelEnabled);

        boolean result = LogUtil.isEnabled(logger, usedTimeMillis, infoThreshold, warnThreshold);

        if (expectedEnabled) {
            assert result : "Expected isEnabled to return true for levelEnabled=" + levelEnabled
                    + ", usedTimeMillis=" + usedTimeMillis
                    + ", infoThreshold=" + infoThreshold
                    + ", warnThreshold=" + warnThreshold;
        } else {
            assert !result : "Expected isEnabled to return false for levelEnabled=" + levelEnabled
                    + ", usedTimeMillis=" + usedTimeMillis
                    + ", infoThreshold=" + infoThreshold
                    + ", warnThreshold=" + warnThreshold;
        }
    }

}
