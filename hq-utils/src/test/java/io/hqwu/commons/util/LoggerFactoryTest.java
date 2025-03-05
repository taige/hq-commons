package io.hqwu.commons.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * Date: 13-10-29
 * Time: 下午2:11
 *
 */
public class LoggerFactoryTest {

    @Test
    public void testGetLogger() throws Exception {
        Logger logger = LoggerFactory.getLogger();
        assertNotNull(logger);
        new Logger().info("empty");
        logger.info("empty");
    }

    @Test
    public void testGetLoggerString() throws Exception {
        Logger logger = LoggerFactory.getLogger("testGetLoggerString");
        assertNotNull(logger);
        logger.info("testGetLoggerString");
    }

    @Test
    public void testGetLoggerClass() throws Exception {
        Logger logger = LoggerFactory.getLogger(LoggerFactoryTest.class);
        assertNotNull(logger);
        logger.info("testGetLoggerClass");
    }
}
