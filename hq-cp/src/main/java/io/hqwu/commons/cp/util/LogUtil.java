package io.hqwu.commons.cp.util;

import org.slf4j.Logger;

/**
 * 日志工具类，提供基于执行耗时阈值的动态日志记录功能。
 *
 * <p>该工具类允许根据操作的实际执行时间与预设的警告 (Warn) 和信息 (Info) 阈值进行比较，
 * 从而自动选择合适的 {@link Logger} 级别进行输出。主要用于性能监控和慢操作审计。
 *
 * @author taige (Wu, Hongqiang)
 * @since 2025-02-27
 */
public class LogUtil {

    /**
     * Log based on thresholds.
     * @param logger 日志记录器
     * @param usedTimeMillis 执行时间(ms)
     * @param infoThresholdMillis 信息级别阈值(ms)
     * @param warnThresholdMillis 警告级别阈值(ms)
     * @param message 日志消息
     * @param messageParts 日志消息参数
     */
    public static void logBasedOnThreshold(
            Logger logger, long usedTimeMillis, long infoThresholdMillis, long warnThresholdMillis, String message, Object... messageParts) {
        if (usedTimeMillis > warnThresholdMillis && logger.isWarnEnabled()) {
            logger.warn(message, messageParts);
        } else if (usedTimeMillis > infoThresholdMillis && logger.isInfoEnabled()) {
            logger.info(message, messageParts);
        } else if (logger.isDebugEnabled()) {
            logger.debug(message, messageParts);
        }
    }

    /**
     * Determines if logging is enabled based on the execution time and the specified logging thresholds.
     *
     * @param logger the logger instance used to check the enabled logging levels
     * @param usedTimeMillis the time taken for execution in milliseconds
     * @param infoThresholdMillis the threshold in milliseconds for enabling info-level logging
     * @param warnThresholdMillis the threshold in milliseconds for enabling warn-level logging
     * @return true if the logger is enabled for warn-level, info-level, or debug-level logging based on the execution time and thresholds; false otherwise
     */
    public static boolean isEnabled(Logger logger, long usedTimeMillis, long infoThresholdMillis, long warnThresholdMillis) {
        if (usedTimeMillis > warnThresholdMillis && logger.isWarnEnabled()) {
            return true;
        } else if (usedTimeMillis > infoThresholdMillis && logger.isInfoEnabled()) {
            return true;
        } else return logger.isDebugEnabled();
    }

}
