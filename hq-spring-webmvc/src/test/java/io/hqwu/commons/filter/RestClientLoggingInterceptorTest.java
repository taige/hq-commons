package io.hqwu.commons.filter;

import io.hqwu.commons.util.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for {@link RestClientLoggingInterceptor}
 */
class RestClientLoggingInterceptorTest {

    private RestClientLoggingInterceptor interceptor;
    private Logger mockLogger;
    private ClientHttpRequestExecution execution;

    @BeforeEach
    void setUp() {
        // Mock 内部使用的 Logger
        mockLogger = mock(Logger.class);
        execution = mock(ClientHttpRequestExecution.class);

        interceptor = new RestClientLoggingInterceptor("TestClient");
        // 使用 ReflectionTestUtils 注入 mockLogger，因为它是 private final 且在构造函数中初始化的
        ReflectionTestUtils.setField(interceptor, "LOGGER", mockLogger);
    }

    @Test
    void testIntercept_TextContent_ShouldLogFullBody() throws IOException {
        // Arrange
        when(mockLogger.isDebugEnabled()).thenReturn(true);

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, URI.create("http://example.com/api"));
        request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] reqBody = "{\"req\":\"data\"}".getBytes(StandardCharsets.UTF_8);

        MockClientHttpResponse response = new MockClientHttpResponse("{\"res\":\"data\"}".getBytes(StandardCharsets.UTF_8), HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        when(execution.execute(any(), any())).thenReturn(response);

        // Act
        ClientHttpResponse result = interceptor.intercept(request, reqBody, execution);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());

        // 验证请求日志：Info 记录基本信息，Debug 记录 Body
        verify(mockLogger).info(contains("%sing"), any(), any(), any(), any());
        verify(mockLogger).debug(contains("Request body: %s"), any(), eq("{\"req\":\"data\"}"));

        // 验证响应日志
        verify(mockLogger).info(contains("%sed"), any(), any(), any(), any(), any(), any());
        verify(mockLogger).debug(contains("Response body: %s"), any(), eq("{\"res\":\"data\"}"));
    }

    @Test
    void testIntercept_BinaryContent_ShouldLogMetadataOnly() throws IOException {
        // Arrange
        when(mockLogger.isDebugEnabled()).thenReturn(true);

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, URI.create("http://example.com/upload"));
        request.getHeaders().setContentType(MediaType.APPLICATION_OCTET_STREAM);
        byte[] reqBody = new byte[]{1, 2, 3, 4, 5};

        MockClientHttpResponse response = new MockClientHttpResponse(new byte[]{0xA, 0xB}, HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.IMAGE_PNG);
        response.getHeaders().setContentLength(2);

        when(execution.execute(any(), any())).thenReturn(response);

        // Act
        interceptor.intercept(request, reqBody, execution);

        // Assert
        // 验证请求日志：应该是 [Binary data] 格式，包含类型和长度
        verify(mockLogger).debug(contains("Request body: [Binary data]"), any(), eq(MediaType.APPLICATION_OCTET_STREAM), eq(5));

        // 验证响应日志
        verify(mockLogger).debug(contains("Response body: [Binary data]"), any(), eq(MediaType.IMAGE_PNG), eq(2L));
    }

    @Test
    void testIntercept_DebugDisabled_ShouldNotLogBody() throws IOException {
        // Arrange
        when(mockLogger.isDebugEnabled()).thenReturn(false);

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://example.com/api"));
        when(execution.execute(any(), any())).thenReturn(new MockClientHttpResponse(new byte[0], HttpStatus.OK));

        // Act
        interceptor.intercept(request, new byte[0], execution);

        // Assert
        // 验证从未调用过 debug 方法
        verify(mockLogger, never()).debug(anyString(), (Object) any());
        verify(mockLogger, never()).debug(anyString(), any(), any());
        // 验证调用了 info 方法 (请求和响应各一次)
        verify(mockLogger).info(anyString(), any(), any(), any(), any()); // 请求日志 (5个参数)
        verify(mockLogger).info(anyString(), any(), any(), any(), any(), any(), any()); // 响应日志 (7个参数)
    }

    @Test
    void testIntercept_WithBaseUri_ShouldRelativizeUri() throws IOException {
        // Arrange
        URI baseUri = URI.create("http://example.com/api/");
        interceptor = new RestClientLoggingInterceptor("TestClient", baseUri);
        ReflectionTestUtils.setField(interceptor, "LOGGER", mockLogger);

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://example.com/api/users/1"));
        when(execution.execute(any(), any())).thenReturn(new MockClientHttpResponse(new byte[0], HttpStatus.OK));

        // Act
        interceptor.intercept(request, new byte[0], execution);

        // Assert
        // 验证日志中的 URI 是相对路径 "/users/1"
        // LOGGER.info("[%s]%sing : %s, Headers: %s", logTag, method, requestUri, headers);
        // 参数顺序：format, logTag, method, requestUri, headers
        verify(mockLogger).info(anyString(), any(), eq(HttpMethod.GET), eq("/users/1"), any());
    }

    @Test
    void testIntercept_NoContentType_ShouldTreatAsBinary() throws IOException {
        // Arrange
        when(mockLogger.isDebugEnabled()).thenReturn(true);

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, URI.create("http://example.com/api"));
        // 不设置 Content-Type
        request.getHeaders().remove(HttpHeaders.CONTENT_TYPE);
        byte[] reqBody = "data".getBytes();

        when(execution.execute(any(), any())).thenReturn(new MockClientHttpResponse(new byte[0], HttpStatus.OK));

        // Act
        interceptor.intercept(request, reqBody, execution);

        // Assert
        // 没有 Content-Type 默认为非文本，走二进制日志逻辑
        verify(mockLogger).debug(contains("Request body: [Binary data]"), any(), eq(null), eq(4));
    }
}