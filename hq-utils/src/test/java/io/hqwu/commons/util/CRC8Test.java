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

    @Test
    void test_calcCrc8_withOffsetAndLen() {
        // 测试 calcCrc8(byte[] data, int offset, int len) 方法
        byte[] data = "Hello World".getBytes();

        // 测试从起始位置开始，计算部分数据的 CRC8
        byte crc1 = CRC8.calcCrc8(data, 0, 5); // "Hello"
        byte crc2 = CRC8.calcCrc8("Hello".getBytes());
        assertEquals(crc2, crc1);

        // 测试从中间位置开始
        byte crc3 = CRC8.calcCrc8(data, 6, 5); // "World"
        byte crc4 = CRC8.calcCrc8("World".getBytes());
        assertEquals(crc4, crc3);

        // 测试计算整个数组
        byte crc5 = CRC8.calcCrc8(data, 0, data.length);
        byte crc6 = CRC8.calcCrc8(data);
        assertEquals(crc6, crc5);

        // 测试单个字节
        byte crc7 = CRC8.calcCrc8(data, 0, 1); // "H"
        byte crc8 = CRC8.calcCrc8("H".getBytes());
        assertEquals(crc8, crc7);
    }

}

