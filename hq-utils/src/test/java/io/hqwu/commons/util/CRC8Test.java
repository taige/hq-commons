package io.hqwu.commons.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created with IntelliJ IDEA for hq-commons
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-01-15
 * Time: 2:42 p.m.
 */
class CRC8Test {
    private static final Logger LOGGER = new Logger();

    @Test
    void test_calcCrc8_2bI() {
        assertEquals((byte) 0x80, CRC8.calcCrc8("2bI".getBytes()));
    }

}