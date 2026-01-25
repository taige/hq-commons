package io.hqwu.commons.util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * 网络 IO 工具类，扩展自 {@link org.apache.commons.io.IOUtils}。
 *
 * <p>该类提供了针对网络通信场景的增强功能：
 * <ul>
 *   <li><b>编码转换</b>：支持 BCD 编码与 ASCII 字符串的互转（基于 {@link org.apache.commons.codec.binary.Hex}）。</li>
 *   <li><b>字节操作</b>：提供高效的字节数组连接与合并工具。</li>
 *   <li><b>网络序处理</b>：实现数值类型（short, int, long）与网络字节序（Big-Endian）的编解码。</li>
 *   <li><b>资源管理</b>：提供对 {@link Closeable} 资源的静默关闭，支持反射调用非标准 close 方法。</li>
 *   <li><b>流读取</b>：支持从 {@link InputStream} 中完整读取指定长度或全部字节数据。</li>
 * </ul>
 *
 * @author shenjianlin
 * @version 1.0
 * @see org.apache.commons.io.IOUtils
 * @see org.apache.commons.codec.binary.Hex
 */
public final class IOUtil extends IOUtils {
    private static final Logger _log = LoggerFactory.getLogger(IOUtil.class);

    /**
     * 将BCD编码的字节流解码成ASCII字符串
     *
     * @param data BCD编码的字节流
     * @return ASCII字符串
     */
    public static final String bcd(final byte[] data) {
        return bcd(data, -1);// 全部转码
    }

    /**
     * 将BCD编码的字节流解码成ASCII字符串,为日志输出使用
     *
     * @param data BCD编码的字节流
     * @param len  编码长度，其他部分用...表示
     * @return ASCII字符串
     */
    public static final String bcd(final byte[] data, final int len) {
        if (ArrayUtils.isEmpty(data)) {
            return "";// return null?
        }
        char[] chars = Hex.encodeHex(data, false);
        if (len == -1 || 2 * len >= chars.length) {// -1 强制全部解码，或者编码出来的长度小于定义值
            return new String(chars);
        } else {
            return new String(Hex.encodeHex(data, false), 0, 2 * len) + "...";
        }
    }

    /**
     * 将ASCII字符串编码成BCD编码字节流
     *
     * @param ascii ASCII字符串
     * @return BCD编码后的字节流
     * @throws IllegalArgumentException 编码异常时抛出，如长度不符合规则
     */
    public static final byte[] bcd(final String ascii)
            throws IllegalArgumentException {
        if (StringUtil.isEmpty(ascii)) {
            return new byte[0];// 返回空字符数组，简化客户端调用
        }
        try {
            // 吃掉Checked异常，一般用户不能恢复此种异常
            return Hex.decodeHex(ascii.toCharArray());
        } catch (DecoderException e) {
            throw new IllegalArgumentException("转码异常", e);
        }
    }

    /**
     * 字节数组连接
     *
     * @param first  字节数组
     * @param second 字节数组
     * @return 连接后的字节数组
     * @TODO public static <T> T[] join(T[] one, T[] two)
     * @see #joinBytes(byte[], byte[], int, int)
     */
    public static final byte[] joinBytes(byte[] first, byte[] second) {
        return joinBytes(first, second, 0, second != null ? second.length : 0);
    }

    /**
     * /** 字节数组连接
     *
     * @param first  字节数组
     * @param second 字节数组
     * @param offset 第二个数组拷贝开始位置
     * @param len    第二个数组拷贝的长度
     * @return 连接后的字节数组
     */
    public static final byte[] joinBytes(byte[] first, byte[] second,
                                         int offset, int len) {
        if (ArrayUtils.isEmpty(first)) {// 前为空，直接返回后者
            return second == null ? new byte[0] : second;
        } else if (ArrayUtils.isEmpty(second)) {// 后为空直接返回前者
            return first;
        }
        // 两者均不为空，拷贝到一起
        byte[] all = new byte[first.length + len];
        System.arraycopy(first, 0, all, 0, first.length);
        System.arraycopy(second, offset, all, first.length, len);

        if (_log.isDebugEnabled()) {
            _log.debug("# joinBytes({},{})=[{}]", bcd(first, 64),
                    bcd(second, 64), bcd(all, 128));
        }
        return all;
    }

    // -------------------------网络序编/解码----------------------------

