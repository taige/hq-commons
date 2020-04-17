/*
 * @(#)IOUtilTest.java Created on 2013-8-12
 *
 * Copyright 2012-2013 Chinabank Payments, Inc. All rights reserved.
 * Use is subject to license terms.
 */
package com.umpay.commons.util;

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
    static String EXIST_CP_FILE_PATH = "com/umpay/commons/util/test.file";

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
     * Test method for {@link com.umpay.commons.util.IOUtil#bcd(byte[])}.
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
     * Test method for {@link com.umpay.commons.util.IOUtil#bcd(String)}.
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
        try {
            IOUtil.shortToBuf(toEncode, buf, 0);
        } catch (NullPointerException e) {
            assertTrue(true);// 异常是正确的
        }

        buf = new byte[2];
        try {
            IOUtil.shortToBuf(toEncode, buf, 1);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            assertTrue(true);// 异常是正确的
        }

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
        try {
            IOUtil.intToBuf(toEncode, buf, 0);
        } catch (NullPointerException e) {
            assertTrue(true);// 异常是正确的
        }

        buf = new byte[4];
        try {
            IOUtil.intToBuf(toEncode, buf, 1);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            assertTrue(true);// 异常是正确的
        }

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
        try {
            IOUtil.longToBuf(toEncode, buf, 0);
        } catch (NullPointerException e) {
            assertTrue(true);// 异常是正确的
        }

        buf = new byte[8];
        try {
            IOUtil.longToBuf(toEncode, buf, 1);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            assertTrue(true);// 异常是正确的
        }

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
            System.out.println(e.getMessage());
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
            System.out.println(e.getMessage());
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
            System.out.println(e.getMessage());
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
}
