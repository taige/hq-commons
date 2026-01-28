package io.hqwu.commons.servlet.support;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link CachedBodyHttpServletRequest} 的单元测试类。
 * <p>
 * 测试覆盖：
 * <ul>
 *   <li>构造函数：缓存请求体和参数</li>
 *   <li>可重复读取：getInputStream、getReader 多次调用</li>
 *   <li>参数访问：getParameter、getParameterMap、getParameterNames、getParameterValues</li>
 *   <li>直接访问缓存：getContentAsByteArray</li>
 *   <li>边界条件：空请求体、大数据量、特殊字符</li>
 *   <li>异常处理：构造函数中的 IOException</li>
 * </ul>
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CachedBodyHttpServletRequest 单元测试")
class CachedBodyHttpServletRequestTest {

    @Mock
    private HttpServletRequest mockRequest;

    // ==================== 构造函数测试 ====================

    @Test
    @DisplayName("测试构造函数 - 正常创建")
    void testConstructor() throws IOException {
        String requestBody = "{\"name\":\"test\",\"value\":123}";
        byte[] bodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(bodyBytes));
        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);

        assertNotNull(cachedRequest);
        assertArrayEquals(bodyBytes, cachedRequest.getContentAsByteArray());
    }

    @Test
    @DisplayName("测试构造函数 - 空请求体")
    void testConstructorWithEmptyBody() throws IOException {
        byte[] emptyBody = new byte[0];

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(emptyBody));
        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);

        assertNotNull(cachedRequest);
        assertArrayEquals(emptyBody, cachedRequest.getContentAsByteArray());
        assertEquals(0, cachedRequest.getContentAsByteArray().length);
    }

    @Test
    @DisplayName("测试构造函数 - 缓存参数映射")
    void testConstructorCachesParameterMap() throws IOException {
        byte[] bodyBytes = "test".getBytes(StandardCharsets.UTF_8);
        Map<String, String[]> params = new HashMap<>();
        params.put("param1", new String[]{"value1"});
        params.put("param2", new String[]{"value2a", "value2b"});

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(bodyBytes));
        when(mockRequest.getParameterMap()).thenReturn(params);

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);

        assertEquals(params, cachedRequest.getParameterMap());
        verify(mockRequest, times(1)).getParameterMap();
    }

    @Test
    @DisplayName("测试构造函数 - IOException异常")
    void testConstructorWithIOException() throws IOException {
        when(mockRequest.getInputStream()).thenThrow(new IOException("Test IOException"));

        assertThrows(IOException.class, () -> {
            new CachedBodyHttpServletRequest(mockRequest);
        });
    }

    // ==================== 可重复读取测试 ====================

    @Test
    @DisplayName("测试getInputStream - 可重复读取")
    void testGetInputStreamRepeatable() throws IOException {
        String requestBody = "Hello World";
        byte[] bodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(bodyBytes));
        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);

        // 第一次读取
        ServletInputStream inputStream1 = cachedRequest.getInputStream();
        byte[] buffer1 = new byte[bodyBytes.length];
        int read1 = inputStream1.read(buffer1);

        // 第二次读取（验证可重复读取）
        ServletInputStream inputStream2 = cachedRequest.getInputStream();
        byte[] buffer2 = new byte[bodyBytes.length];
        int read2 = inputStream2.read(buffer2);

        assertEquals(bodyBytes.length, read1);
        assertEquals(bodyBytes.length, read2);
        assertArrayEquals(buffer1, buffer2);
        assertEquals(requestBody, new String(buffer1, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("测试getInputStream - 逐字节读取")
    void testGetInputStreamByteByByte() throws IOException {
        String requestBody = "Test";
        byte[] bodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(bodyBytes));
        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);
        ServletInputStream inputStream = cachedRequest.getInputStream();

        assertEquals('T', inputStream.read());
        assertEquals('e', inputStream.read());
        assertEquals('s', inputStream.read());
        assertEquals('t', inputStream.read());
        assertEquals(-1, inputStream.read()); // 流结束
    }

    @Test
    @DisplayName("测试getReader - 可重复读取")
    void testGetReaderRepeatable() throws IOException {
        String requestBody = "Hello World";
        byte[] bodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(bodyBytes));
        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);

        // 第一次读取
        BufferedReader reader1 = cachedRequest.getReader();
        String line1 = reader1.readLine();

        // 第二次读取（验证可重复读取）
        BufferedReader reader2 = cachedRequest.getReader();
        String line2 = reader2.readLine();

        assertEquals(requestBody, line1);
        assertEquals(requestBody, line2);
    }

    @Test
    @DisplayName("测试getReader - 多行内容")
    void testGetReaderMultiLine() throws IOException {
        String requestBody = "Line1\nLine2\nLine3";
        byte[] bodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(bodyBytes));
        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);
        BufferedReader reader = cachedRequest.getReader();

        assertEquals("Line1", reader.readLine());
        assertEquals("Line2", reader.readLine());
        assertEquals("Line3", reader.readLine());
        assertNull(reader.readLine()); // 流结束
    }

    // ==================== 参数访问测试 ====================

    @Test
    @DisplayName("测试getParameter - 单个参数值")
    void testGetParameterSingleValue() throws IOException {
        byte[] bodyBytes = "test".getBytes(StandardCharsets.UTF_8);
        Map<String, String[]> params = new HashMap<>();
        params.put("name", new String[]{"John"});

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(bodyBytes));
        when(mockRequest.getParameterMap()).thenReturn(params);

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);

        assertEquals("John", cachedRequest.getParameter("name"));
    }

    @Test
    @DisplayName("测试getParameter - 多个参数值返回第一个")
    void testGetParameterMultipleValues() throws IOException {
        byte[] bodyBytes = "test".getBytes(StandardCharsets.UTF_8);
        Map<String, String[]> params = new HashMap<>();
        params.put("tags", new String[]{"tag1", "tag2", "tag3"});

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(bodyBytes));
        when(mockRequest.getParameterMap()).thenReturn(params);

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);

        assertEquals("tag1", cachedRequest.getParameter("tags"));
    }

    @Test
    @DisplayName("测试getParameter - 空数组返回空字符串")
    void testGetParameterEmptyArray() throws IOException {
        byte[] bodyBytes = "test".getBytes(StandardCharsets.UTF_8);
        Map<String, String[]> params = new HashMap<>();
        params.put("empty", new String[0]);

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(bodyBytes));
        when(mockRequest.getParameterMap()).thenReturn(params);

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);

        assertEquals("", cachedRequest.getParameter("empty"));
    }

    @Test
    @DisplayName("测试getParameter - 参数不存在返回null")
    void testGetParameterNotExists() throws IOException {
        byte[] bodyBytes = "test".getBytes(StandardCharsets.UTF_8);
        Map<String, String[]> params = new HashMap<>();

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(bodyBytes));
        when(mockRequest.getParameterMap()).thenReturn(params);

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);

        assertNull(cachedRequest.getParameter("nonexistent"));
    }

    @Test
    @DisplayName("测试getParameterMap - 返回缓存的参数映射")
    void testGetParameterMap() throws IOException {
        byte[] bodyBytes = "test".getBytes(StandardCharsets.UTF_8);
        Map<String, String[]> params = new HashMap<>();
        params.put("param1", new String[]{"value1"});
        params.put("param2", new String[]{"value2"});

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(bodyBytes));
        when(mockRequest.getParameterMap()).thenReturn(params);

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);
        Map<String, String[]> resultMap = cachedRequest.getParameterMap();

        assertEquals(2, resultMap.size());
        assertArrayEquals(new String[]{"value1"}, resultMap.get("param1"));
        assertArrayEquals(new String[]{"value2"}, resultMap.get("param2"));
    }

    @Test
    @DisplayName("测试getParameterNames - 返回所有参数名")
    void testGetParameterNames() throws IOException {
        byte[] bodyBytes = "test".getBytes(StandardCharsets.UTF_8);
        Map<String, String[]> params = new HashMap<>();
        params.put("param1", new String[]{"value1"});
        params.put("param2", new String[]{"value2"});
        params.put("param3", new String[]{"value3"});

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(bodyBytes));
        when(mockRequest.getParameterMap()).thenReturn(params);

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);
        Enumeration<String> names = cachedRequest.getParameterNames();

        Set<String> nameSet = new HashSet<>();
        while (names.hasMoreElements()) {
            nameSet.add(names.nextElement());
        }

        assertEquals(3, nameSet.size());
        assertTrue(nameSet.contains("param1"));
        assertTrue(nameSet.contains("param2"));
        assertTrue(nameSet.contains("param3"));
    }

    @Test
    @DisplayName("测试getParameterValues - 返回参数的所有值")
    void testGetParameterValues() throws IOException {
        byte[] bodyBytes = "test".getBytes(StandardCharsets.UTF_8);
        Map<String, String[]> params = new HashMap<>();
        params.put("tags", new String[]{"tag1", "tag2", "tag3"});

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(bodyBytes));
        when(mockRequest.getParameterMap()).thenReturn(params);

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);
        String[] values = cachedRequest.getParameterValues("tags");

        assertArrayEquals(new String[]{"tag1", "tag2", "tag3"}, values);
    }

    @Test
    @DisplayName("测试getParameterValues - 参数不存在返回null")
    void testGetParameterValuesNotExists() throws IOException {
        byte[] bodyBytes = "test".getBytes(StandardCharsets.UTF_8);
        Map<String, String[]> params = new HashMap<>();

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(bodyBytes));
        when(mockRequest.getParameterMap()).thenReturn(params);

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);

        assertNull(cachedRequest.getParameterValues("nonexistent"));
    }

    // ==================== 直接访问缓存测试 ====================

    @Test
    @DisplayName("测试getContentAsByteArray - 返回缓存的字节数组")
    void testGetContentAsByteArray() throws IOException {
        String requestBody = "{\"key\":\"value\"}";
        byte[] bodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(bodyBytes));
        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);
        byte[] cachedContent = cachedRequest.getContentAsByteArray();

        assertArrayEquals(bodyBytes, cachedContent);
        assertEquals(requestBody, new String(cachedContent, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("测试getContentAsByteArray - 多次调用返回相同内容")
    void testGetContentAsByteArrayConsistency() throws IOException {
        String requestBody = "Test Content";
        byte[] bodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(bodyBytes));
        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);

        byte[] content1 = cachedRequest.getContentAsByteArray();
        byte[] content2 = cachedRequest.getContentAsByteArray();

        assertArrayEquals(content1, content2);
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("测试边界条件 - 大数据量请求体")
    void testLargeRequestBody() throws IOException {
        // 创建 10KB 的数据
        byte[] largeBody = new byte[10 * 1024];
        Arrays.fill(largeBody, (byte) 'A');

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(largeBody));
        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);

        assertArrayEquals(largeBody, cachedRequest.getContentAsByteArray());
        assertEquals(10 * 1024, cachedRequest.getContentAsByteArray().length);
    }

    @Test
    @DisplayName("测试边界条件 - UTF-8多字节字符")
    void testMultiByteCharacters() throws IOException {
        String requestBody = "你好世界 🌍 Hello World";
        byte[] bodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(bodyBytes));
        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);
        BufferedReader reader = cachedRequest.getReader();
        String readContent = reader.readLine();

        assertEquals(requestBody, readContent);
    }

    @Test
    @DisplayName("测试边界条件 - JSON格式请求体")
    void testJsonRequestBody() throws IOException {
        String jsonBody = "{\"name\":\"John\",\"age\":30,\"city\":\"New York\"}";
        byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(bodyBytes));
        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);

        BufferedReader reader = cachedRequest.getReader();
        String readJson = reader.readLine();

        assertEquals(jsonBody, readJson);
    }

    @Test
    @DisplayName("测试边界条件 - 特殊字符")
    void testSpecialCharacters() throws IOException {
        String requestBody = "Special: \n\r\t\0 !@#$%^&*()";
        byte[] bodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);

        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(bodyBytes));
        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);

        assertArrayEquals(bodyBytes, cachedRequest.getContentAsByteArray());
    }

    // ==================== 集成测试 ====================

    @Test
    @DisplayName("集成测试 - 使用Spring MockHttpServletRequest")
    void testWithSpringMockRequest() throws IOException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        String requestBody = "{\"test\":\"data\"}";
        mockRequest.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
        mockRequest.setParameter("param1", "value1");
        mockRequest.setParameter("param2", "value2a", "value2b");

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(mockRequest);

        // 验证请求体缓存
        assertEquals(requestBody, new String(cachedRequest.getContentAsByteArray(), StandardCharsets.UTF_8));

        // 验证可重复读取
        String body1 = cachedRequest.getReader().readLine();
        String body2 = cachedRequest.getReader().readLine();
        assertEquals(body1, body2);

        // 验证参数缓存
        assertEquals("value1", cachedRequest.getParameter("param1"));
        assertArrayEquals(new String[]{"value2a", "value2b"}, cachedRequest.getParameterValues("param2"));
    }

    // ==================== 辅助类 ====================

    /**
     * Mock ServletInputStream 实现，用于测试
     */
    private static class MockServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream inputStream;

        public MockServletInputStream(byte[] data) {
            this.inputStream = new ByteArrayInputStream(data);
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(jakarta.servlet.ReadListener listener) {
            throw new UnsupportedOperationException();
        }
    }
}
