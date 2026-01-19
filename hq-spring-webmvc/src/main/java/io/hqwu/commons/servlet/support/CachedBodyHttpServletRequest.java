package io.hqwu.commons.servlet.support;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

/**
 * 可重复读取请求体的 HttpServletRequest 包装类。
 * <p>
 * 在标准的 {@link HttpServletRequest} 中，请求体的输入流只能被读取一次。
 * 该类通过在构造时缓存请求体内容和参数映射，允许多次读取请求体数据，
 * 常用于需要多次访问请求体的场景，如日志记录、签名验证等。
 * </p>
 * <p>
 * 主要功能：
 * <ul>
 *   <li>缓存原始请求体内容为字节数组</li>
 *   <li>缓存请求参数映射</li>
 *   <li>提供可重复读取的 {@link ServletInputStream} 和 {@link BufferedReader}</li>
 *   <li>支持通过 {@link #getContentAsByteArray()} 直接获取缓存的请求体</li>
 * </ul>
 * </p>
 * <p>
 * 使用示例：
 * <pre>
 * HttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
 * // 第一次读取
 * String body1 = StreamUtils.copyToString(cachedRequest.getInputStream(), StandardCharsets.UTF_8);
 * // 第二次读取（标准 HttpServletRequest 无法做到）
 * String body2 = StreamUtils.copyToString(cachedRequest.getInputStream(), StandardCharsets.UTF_8);
 * </pre>
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see HttpServletRequestWrapper
 * @see CachedBodyServletInputStream
 * @see ServletInputStream
 * @see HttpServletRequest
 * Date: 2020/4/5
 * Time: 23:54
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private Map<String, String[]> parameterMap;
    private byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.parameterMap = request.getParameterMap();
        InputStream requestInputStream = request.getInputStream();
        this.cachedBody = StreamUtils.copyToByteArray(requestInputStream);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new CachedBodyServletInputStream(this.cachedBody);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
        return new BufferedReader(new InputStreamReader(byteArrayInputStream));
    }

    @Override
    public String getParameter(String name) {
        String[] values = parameterMap.get(name);
        if (values != null) {
            if(values.length == 0) {
                return "";
            }
            return values[0];
        } else {
            return null;
        }
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return this.parameterMap;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return this.parameterMap.get(name);
    }

    public byte[] getContentAsByteArray() {
        return this.cachedBody;
    }
}
