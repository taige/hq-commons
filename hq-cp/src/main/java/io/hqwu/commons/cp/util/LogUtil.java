package io.hqwu.commons.cp.util;

import org.slf4j.Logger;

/**
 * Created with IntelliJ IDEA for hq-commons-parent
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2025-02-27
 * Time: 17:44
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
    public static void logBasedOnThreshold(Logger logger, long usedTimeMillis, long infoThresholdMillis, long warnThresholdMillis, String message, Object... messageParts) {
        if (usedTimeMillis > warnThresholdMillis && logger.isWarnEnabled()) {
            logger.warn(message, messageParts);
        } else if (usedTimeMillis > infoThresholdMillis && logger.isInfoEnabled()) {
            logger.info(message, messageParts);
        } else if (logger.isDebugEnabled()) {
            logger.debug(message, messageParts);
        }
    }

}
