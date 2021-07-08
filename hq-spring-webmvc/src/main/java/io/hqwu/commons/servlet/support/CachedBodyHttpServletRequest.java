package io.hqwu.commons.servlet.support;

import com.umpay.commons.util.Logger;
import org.springframework.util.StreamUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

/**
 * Created with IntelliJ IDEA for hq-commons
 * User: taige
 * Date: 2020/4/5
 * Time: 23:54
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    private static final Logger LOGGER = new Logger();

    private final Map<String, String[]> parameterMap;
    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.parameterMap = request.getParameterMap();
        InputStream requestInputStream = request.getInputStream();
        try {
            this.cachedBody = StreamUtils.copyToByteArray(requestInputStream);
        } catch (IOException e) {
            LOGGER.info("read request body failed. content-type: %s, content-length: %d", request.getContentType(), request.getContentLengthLong());
            throw e;
        }
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
