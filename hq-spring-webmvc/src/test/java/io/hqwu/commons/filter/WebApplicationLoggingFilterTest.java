package io.hqwu.commons.filter;

import io.hqwu.commons.servlet.support.CachedBodyHttpServletRequest;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for {@link WebApplicationLoggingFilter}
 */
class WebApplicationLoggingFilterTest {

    private WebApplicationLoggingFilter filter;
    // 用于捕获日志输出的变量
    private String capturedBeforeMessage;
    private String capturedAfterMessage;

    @BeforeEach
    void setUp() {
        capturedBeforeMessage = null;
        capturedAfterMessage = null;

        // 使用匿名子类重写 shouldLog 和日志输出方法，以便验证日志内容
        filter = new WebApplicationLoggingFilter() {
            @Override
            protected boolean shouldLog(jakarta.servlet.http.HttpServletRequest request) {
                return true;
            }

            @Override
            protected void beforeRequest(HttpServletRequest request, String message) {
                capturedBeforeMessage = message;
                // super.beforeRequest(request, message); // 可选：如果不想在控制台看到日志，可以注释掉
            }

            @Override
            protected void afterRequest(HttpServletRequest request, String message) {
                capturedAfterMessage = message;
            }
        };
        // 开启所有日志选项以覆盖更多代码路径
        filter.setIncludePayload(true);
        filter.setIncludeHeaders(true);
        filter.setIncludeClientInfo(true);
        filter.setIncludeQueryString(true);
    }