    /**
     * 将short类型的变量编码成网络序的字节流（2个字节）
     * <p/>
     * <pre>
     * byte[] buf = new byte[3];
     * IOUtil.shortToBuf((short) 1, buf, 1);
     * assertArrayEquals(new byte[] { 0x00, 0x00, 0x01 }, buf);
     *
     * byte[] buf = new byte[3];
     * IOUtil.shortToBuf((short) 300, buf, 1);
     * assertArrayEquals(new byte[] { 0x00, 0x01, 0x2C }, buf);
     * </pre>
     *
     * @param toEncode 待编码的Short类型整数
     * @param buf      缓冲区
     * @param offset   字节数组的偏移量
     * @throws IllegalArgumentException 缓冲区过小时抛出，即：buf.length<offset+2
     */
    public static final void shortToBuf(short toEncode, byte[] buf, int offset)
            throws IllegalArgumentException {
        Validate.isTrue(buf.length >= offset + 2, "缓冲区过小,总长度%d<%d+2",
                buf.length, offset);
        for (int i = 0; i < 2; i++) {// 右移，取某一字节
            buf[offset + i] = (byte) (toEncode >>> (1 - i) * 8 & 0xff);
        }
    }

    /**
     * 将int类型的变量编码成网络序的字节流（4个字节）
     * <p/>
     * <pre>
     * byte[] buf = new byte[5];
     * IOUtil.intToBuf(1, buf, 1);
     * assertArrayEquals(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x01 }, buf);
     *
     * byte[] buf = new byte[5];
     * IOUtil.intToBuf(300, buf, 1);
     * assertArrayEquals(new byte[] { 0x00, 0x00, 0x00, 0x01, 0x2C }, buf);
     * </pre>
     *
     * @param toEncode 待编码的int类型整数
     * @param buf      缓冲区
     * @param offset   字节数组的偏移量
     * @throws IllegalArgumentException 缓冲区过小时抛出，即：buf.length<offset+4
     */
    public static final void intToBuf(int toEncode, byte[] buf, int offset)
            throws IllegalArgumentException {
        Validate.isTrue(buf.length >= offset + 4, "缓冲区过小,总长度%d<%d+4",
                buf.length, offset);
        for (int i = 0; i < 4; i++) {// 右移，取某一字节
            buf[offset + i] = (byte) (toEncode >>> (3 - i) * 8 & 0xff);
        }
    }

    /**
     * 将long类型的变量编码成网络序的字节流（8个字节）
     * <p/>
     * <pre>
     * byte[] buf = new byte[9];
     * IOUtil.longToBuf(1, buf, 1);
     * assertArrayEquals(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
     *         0x01 }, buf);
     *
     * byte[] buf = new byte[9];
     * IOUtil.longToBuf(300, buf, 1);
     * assertArrayEquals(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,
     *         0x2C }, buf);
     * </pre>
     *
     * @param toEncode 待编码的long类型整数
     * @param buf      缓冲区
     * @param offset   字节数组的偏移量
     * @throws IllegalArgumentException 缓冲区过小时抛出，即：buf.length<offset+8
     */
    public static final void longToBuf(long toEncode, byte[] buf, int offset)
            throws IllegalArgumentException {
        Validate.isTrue(buf.length >= offset + 8, "缓冲区过小,总长度%d<%d+8",
                buf.length, offset);
        for (int i = 0; i < 8; i++) {// 右移，取某一字节
            buf[offset + i] = (byte) (toEncode >>> (7 - i) * 8 & 0xff);
        }
    }

    /**
     * 将字节流中的2个字节解码成一个short类型的整数
     * <p/>
     * <pre>
     * byte[] buf = new byte[] { 0x00, 0x00, 0x01 };
     * assertEquals(1, IOUtil.bufToShort(buf, 1));
     *
     * byte[] buf = new byte[] { 0x00, 0x01, 0x2C };
     * assertEquals(300, IOUtil.bufToShort(buf, 1));
     * </pre>
     *
     * @param buf    缓冲区
     * @param offset 字节数组的偏移量
     * @return 2个网络序的字节所表示的整数
     * @throws IllegalArgumentException 缓冲区过小时抛出，即：buf.length<offset+2
     */
    public static final short bufToShort(byte[] buf, int offset)
            throws IllegalArgumentException {
        Validate.isTrue(buf.length >= offset + 2, "缓冲区过小,总长度%d<%d+2",
                buf.length, offset);
        short toDecode = 0; // 网络序
        for (int i = 0; i < 2; i++) {
            toDecode |= (int) (0xff & buf[offset + i]) << (1 - i) * 8;
        }
        return (short) (toDecode & 0xffff);
    }

    /**
     * 将字节流中的4个字节解码成一个int类型的整数
     * <p/>
     * <pre>
     * byte[] buf = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x01 };
     * assertEquals(1, IOUtil.bufToInt(buf, 1));
     *
     * byte[] buf = new byte[] { 0x00, 0x00, 0x00, 0x01, 0x2C };
     * assertEquals(300, IOUtil.bufToInt(buf, 1));
     * </pre>
     *
     * @param buf    缓冲区
     * @param offset 字节数组的偏移量
     * @return 4个网络序的字节所表示的整数
     * @throws IllegalArgumentException 缓冲区过小时抛出，即：buf.length<offset+4
     */
    public static final int bufToInt(byte[] buf, int offset)
            throws IllegalArgumentException {
        Validate.isTrue(buf.length >= offset + 4, "缓冲区过小,总长度%d<%d+4",
                buf.length, offset);
        int toDecode = 0; // 网络序
        for (int i = 0; i < 4; i++) {
            toDecode |= (0xff & buf[offset + i]) << (3 - i) * 8;
        }
        return toDecode;
    }

