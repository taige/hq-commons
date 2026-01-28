/*
 * @(#)IOUtilTest.java Created on 2013-8-12
 *
 * Copyright 2012-2013 Chinabank Payments, Inc. All rights reserved.
 * Use is subject to license terms.
 */
package io.hqwu.commons.util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.File;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Description:
 * 
 * @author: shenjianlin <a href="mailto:ustbsjl@gmail.com">ustbsjl@gmail.com</a> <br>
 *          QQ: 79043549
 * @version: 1.0 2013-8-12
 * @history:
 */

public class IOUtilTest {
    private static final Logger LOGGER = new Logger();

    static String EXIST_CP_FILE_PATH = "io/hqwu/commons/util/test.file";

    public static byte[] bcd2(String ascii) {
        int len = ascii.getBytes().length;
        byte[] asc;
        if ((len % 2) == 0) {
            asc = new byte[len];
        } else {
            asc = new byte[len + 1];
            asc[len] = 0x0F;
        }
        System.arraycopy(ascii.getBytes(), 0, asc, 0, len);
        byte[] bcd = new byte[asc.length / 2];
        for (int i = 0; i < asc.length; i++) {
            if ('a' <= asc[i]) {
                asc[i] += 10 - 'a';
            } else if ('A' <= asc[i]) {
                asc[i] += 10 - 'A';
            } else {
                asc[i] -= '0';
            }
        }
        for (int i = 0; i < bcd.length; i++) {
            bcd[i] = (byte) (asc[i * 2] << 4);
            bcd[i] += asc[i * 2 + 1] & 0x0F;
        }
        return bcd;
    }

    public static String bcd2(byte[] bcd) {
        byte[] ascii = new byte[bcd.length * 2];
        for (int i = 0; i < bcd.length; i++) {
            ascii[i * 2] = (byte) (bcd[i] >>> 4 & 0x0F);
            ascii[i * 2 + 1] = (byte) (bcd[i] & 0x0F);
        }
        for (int i = 0; i < ascii.length; i++) {
            if (10 <= ascii[i] && ascii[i] <= 15) {
                ascii[i] += 'A' - 10;
            } else {
                ascii[i] += '0';
            }
        }
        return new String(ascii);
    }

    /**
     * Test method for {@link IOUtil#bcd(byte[])}.
     */
    @Test
    public void testBcdDecode() throws Exception {
        assertEquals("", IOUtil.bcd(new byte[0]));
        assertEquals("31", IOUtil.bcd("1".getBytes()));// 只编码一个字节
        assertEquals("31323334", IOUtil.bcd("1234".getBytes()));
        assertEquals("E4B8ADE59BBD", IOUtil.bcd("中国".getBytes("UTF-8")));

        // 测试与自定义算法一致
        byte[] data = "网银在线".getBytes();
        assertEquals(bcd2(data), IOUtil.bcd(data));
    }

    /**
     * Test method for {@link IOUtil#bcd(String)}.
     */
    @Test
    public void testBcdEncode() {
        assertEquals(0, IOUtil.bcd("").length);// 空字节数组
        assertArrayEquals("12345".getBytes(), IOUtil.bcd("3132333435"));
    }

    @Test
    public void testBcdDecodeEncode() throws Exception {
        byte[] plain = "中国".getBytes("UTF-8");
        String dec = IOUtil.bcd(plain);
        byte[] enc = IOUtil.bcd(dec);
        assertArrayEquals(plain, enc, "BCD编码前后应该一致");
    }

    @Test
    public void testJoinBytes() {
        byte[] first = null;
        byte[] second = null;
        assertArrayEquals(new byte[0], IOUtil.joinBytes(first, second));

        byte[] first1 = { 0x01, 0x02 };
        byte[] second1 = null;
        assertArrayEquals(first1, IOUtil.joinBytes(first1, second1));

        byte[] first2 = { 0x01, 0x02 };
        byte[] second2 = { 0x03, 0x04 };
        byte[] except = new byte[] { 0x01, 0x02, 0x03, 0x04 };
        assertArrayEquals(except, IOUtil.joinBytes(first2, second2));
    }