    @Test
    void testDoFilter_JsonRequest_ShouldWrapRequestAndResponse() throws ServletException, IOException {
        // 准备 JSON 请求
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        request.setContentType("application/json");
        // MockHttpServletRequest 的 setContentType 不会自动设置 Header，而 Filter 依赖 Header 判断
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        request.setContent("{\"test\":\"value\"}".getBytes(StandardCharsets.UTF_8));
        request.addHeader("X-Trace-Id", "12345");

        MockHttpServletResponse response = new MockHttpServletResponse();

        // 模拟 FilterChain
        FilterChain mockChain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) throws IOException, ServletException {
                // 断言：JSON 请求应该被包装，以便多次读取 Body
                assertTrue(req instanceof CachedBodyHttpServletRequest, 
                        "Request should be wrapped with CachedBodyHttpServletRequest for JSON content");
                assertTrue(res instanceof ContentCachingResponseWrapper, 
                        "Response should be wrapped with ContentCachingResponseWrapper");
                
                // 模拟 Controller 读取 Body
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                assertEquals("{\"test\":\"value\"}", body);

                // 模拟 Controller 写入响应
                res.setContentType("application/json");
                res.getWriter().write("{\"status\":\"ok\"}");
            }
        };

        // 执行过滤器
        filter.doFilter(request, response, mockChain);

        // 断言：验证响应体是否被正确回写 (copyBodyToResponse)
        assertEquals("{\"status\":\"ok\"}", response.getContentAsString(), 
                "Response body should be copied back to original response");
    }

    @Test
    void testDoFilter_BinaryRequest_ShouldNotWrapRequest() throws ServletException, IOException {
        // 准备二进制上传请求
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/upload");
        request.setContentType("application/octet-stream");
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
        request.setContent(new byte[]{1, 2, 3, 4, 5});

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain mockChain = (req, res) -> {
            // 断言：二进制请求不应被包装，避免内存溢出
            assertFalse(req instanceof CachedBodyHttpServletRequest, 
                    "Request should NOT be wrapped for binary content");
            // Response 依然会被包装，这是过滤器的默认行为
            assertTrue(res instanceof ContentCachingResponseWrapper);
        };

        filter.doFilter(request, response, mockChain);
    }

    @Test
    void testGetMessagePayload_WithBinaryContent_ShouldReturnBinTag() throws IOException {
        // 构造包含 0x00 (NULL byte) 的数据
        MockHttpServletRequest request = new MockHttpServletRequest();
        byte[] binaryData = new byte[] { 'H', 'e', 'l', 'l', 'o', 0x00, 'W', 'o', 'r', 'l', 'd' };
        request.setContent(binaryData);
        
        // 手动包装，因为 getMessagePayload 依赖 CachedBodyHttpServletRequest
        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);

        // 调用受保护的方法
        String payload = filter.getMessagePayload(wrappedRequest);
        
        // 验证是否触发了二进制检测逻辑
        assertEquals("[BIN]", payload, "Should detect binary content containing null byte");
    }

    @Test
    void testGetMessagePayload_WithTextContent_ShouldReturnString() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String json = "{\"key\":\"中文内容\"}";
        request.setContent(json.getBytes(StandardCharsets.UTF_8));
        
        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);

        String payload = filter.getMessagePayload(wrappedRequest);
        
        assertEquals(json, payload, "Should return correct string for text content");
    }
    
    @Test
    void testDoFilter_ResponseContentTypeCheck() throws ServletException, IOException {
        // 测试 Response Content-Type 检查逻辑 (虽然无法直接断言日志，但可以确保逻辑无异常)
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/image");
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        FilterChain mockChain = (req, res) -> {
            res.setContentType("image/png");
            res.getOutputStream().write(new byte[]{0, 1, 2});
        };
        
        assertDoesNotThrow(() -> filter.doFilter(request, response, mockChain));
        
        // 验证二进制响应也被回写了
        assertEquals(3, response.getContentAsByteArray().length);
    }

    @Test
    void testDoFilter_InvalidMediaType_ShouldNotThrow() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        // 设置一个非法的 Content-Type，触发 MediaType.parseMediaType 抛出 InvalidMediaTypeException
        request.addHeader(HttpHeaders.CONTENT_TYPE, "invalid-type;");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain mockChain = (req, res) -> {};

        assertDoesNotThrow(() -> filter.doFilter(request, response, mockChain));
    }

    @Test
    void testDoFilter_AlreadyWrappedRequest_ShouldNotWrapAgain() throws ServletException, IOException {
        MockHttpServletRequest rawRequest = new MockHttpServletRequest("POST", "/api/test");
        rawRequest.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        // 手动包装
        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(rawRequest);

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain mockChain = (req, res) -> {
            // 验证传入 FilterChain 的请求对象就是我们传入的那个，没有被再次包装
            assertSame(wrappedRequest, req, "Should use the existing wrapper");
        };

        filter.doFilter(wrappedRequest, response, mockChain);
    }

    @Test
    void testLog_WithClientInfo_QueryString_And_TraceId() throws ServletException, IOException {
        filter.setIncludeClientInfo(true);
        filter.setIncludeQueryString(true);
        filter.setIncludeHeaders(false); // 关闭 Header 打印，以测试 TraceID 单独打印的分支
        filter.setTraceIdHeaderName("X-Trace-Id");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.setParameter("param", "value");
        request.setRemoteAddr("127.0.0.1");
        request.setRemoteUser("testuser");
        request.setSession(new MockHttpSession(null, "session123"));
        request.addHeader("X-Trace-Id", "trace-123");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain mockChain = (req, res) -> {};

        filter.doFilter(request, response, mockChain);

        // 验证日志中包含预期的客户端信息和 TraceID
        assertNotNull(capturedBeforeMessage, "Before-request log should be generated");
        assertTrue(capturedBeforeMessage.contains("client=127.0.0.1"), "Should log client IP");
        assertTrue(capturedBeforeMessage.contains("user=testuser"), "Should log remote user");
        assertTrue(capturedBeforeMessage.contains("session=session123"), "Should log session ID");
        assertTrue(capturedBeforeMessage.contains("x-trace-id=trace-123"), "Should log Trace ID");
    }

    @Test
    void testLog_HeaderPredicate_ShouldMaskHeaders() throws ServletException, IOException {
        // 设置 Header 过滤器，忽略 "Secret"
        filter.setHeaderPredicate(h -> !h.equalsIgnoreCase("Secret"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("Secret", "password");
        request.addHeader("Public", "data");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain mockChain = (req, res) -> {};

        filter.doFilter(request, response, mockChain);

        assertNotNull(capturedBeforeMessage);
        // HttpHeaders.toString() 的格式可能因 Spring 版本而异 (例如 [Secret:"masked"] 或 {Secret=[masked]})
        // 改用更健壮的断言：验证包含 masked 且不包含原始密码
        assertTrue(capturedBeforeMessage.contains("Secret"), "Should contain Secret header name");
        assertTrue(capturedBeforeMessage.contains("masked"), "Secret value should be masked");
        assertFalse(capturedBeforeMessage.contains("password"), "Original secret value should not be logged");
        assertTrue(capturedBeforeMessage.contains("Public"), "Public header should be visible");
    }

    @Test
    void testLog_ResponseAttachment_ShouldNotLogPayload() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/download");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain mockChain = (req, res) -> {
            // 设置附件下载 Header
            ((HttpServletResponse) res).addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=test.txt");
            res.getWriter().write("file content");
        };

        filter.doFilter(request, response, mockChain);

        assertNotNull(capturedAfterMessage);
        assertTrue(capturedAfterMessage.contains("payload=[attachment; filename=test.txt]"), "Payload should show content-disposition for attachments");
        assertFalse(capturedAfterMessage.contains("file content"), "Should NOT log actual file content");
    }

    @Test
    void testPayload_Truncation() throws IOException {
        filter.setMaxPayloadLength(5);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent("1234567890".getBytes(StandardCharsets.UTF_8));
        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request);

        String payload = filter.getMessagePayload(wrapped);
        assertEquals("12345...", payload);
    }

    @Test
    void testLog_UnknownHttpStatusCode() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(999); // 设置一个非标准的 HTTP 状态码

        FilterChain mockChain = (req, res) -> {};

        // 验证不会因为 HttpStatus.valueOf 抛出异常而中断，应该降级打印数字
        assertDoesNotThrow(() -> filter.doFilter(request, response, mockChain));
    }

    @Test
    void testGetMessagePayload_UnsupportedEncoding() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent("content".getBytes(StandardCharsets.UTF_8));
        request.setCharacterEncoding("INVALID_ENCODING_XYZ"); // 设置无效编码

        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);

        String payload = filter.getMessagePayload(wrappedRequest);

        assertEquals("unknown", payload, "Should return 'unknown' when encoding is unsupported");
    }

    @Test
    void testDoFilter_IOException_DuringRequestCaching() {
        MockHttpServletRequest rawRequest = new MockHttpServletRequest("POST", "/api/fail");
        rawRequest.setContentType("application/json");
        rawRequest.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        // 使用 Wrapper 模拟 getInputStream 抛出 IOException
        HttpServletRequest throwingRequest = new HttpServletRequestWrapper(rawRequest) {
            @Override
            public ServletInputStream getInputStream() throws IOException {
                throw new IOException("Simulated IO Error");
            }
        };

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        // 过滤器应捕获 IOException，记录日志后重新抛出
        assertThrows(IOException.class, () -> filter.doFilter(throwingRequest, response, chain));
    }
}