    /**
     * 将字节流中的8个字节解码成一个long类型的整数
     * <p/>
     * <pre>
     * byte[] buf = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01 };
     * assertEquals(1, IOUtil.bufToLong(buf, 1));
     *
     * byte[] buf = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x2C };
     * assertEquals(300, IOUtil.bufToLong(buf, 1));
     *
     * </pre>
     *
     * @param buf    缓冲区
     * @param offset 字节数组的偏移量
     * @return 8个网络序的字节所表示的整数
     * @throws IllegalArgumentException 缓冲区过小时抛出，即：buf.length<offset+8
     */
    public static long bufToLong(byte[] buf, int offset)
            throws IllegalArgumentException {
        Validate.isTrue(buf.length >= offset + 8, "缓冲区过小,总长度%d<%d+8",
                buf.length, offset);
        long toDecode = 0; // 网络序
        for (int i = 0; i < 8; i++) {
            toDecode |= (long) (0xff & buf[offset + i]) << (7 - i) * 8;
        }
        return toDecode;
    }

    /**
     * 强制关闭一个 <code>Closeable</code>对象，忽略任何异常，在commons-lang的基础上登记了日志<br>
     * <p/>
     * <pre>
     * Example code:
     *
     * Closeable closeable = null;
     * try {
     *     closeable = new FileReader(&quot;foo.txt&quot;);
     *     // process closeable
     *     closeable.close();
     * } catch (Exception e) {
     *     // error handling
     * } finally {
     *     IOUtil.closeQuietly(closeable, "finally close!");
     * }
     * </pre>
     *
     * @param closeable 实现Closeable接口，需要关闭的对象
     * @param debugInfo 调试信息
     */
    public static void close(Closeable closeable, String debugInfo) {
        try {
            if (closeable != null) {
                closeable.close();
            }
            _log.debug("# close({})... {}", closeable,
                    debugInfo != null ? debugInfo : "");
        } catch (IOException ioe) {
            _log.warn("# close({}) fail {} ", closeable, ioe);
            // ignore
        }
    }

    /**
     * @param closeable 实现Closeable接口，需要关闭的对象
     * @see #close(java.io.Closeable, String)
     */
    public static void close(Closeable closeable) {
        close(closeable, (String) null);
    }

    /**
     * 调用对象的close方法：如果对象实现了<code>Closeable</code>
     * 接口，这直接调用，否则采用反射调用对象的close方法，可能抛出调用异常
     *
     * @param closeable 实现close方法的对象
     * @param debugInfo 调试信息
     * @see #close(java.io.Closeable)
     */
    public static void close(Object closeable, String debugInfo) {
        if (closeable == null) {
            return;// 无需关闭
        }
        if (closeable instanceof Closeable) {
            close((Closeable) closeable, debugInfo);
        } else {
            try {
                MethodUtils.invokeMethod(closeable, "close");
                _log.debug("# close({})... {}", closeable,
                        debugInfo != null ? debugInfo : "");
            } catch (Exception e) {
                _log.warn("# close({}) fail {} ", closeable, e);
            }
        }
    }

    /**
     * @param closeable 实现close方法的对象
     * @see #close(java.io.Closeable, String)
     */
    public static void close(Object closeable) {
        close(closeable, null);
    }

    public static byte[] readFully(InputStream is, int size) throws IOException {
        return readFully0(is, size);
    }

    static byte[] readFully0(InputStream is, int size) throws IOException {
        Validate.isTrue(size >= 0, "readFully(size<0)");
        byte[] out = new byte[size];
        if (size == 0) {
            return out;    //@return byte[0]
        }
        int n, offset = 0;
        do {
            n = is.read(out, offset, size - offset);
            if (n == -1) {
                throw new IOException("readFully()...EOF");
            }
            offset += n;
        } while (offset < size);
        return out;
    }

    public static byte[] readFully(InputStream is) throws IOException {
        //处理cLen==-1的问题（无Content-Length项），读到无数据为止！
        final int bufSize = 1024;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (!(is instanceof BufferedInputStream)) {
            is = new BufferedInputStream(is, bufSize);//为了提高速度
        }
        byte[] buf = new byte[bufSize];
        do {
            int rc = is.read(buf, 0, buf.length);//读超？
            if (rc == -1) {
                break; // 直到无数据为止！！！
            }
            baos.write(buf, 0, rc); // 写入；
        } while (true);
        return baos.toByteArray();
    }
}