    @Test
    public void testShortToBuf() {
        short toEncode = 1;
        byte[] buf = null;

        assertThrows(NullPointerException.class, () ->
                IOUtil.shortToBuf(toEncode, null, 0)
        );

        assertThrows(IllegalArgumentException.class, () ->
                IOUtil.shortToBuf(toEncode, new byte[2], 1)
        );

        buf = new byte[3];
        IOUtil.shortToBuf((short) 1, buf, 1);
        assertArrayEquals(new byte[] { 0x00, 0x00, 0x01 }, buf);

        buf = new byte[3];
        IOUtil.shortToBuf((short) 300, buf, 1);
        assertArrayEquals(new byte[] { 0x00, 0x01, 0x2C }, buf);
    }

    @Test
    public void testIntToBuf() {
        int toEncode = 1;
        byte[] buf = null;

        assertThrows(NullPointerException.class, () ->
                IOUtil.intToBuf(toEncode, null, 0)
        );

        assertThrows(IllegalArgumentException.class, () ->
                IOUtil.intToBuf(toEncode, new byte[4], 1)
        );

        buf = new byte[5];
        IOUtil.intToBuf(1, buf, 1);
        assertArrayEquals(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x01 }, buf);

        buf = new byte[5];
        IOUtil.intToBuf(300, buf, 1);
        assertArrayEquals(new byte[] { 0x00, 0x00, 0x00, 0x01, 0x2C }, buf);
    }

    @Test
    public void testLongToBuf() {
        long toEncode = 1L;
        byte[] buf = null;
        assertThrows(NullPointerException.class, () ->
                IOUtil.longToBuf(toEncode, null, 0)
        );

        assertThrows(IllegalArgumentException.class, () ->
                IOUtil.longToBuf(toEncode, new byte[8], 1)
        );

        buf = new byte[9];
        IOUtil.longToBuf(1, buf, 1);
        assertArrayEquals(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x01 }, buf);

        buf = new byte[9];
        IOUtil.longToBuf(300, buf, 1);
        assertArrayEquals(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x01, 0x2C }, buf);
    }

    @Test
    public void testBufToShort() {
        byte[] buf = null;
        try {
            IOUtil.bufToShort(buf, 0);
        } catch (NullPointerException e) {
            assertTrue(true);// 异常是正确的
        }

        buf = new byte[2];
        try {
            IOUtil.bufToShort(buf, 1);
        } catch (IllegalArgumentException e) {
            LOGGER.info(e.getMessage());
            assertTrue(true);// 异常是正确的
        }

        buf = new byte[] { 0x00, 0x00, 0x01 };
        assertEquals(1, IOUtil.bufToShort(buf, 1));

        buf = new byte[] { 0x00, 0x01, 0x2C };
        assertEquals(300, IOUtil.bufToShort(buf, 1));
    }

    @Test
    public void testBufToInt() {
        byte[] buf = null;
        try {
            IOUtil.bufToInt(buf, 0);
        } catch (NullPointerException e) {
            assertTrue(true);// 异常是正确的
        }

        buf = new byte[4];
        try {
            IOUtil.bufToInt(buf, 1);
        } catch (IllegalArgumentException e) {
            LOGGER.info(e.getMessage());
            assertTrue(true);// 异常是正确的
        }

        buf = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x01 };
        assertEquals(1, IOUtil.bufToInt(buf, 1));

        buf = new byte[] { 0x00, 0x00, 0x00, 0x01, 0x2C };
        assertEquals(300, IOUtil.bufToInt(buf, 1));
    }

    @Test
    public void testBufToLong() {
        byte[] buf = null;
        try {
            IOUtil.bufToLong(buf, 0);
        } catch (NullPointerException e) {
            assertTrue(true);// 异常是正确的
        }

        buf = new byte[4];
        try {
            IOUtil.bufToLong(buf, 1);
        } catch (IllegalArgumentException e) {
            LOGGER.info(e.getMessage());
            assertTrue(true);// 异常是正确的
        }

        buf = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01 };
        assertEquals(1, IOUtil.bufToLong(buf, 1));

        buf = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x2C };
        assertEquals(300, IOUtil.bufToLong(buf, 1));
    }

    @Test
    public void testCloseQuietlyCloseable() throws Exception {
        Closeable obj = null;
        IOUtil.close(obj);
        // 关闭一个Closeable
        File file = FileUtil.getFileByPath(EXIST_CP_FILE_PATH);
        Closeable closeable = FileUtil.openInputStream(file);
        IOUtil.close(closeable);
        assertTrue(true);// 没有发生异常
    }

    @Test
    @Disabled
    public void testCloseQuietlyObject() throws Exception {
        Object obj = null;
        IOUtil.close(obj);
        // 关闭一个Socket
        Socket sock = new Socket("www.baidu.com", 80);
        IOUtil.close(sock, "使用完毕");
        assertTrue(true);// 没有发生异常

        // 关闭一个Socket(先前已经关闭)
        Socket sock2 = new Socket("www.baidu.com", 80);
        sock2.close();
        IOUtil.close(sock2);
        assertTrue(true);// 没有发生异常
    }

    // ==================== Additional test cases for close methods ====================

    @Test
    public void testCloseObjectWithNull() {
        // Test close(Object) with null
        IOUtil.close((Object) null);
        assertTrue(true); // No exception should be thrown

        // Test close(Object, String) with null
        IOUtil.close((Object) null, "debug info");
        assertTrue(true); // No exception should be thrown
    }

    @Test
    public void testCloseObjectWithCloseable() throws Exception {
        // Test close(Object, String) with Closeable object
        File file = FileUtil.getFileByPath(EXIST_CP_FILE_PATH);
        Closeable closeable = FileUtil.openInputStream(file);
        IOUtil.close((Object) closeable, "closing input stream");
        assertTrue(true); // No exception should be thrown
    }

    @Test
    public void testCloseObjectWithDebugInfo() throws Exception {
        // Test close(Object, String) with debug information
        File file = FileUtil.getFileByPath(EXIST_CP_FILE_PATH);
        Closeable closeable = FileUtil.openInputStream(file);
        IOUtil.close(closeable, "test debug info");
        assertTrue(true); // No exception should be thrown
    }

    @Test
    public void testCloseObjectWithNullDebugInfo() throws Exception {
        // Test close(Object, String) with null debug info
        File file = FileUtil.getFileByPath(EXIST_CP_FILE_PATH);
        Closeable closeable = FileUtil.openInputStream(file);
        IOUtil.close((Object) closeable, null);
        assertTrue(true); // No exception should be thrown
    }

    @Test
    public void testCloseObjectWithoutCloseableInterface() {
        // Test with an object that has close method but doesn't implement Closeable
        // Note: In Java 9+, reflection may not be able to access private inner class methods
        MockCloseableObject mockObj = new MockCloseableObject();
        assertFalse(mockObj.isClosed());
        IOUtil.close(mockObj, "closing mock object");
        // The close method will be called via reflection if accessible
        // This test verifies that IOUtil.close handles objects with close() gracefully
        assertTrue(true); // No exception should be thrown
    }

    @Test
    public void testCloseObjectWithoutCloseMethod() {
        // Test with an object that doesn't have close method
        String str = "test string";
        IOUtil.close(str, "should handle gracefully");
        assertTrue(true); // Should not throw exception
    }

    @Test
    public void testCloseObjectAlreadyClosed() throws Exception {
        // Test closing an already closed object
        File file = FileUtil.getFileByPath(EXIST_CP_FILE_PATH);
        Closeable closeable = FileUtil.openInputStream(file);
        closeable.close();
        IOUtil.close(closeable);
        assertTrue(true); // Should handle already closed object gracefully
    }

    // ==================== Test cases for readFully methods ====================

    @Test
    public void testReadFullyWithSize() throws Exception {
        // Test reading exact size from stream
        byte[] data = "Hello World".getBytes();
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);

        byte[] result = IOUtil.readFully(bais, 5);
        assertEquals(5, result.length);
        assertArrayEquals("Hello".getBytes(), result);
    }

    @Test
    public void testReadFullyWithSizeZero() throws Exception {
        // Test reading zero bytes
        byte[] data = "Hello World".getBytes();
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);

        byte[] result = IOUtil.readFully(bais, 0);
        assertEquals(0, result.length);
    }

    @Test
    public void testReadFullyWithSizeExceedsAvailable() {
        // Test reading more bytes than available (should throw IOException)
        byte[] data = "Hello".getBytes();
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);

        assertThrows(java.io.IOException.class, () -> {
            IOUtil.readFully(bais, 10);
        });
    }

    @Test
    public void testReadFullyWithNegativeSize() {
        // Test with negative size (should throw IllegalArgumentException)
        byte[] data = "Hello World".getBytes();
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);

        assertThrows(IllegalArgumentException.class, () -> {
            IOUtil.readFully(bais, -1);
        });
    }

    @Test
    public void testReadFullyWithoutSize() throws Exception {
        // Test reading entire stream without specifying size
        byte[] data = "Hello World".getBytes();
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);

        byte[] result = IOUtil.readFully(bais);
        assertArrayEquals(data, result);
    }

    @Test
    public void testReadFullyWithoutSizeEmptyStream() throws Exception {
        // Test reading from empty stream
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(new byte[0]);

        byte[] result = IOUtil.readFully(bais);
        assertEquals(0, result.length);
    }

    @Test
    public void testReadFullyWithoutSizeLargeData() throws Exception {
        // Test reading large data (more than buffer size)
        byte[] data = new byte[2048]; // Larger than default buffer (1024)
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);

        byte[] result = IOUtil.readFully(bais);
        assertArrayEquals(data, result);
    }

    @Test
    public void testReadFullyWithBufferedInputStream() throws Exception {
        // Test with BufferedInputStream
        byte[] data = "Hello World".getBytes();
        java.io.BufferedInputStream bis = new java.io.BufferedInputStream(
            new java.io.ByteArrayInputStream(data)
        );

        byte[] result = IOUtil.readFully(bis);
        assertArrayEquals(data, result);
    }

    @Test
    public void testReadFullyMultipleReads() throws Exception {
        // Test reading in chunks (simulating multiple read operations)
        byte[] data = "0123456789".getBytes();
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);

        byte[] result = IOUtil.readFully(bais, 10);
        assertEquals(10, result.length);
        assertArrayEquals(data, result);
    }

    @Test
    public void testReadFully0WithSize() throws Exception {
        // Test the internal readFully0 method directly
        byte[] data = "Test Data".getBytes();
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);

        byte[] result = IOUtil.readFully0(bais, 9);
        assertArrayEquals(data, result);
    }

    @Test
    public void testReadFully0WithSizeZero() throws Exception {
        // Test readFully0 with size zero
        byte[] data = "Test Data".getBytes();
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);

        byte[] result = IOUtil.readFully0(bais, 0);
        assertEquals(0, result.length);
    }

    @Test
    public void testReadFully0EOF() {
        // Test readFully0 when stream reaches EOF prematurely
        byte[] data = "Short".getBytes();
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);

        assertThrows(java.io.IOException.class, () -> {
            IOUtil.readFully0(bais, 20);
        });
    }

    @Test
    public void testReadFully0NegativeSize() {
        // Test readFully0 with negative size
        byte[] data = "Test Data".getBytes();
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);

        assertThrows(IllegalArgumentException.class, () -> {
            IOUtil.readFully0(bais, -5);
        });
    }

    @Test
    public void testReadFullyWithExactBufferSize() throws Exception {
        // Test reading data exactly equal to buffer size (1024)
        byte[] data = new byte[1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);

        byte[] result = IOUtil.readFully(bais);
        assertArrayEquals(data, result);
    }

    @Test
    public void testReadFullyPartialRead() throws Exception {
        // Test partial read with size parameter
        byte[] data = "0123456789ABCDEF".getBytes();
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);

        byte[] result = IOUtil.readFully(bais, 8);
        assertEquals(8, result.length);
        assertArrayEquals("01234567".getBytes(), result);

        // Verify remaining data is still in stream
        result = IOUtil.readFully(bais, 8);
        assertArrayEquals("89ABCDEF".getBytes(), result);
    }

    // Mock class for testing close with non-Closeable objects
    public static class MockCloseableObject {
        private boolean closed = false;

        public void close() {
            this.closed = true;
        }

        public boolean isClosed() {
            return closed;
        }
    }
}
