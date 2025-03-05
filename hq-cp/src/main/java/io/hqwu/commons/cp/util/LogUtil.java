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
     * @param logger
     * @param usedTimeMillis
     * @param infoThresholdMillis
     * @param warnThresholdMillis
     * @param message
     * @param messageParts
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
