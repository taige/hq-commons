package io.hqwu.commons.servlet.support;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 基于缓存字节数组的 ServletInputStream 实现类。
 * <p>
 * 该类用于支持请求体的可重复读取功能。通过将请求体内容预先缓存到字节数组中，
 * 并基于 {@link ByteArrayInputStream} 提供输入流操作，使得同一请求体可以被多次读取。
 * </p>
 * <p>
 * 主要特性：
 * <ul>
 *   <li>基于内存中的字节数组缓存，实现可重复读取</li>
 *   <li>实现 {@link ServletInputStream} 的所有必要方法</li>
 *   <li>不支持异步读取（{@link #setReadListener(ReadListener)} 会抛出异常）</li>
 *   <li>始终处于就绪状态（{@link #isReady()} 返回 true）</li>
 * </ul>
 * </p>
 * <p>
 * 通常与 {@link CachedBodyHttpServletRequest} 配合使用，
 * 为该包装类提供可重复读取的输入流实现。
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see ServletInputStream
 * @see CachedBodyHttpServletRequest
 * @see ByteArrayInputStream
 * Date: 2020/4/5
 * Time: 23:55
 */
public class CachedBodyServletInputStream extends ServletInputStream {

    private InputStream cachedBodyInputStream;

    public CachedBodyServletInputStream(byte[] cachedBody) {
        this.cachedBodyInputStream = new ByteArrayInputStream(cachedBody);
    }

    @Override
    public int read() throws IOException {
        return cachedBodyInputStream.read();
    }

    @Override
    public boolean isFinished() {
        try {
            return cachedBodyInputStream.available() == 0;
        } catch (IOException e) {
        }
        return true;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener listener) {
        throw new UnsupportedOperationException();
    }
}