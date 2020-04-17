package com.umpay.commons.util;

import org.junit.jupiter.api.Test;

/**
 * Created with IntelliJ IDEA.
 * User: zhangyao
 * Date: 18-4-26
 * Time: 上午9:44
 * To change this template use File | Settings | File Templates.
 */
public class Base64UtilTest {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Test
    public void test() throws Exception {
        LOGGER.info(Base64Util.encryptBASE64("unpay:test".getBytes()));
    }
}
