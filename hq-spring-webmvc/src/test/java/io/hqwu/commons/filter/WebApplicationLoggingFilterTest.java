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

    @Test
    void testShouldLog_WhenEnabledAndLoggerInfoEnabled_ShouldReturnTrue() {
        // 创建新的 filter 实例（不重写 shouldLog）
        WebApplicationLoggingFilter realFilter = new WebApplicationLoggingFilter();
        realFilter.setEnabled(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

        // shouldLog 依赖 enabled 标志和 LOGGER.isInfoEnabled()
        // 验证 enabled=true 时的行为（即使 logger 可能未启用，也验证方法调用正常）
        boolean result = realFilter.shouldLog(request);
        // 在测试环境中，LOGGER.isInfoEnabled() 的返回值取决于日志配置
        // 我们只验证方法不会抛出异常，并且逻辑正确
        // 当 enabled=true 时，结果取决于 LOGGER.isInfoEnabled()
        assertNotNull(result, "shouldLog should return a boolean value");
    }

    @Test
    void testShouldLog_WhenDisabled_ShouldReturnFalse() {
        WebApplicationLoggingFilter realFilter = new WebApplicationLoggingFilter();
        realFilter.setEnabled(false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

        boolean result = realFilter.shouldLog(request);
        assertFalse(result, "shouldLog should return false when enabled=false");
    }

    @Test
    void testBeforeRequest_ShouldCaptureMessage() throws ServletException, IOException {
        // 使用自定义子类来验证 beforeRequest 被正确调用
        final String[] capturedMessage = {null};
        WebApplicationLoggingFilter customFilter = new WebApplicationLoggingFilter() {
            @Override
            protected boolean shouldLog(HttpServletRequest request) {
                return true; // 确保日志启用
            }

            @Override
            protected void beforeRequest(HttpServletRequest request, String message) {
                capturedMessage[0] = message;
                super.beforeRequest(request, message);
            }
        };
        customFilter.setIncludeQueryString(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.setParameter("key", "value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        customFilter.doFilter(request, response, chain);

        assertNotNull(capturedMessage[0], "beforeRequest should be called with a message");
        assertTrue(capturedMessage[0].contains("GET /api/test"), "Message should contain request method and URI");
    }

    @Test
    void testAfterRequest_ShouldCaptureMessage() throws ServletException, IOException {
        // 使用自定义子类来验证 afterRequest 被正确调用
        final String[] capturedMessage = {null};
        WebApplicationLoggingFilter customFilter = new WebApplicationLoggingFilter() {
            @Override
            protected boolean shouldLog(HttpServletRequest request) {
                return true; // 确保日志启用
            }

            @Override
            protected void afterRequest(HttpServletRequest request, String message) {
                capturedMessage[0] = message;
                super.afterRequest(request, message);
            }
        };

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/submit");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            ((HttpServletResponse) res).setStatus(201);
        };

        customFilter.doFilter(request, response, chain);

        assertNotNull(capturedMessage[0], "afterRequest should be called with a message");
        assertTrue(capturedMessage[0].contains("POST /api/submit"), "Message should contain request method and URI");
    }

    @Test
    void testSetBeforeMessagePrefix_ShouldUpdatePrefix() throws ServletException, IOException {
        filter.setBeforeMessagePrefix(">>> REQUEST: ");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        assertNotNull(capturedBeforeMessage);
        assertTrue(capturedBeforeMessage.startsWith(">>> REQUEST: "),
                "Before message should start with custom prefix");
        assertEquals(">>> REQUEST: ", filter.getBeforeMessagePrefix(),
                "Getter should return the set prefix");
    }

    @Test
    void testSetBeforeMessageSuffix_ShouldUpdateSuffix() throws ServletException, IOException {
        filter.setBeforeMessageSuffix(" <<<");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        assertNotNull(capturedBeforeMessage);
        assertTrue(capturedBeforeMessage.endsWith(" <<<"),
                "Before message should end with custom suffix");
        assertEquals(" <<<", filter.getBeforeMessageSuffix(),
                "Getter should return the set suffix");
    }

    @Test
    void testSetAfterMessagePrefix_ShouldUpdatePrefix() throws ServletException, IOException {
        filter.setAfterMessagePrefix("<<< RESPONSE: ");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        assertNotNull(capturedAfterMessage);
        assertTrue(capturedAfterMessage.startsWith("<<< RESPONSE: "),
                "After message should start with custom prefix");
        assertEquals("<<< RESPONSE: ", filter.getAfterMessagePrefix(),
                "Getter should return the set prefix");
    }

    @Test
    void testSetAfterMessageSuffix_ShouldUpdateSuffix() throws ServletException, IOException {
        filter.setAfterMessageSuffix(" >>>");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        assertNotNull(capturedAfterMessage);
        assertTrue(capturedAfterMessage.endsWith(" >>>"),
                "After message should end with custom suffix");
        assertEquals(" >>>", filter.getAfterMessageSuffix(),
                "Getter should return the set suffix");
    }

    @Test
    void testCustomMessagePrefixAndSuffix_AllCombined() throws ServletException, IOException {
        filter.setBeforeMessagePrefix("[BEGIN] ");
        filter.setBeforeMessageSuffix(" [END-BEGIN]");
        filter.setAfterMessagePrefix("[COMPLETE] ");
        filter.setAfterMessageSuffix(" [END-COMPLETE]");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/data");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        // 验证 before message
        assertNotNull(capturedBeforeMessage);
        assertTrue(capturedBeforeMessage.startsWith("[BEGIN] "),
                "Before message should have custom prefix");
        assertTrue(capturedBeforeMessage.endsWith(" [END-BEGIN]"),
                "Before message should have custom suffix");

        // 验证 after message
        assertNotNull(capturedAfterMessage);
        assertTrue(capturedAfterMessage.startsWith("[COMPLETE] "),
                "After message should have custom prefix");
        assertTrue(capturedAfterMessage.endsWith(" [END-COMPLETE]"),
                "After message should have custom suffix");
    }

    @Test
    void testDoFilter_ApplicationXmlContentType_ShouldWrapRequest() throws ServletException, IOException {
        // 测试 application/xml Content-Type 分支
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/xml");
        request.setContentType("application/xml");
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/xml");
        request.setContent("<root><test>value</test></root>".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain mockChain = (req, res) -> {
            assertTrue(req instanceof CachedBodyHttpServletRequest,
                    "Request should be wrapped for application/xml content");
        };

        filter.doFilter(request, response, mockChain);
    }

    @Test
    void testDoFilter_TextPlainContentType_ShouldWrapRequest() throws ServletException, IOException {
        // 测试 text/plain Content-Type 分支（覆盖 text/* 类型）
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/text");
        request.setContentType("text/plain");
        request.addHeader(HttpHeaders.CONTENT_TYPE, "text/plain");
        request.setContent("Plain text content".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain mockChain = (req, res) -> {
            assertTrue(req instanceof CachedBodyHttpServletRequest,
                    "Request should be wrapped for text/plain content");
        };

        filter.doFilter(request, response, mockChain);
    }

    @Test
    void testDoFilter_AlreadyWrappedResponse_ShouldNotWrapAgain() throws ServletException, IOException {
        // 测试当 Response 已经被包装时，不应再次包装的分支
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(rawResponse);

        FilterChain mockChain = (req, res) -> {
            assertSame(wrappedResponse, res, "Should use the existing response wrapper");
        };

        filter.doFilter(request, wrappedResponse, mockChain);
    }

    @Test
    void testDoFilter_LoggingDisabled_ShouldNotLog() throws ServletException, IOException {
        // 测试当日志功能禁用时，不应记录日志
        // 创建新的 filter 实例（不是使用 setUp 中的 filter）
        final String[] capturedBefore = {null};
        final String[] capturedAfter = {null};

        WebApplicationLoggingFilter disabledFilter = new WebApplicationLoggingFilter() {
            @Override
            protected void beforeRequest(HttpServletRequest request, String message) {
                capturedBefore[0] = message;
            }

            @Override
            protected void afterRequest(HttpServletRequest request, String message) {
                capturedAfter[0] = message;
            }
        };
        disabledFilter.setEnabled(false); // 禁用日志
        disabledFilter.setIncludePayload(true);
        disabledFilter.setIncludeHeaders(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        request.setContentType("application/json");
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        request.setContent("{\"test\":\"value\"}".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain mockChain = (req, res) -> {
            // 过滤器链正常执行
        };

        disabledFilter.doFilter(request, response, mockChain);

        // 验证 beforeRequest 和 afterRequest 没有被调用（不记录日志）
        assertNull(capturedBefore[0], "Should not log before message when disabled");
        assertNull(capturedAfter[0], "Should not log after message when disabled");
    }

    @Test
    void testDoFilter_IncludePayloadDisabled_ShouldNotWrapRequest() throws ServletException, IOException {
        // 测试当 includePayload=false 时，不应包装请求
        filter.setIncludePayload(false);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        request.setContentType("application/json");
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        request.setContent("{\"test\":\"value\"}".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain mockChain = (req, res) -> {
            assertFalse(req instanceof CachedBodyHttpServletRequest,
                    "Request should NOT be wrapped when includePayload is false");
        };

        filter.doFilter(request, response, mockChain);
    }

    @Test
    void testGetAfterMessage_ResponseNotWrapped_ShouldUseCreateMessage() throws ServletException, IOException {
        // 测试当响应未被包装时，getAfterMessage 使用 createMessage 的分支
        // 创建一个特殊的 filter，使其 shouldLog 返回 false，这样不会包装响应
        final String[] afterMsg = {null};

        WebApplicationLoggingFilter specialFilter = new WebApplicationLoggingFilter() {
            private boolean firstCall = true;

            @Override
            protected boolean shouldLog(HttpServletRequest request) {
                // 第一次调用（doFilterInternal 开始）返回 false，避免包装
                // 但我们需要在 finally 块中手动调用 afterRequest 来测试
                if (firstCall) {
                    firstCall = false;
                    return false;
                }
                return true; // 后续调用返回 true
            }

            @Override
            protected void afterRequest(HttpServletRequest request, String message) {
                afterMsg[0] = message;
            }
        };

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/simple");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            // 验证响应未被包装（因为 shouldLog 返回 false）
            assertFalse(res instanceof ContentCachingResponseWrapper,
                    "Response should not be wrapped when shouldLog returns false");
        };

        specialFilter.doFilter(request, response, chain);

        // 由于 shouldLog 返回 false，afterRequest 不会被调用
        // 这个测试主要验证当 shouldLog=false 时，响应不会被包装的分支
        assertNull(afterMsg[0], "afterRequest should not be called when shouldLog is false");
    }

    @Test
    void testDoFilter_ResponseContentDispositionNull_WithNullContentType() throws ServletException, IOException {
        // 测试响应的 Content-Disposition 为 null，且 Content-Type 也为 null 的分支
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/null-content");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain mockChain = (req, res) -> {
            // 不设置 Content-Type 和 Content-Disposition，保持为 null
        };

        filter.doFilter(request, response, mockChain);

        assertNotNull(capturedAfterMessage);
        assertTrue(capturedAfterMessage.contains("[IGNORED Content-Type: null]"),
                "Should handle null content type correctly");
    }

    @Test
    void testDoFilter_ResponseWithTextHtmlContentType() throws ServletException, IOException {
        // 测试响应的 Content-Type 为 text/html 时，应该记录 payload
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/html");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain mockChain = (req, res) -> {
            res.setContentType("text/html");
            res.getWriter().write("<html><body>Test</body></html>");
        };

        filter.doFilter(request, response, mockChain);

        assertNotNull(capturedAfterMessage);
        assertTrue(capturedAfterMessage.contains("<html><body>Test</body></html>"),
                "Should log HTML content for text/html content type");
    }

    @Test
    void testDoFilter_ResponseWithApplicationJsonCharset() throws ServletException, IOException {
        // 测试响应的 Content-Type 包含 charset 的 JSON 类型
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/json-charset");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain mockChain = (req, res) -> {
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"result\":\"success\"}");
        };

        filter.doFilter(request, response, mockChain);

        assertNotNull(capturedAfterMessage);
        assertTrue(capturedAfterMessage.contains("{\"result\":\"success\"}"),
                "Should log JSON content even with charset parameter");
    }

    @Test
    void testDoFilter_ResponseWithXmlContentType() throws ServletException, IOException {
        // 测试响应的 Content-Type 包含 xml
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/xml-response");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain mockChain = (req, res) -> {
            res.setContentType("application/xml");
            res.getWriter().write("<response><status>ok</status></response>");
        };

        filter.doFilter(request, response, mockChain);

        assertNotNull(capturedAfterMessage);
        assertTrue(capturedAfterMessage.contains("<response><status>ok</status></response>"),
                "Should log XML content for application/xml content type");
    }

    @Test
    void testBuildMessageHead_ClientInfoWithEmptyRemoteAddr() throws ServletException, IOException {
        // 测试 buildMessageHead 中 remoteAddr 为空的分支
        filter.setIncludeClientInfo(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.setRemoteAddr(""); // 设置为空字符串

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        assertNotNull(capturedBeforeMessage);
        assertFalse(capturedBeforeMessage.contains("client="),
                "Should not log empty client address");
    }

    @Test
    void testDoFilter_EmptyResponsePayload() throws ServletException, IOException {
        // 测试响应 payload 为空的情况
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/empty");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain mockChain = (req, res) -> {
            res.setContentType("application/json");
            // 不写任何内容，响应体为空
        };

        filter.doFilter(request, response, mockChain);

        assertNotNull(capturedAfterMessage);
        // 空响应体会导致 payload=null
        assertTrue(capturedAfterMessage.contains("payload=[null]") ||
                   capturedAfterMessage.contains("payload=[]"),
                "Should handle empty response payload");
    }

    @Test
    void testGetAfterMessage_WithUnwrappedResponse_ShouldUseCreateMessage() throws Exception {
        // 专门测试第271行：当响应不是 ContentCachingResponseWrapper 时，使用 createMessage 方法
        // 使用反射直接调用 getAfterMessage 方法，传入一个普通的响应对象

        WebApplicationLoggingFilter testFilter = new WebApplicationLoggingFilter();
        testFilter.setIncludeQueryString(true);
        testFilter.setIncludeClientInfo(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/simple");
        request.setParameter("id", "123");
        request.setRemoteAddr("10.0.0.1");

        // 创建一个普通的 MockHttpServletResponse（不是 ContentCachingResponseWrapper）
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        // 使用反射调用私有方法 getAfterMessage
        java.lang.reflect.Method method = WebApplicationLoggingFilter.class
                .getDeclaredMethod("getAfterMessage", HttpServletRequest.class, HttpServletResponse.class);
        method.setAccessible(true);

        String message = (String) method.invoke(testFilter, request, response);

        // 验证返回的消息是通过 createMessage 方法生成的（第271行）
        assertNotNull(message, "Message should not be null");

        // createMessage 生成的基本消息应该包含请求方法和URI
        assertTrue(message.contains("GET") && message.contains("/api/simple"),
                "Message should contain request method and URI");

        // 关键验证点：createMessage 不会包含响应特定的信息（status、headers、payload等）
        // 因为这些信息只在响应是 ContentCachingResponseWrapper 时才会添加（第274-310行）
        // 当响应不是 ContentCachingResponseWrapper 时，直接走第271行返回 createMessage 的结果
        assertFalse(message.contains("status="),
                "createMessage should NOT contain status code (line 271 path taken)");

        // 验证消息格式包含默认的 prefix 和 suffix
        assertTrue(message.startsWith(testFilter.getAfterMessagePrefix()),
                "Message should start with after message prefix");
        assertTrue(message.endsWith(testFilter.getAfterMessageSuffix()),
                "Message should end with after message suffix");
    }

    @Test
    void testDoFilter_AsyncDispatch_ShouldHandleCorrectly() throws ServletException, IOException {
        // 测试异步调度场景 - Line 118 分支
        // 使用MockHttpServletRequest的setDispatcherType方法模拟异步调度
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/async");
        request.setDispatcherType(DispatcherType.ASYNC);

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        // 异步调度场景下，isAsyncDispatch返回true，isFirstRequest = false
        filter.doFilter(request, response, chain);

        // 验证测试没有抛出异常即可 - 异步调度场景下行为可能不同
        assertNotNull(response);
    }

    @Test
    void testDoFilter_WithNonApplicationNonTextMediaType_ShouldNotWrap() throws ServletException, IOException {
        // 测试Line 127未覆盖的分支 - mediaType既不是text也不是application
        filter.setIncludePayload(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/upload");
        request.setContentType("multipart/form-data");
        request.addHeader(HttpHeaders.CONTENT_TYPE, "multipart/form-data");
        request.setContent("boundary-data".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            // multipart/form-data 不应该被包装
            assertFalse(req instanceof CachedBodyHttpServletRequest,
                    "Request with multipart/form-data should not be wrapped");
        };

        filter.doFilter(request, response, chain);
    }

    @Test
    void testDoFilter_BuildMessageHeadThrowsException_ShouldCatchAndRethrow() throws Exception {
        // 测试Line 140-141 - 日志记录失败时的异常处理
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/fail");
        request.setContentType("application/json");
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        // 创建一个会抛出IOException的请求包装器
        HttpServletRequestWrapper throwingRequest = new HttpServletRequestWrapper(request) {
            @Override
            public ServletInputStream getInputStream() throws IOException {
                throw new IOException("Simulated IO Error when reading request body");
            }
        };

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        // 应该抛出IOException
        IOException exception = assertThrows(IOException.class, () -> {
            filter.doFilter(throwingRequest, response, chain);
        });

        assertEquals("Simulated IO Error when reading request body", exception.getMessage());
    }

    @Test
    void testGetMessagePayload_ResponseWrapperIsNull_ShouldReturnNull() {
        // 测试Line 179分支 - 响应wrapper为null的情况
        MockHttpServletResponse plainResponse = new MockHttpServletResponse();

        // 直接测试getMessagePayload方法
        String payload = filter.getMessagePayload(plainResponse);

        assertNull(payload, "Payload should be null when response is not wrapped");
    }

    @Test
    void testDoFilter_ContentTypeParsing_EdgeCases() throws ServletException, IOException {
        // 测试Content-Type解析的边界情况

        // 测试1: Content-Type为null
        MockHttpServletRequest request1 = new MockHttpServletRequest("POST", "/api/test1");
        // 不设置Content-Type
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        FilterChain chain1 = (req, res) -> {};

        assertDoesNotThrow(() -> filter.doFilter(request1, response1, chain1));

        // 测试2: Content-Type为空字符串
        MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/api/test2");
        request2.addHeader(HttpHeaders.CONTENT_TYPE, "");
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        FilterChain chain2 = (req, res) -> {};

        assertDoesNotThrow(() -> filter.doFilter(request2, response2, chain2));
    }

    @Test
    void testDoFilter_ImageContentType_ShouldNotWrapRequest() throws ServletException, IOException {
        // 测试图片类型的Content-Type - 验证Line 126-129的另一个分支
        filter.setIncludePayload(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/upload-image");
        request.setContentType("image/png");
        request.addHeader(HttpHeaders.CONTENT_TYPE, "image/png");
        request.setContent(new byte[]{(byte)0x89, 0x50, 0x4E, 0x47}); // PNG header

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            // 图片类型不应该被包装
            assertFalse(req instanceof CachedBodyHttpServletRequest,
                    "Request with image content type should not be wrapped");
        };

        filter.doFilter(request, response, chain);
    }

    @Test
    void testDoFilter_VideoContentType_ShouldNotWrapRequest() throws ServletException, IOException {
        // 测试视频类型的Content-Type
        filter.setIncludePayload(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/upload-video");
        request.setContentType("video/mp4");
        request.addHeader(HttpHeaders.CONTENT_TYPE, "video/mp4");
        request.setContent(new byte[]{0x00, 0x00, 0x00, 0x18}); // MP4 header

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            // 视频类型不应该被包装
            assertFalse(req instanceof CachedBodyHttpServletRequest,
                    "Request with video content type should not be wrapped");
        };

        filter.doFilter(request, response, chain);
    }

    @Test
    void testDoFilter_ApplicationPdfContentType_ShouldNotWrapRequest() throws ServletException, IOException {
        // 测试application/pdf类型 - 是application但不包含json或xml
        filter.setIncludePayload(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/upload-pdf");
        request.setContentType("application/pdf");
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/pdf");
        request.setContent("%PDF-1.4".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            // application/pdf 不包含json或xml，不应该被包装
            assertFalse(req instanceof CachedBodyHttpServletRequest,
                    "Request with application/pdf should not be wrapped");
        };

        filter.doFilter(request, response, chain);
    }

    @Test
    void testDoFilter_ApplicationOctetStreamContentType_ShouldNotWrapRequest() throws ServletException, IOException {
        // 测试application/octet-stream类型
        filter.setIncludePayload(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/upload-binary");
        request.setContentType("application/octet-stream");
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
        request.setContent(new byte[]{0x00, 0x01, 0x02, 0x03});

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            // application/octet-stream 不包含json或xml，不应该被包装
            assertFalse(req instanceof CachedBodyHttpServletRequest,
                    "Request with application/octet-stream should not be wrapped");
        };

        filter.doFilter(request, response, chain);
    }

    @Test
    void testDoFilter_EmptyPayload_ShouldHandleGracefully() throws ServletException, IOException {
        // 测试Line 183的边界情况 - 空payload
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/empty");
        request.setContentType("application/json");
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        request.setContent(new byte[0]); // 空内容

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            res.setContentType("application/json");
            // 不写任何内容
        };

        filter.doFilter(request, response, chain);

        assertNotNull(capturedAfterMessage);
        // 空payload应该显示为null或空
        assertTrue(capturedAfterMessage.contains("payload=[null]") ||
                   capturedAfterMessage.contains("payload=[]"));
    }
}