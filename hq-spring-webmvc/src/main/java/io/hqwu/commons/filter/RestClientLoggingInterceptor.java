package io.hqwu.commons.filter;

import io.hqwu.commons.util.ClassUtil;
import io.hqwu.commons.util.Formatter;
import io.hqwu.commons.util.Logger;
import io.hqwu.commons.util.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * RestTemplate 的 REST 客户端日志拦截器
 *
 * <p>用于拦截 {@link org.springframework.web.client.RestTemplate} 发起的 HTTP 请求和响应，
 * 并记录详细的调用日志，包括请求方法、URI、请求头、请求体、响应状态码、响应头、响应体以及耗时等信息。</p>
 *
 * <p>日志级别：
 * <ul>
 *   <li>INFO 级别：记录请求的基本信息（方法、URI、请求头）和响应的基本信息（状态码、响应头、耗时）</li>
 *   <li>DEBUG 级别：额外记录请求体和响应体的详细内容（仅文本类型，二进制数据会记录类型和长度）</li>
 * </ul>
 * </p>
 *
 * <p><strong>注意：</strong>为了安全地记录响应体，必须使用
 * {@link org.springframework.http.client.BufferingClientHttpRequestFactory} 包装 RestTemplate 的请求工厂，
 * 以便响应体可以被多次读取。否则响应流读取后将无法在业务代码中再次使用。</p>
 *
 * <p>示例用法：
 * <pre>{@code
 * RestTemplate restTemplate = new RestTemplate(
 *     new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory())
 * );
 * restTemplate.getInterceptors().add(
 *     new RestClientLoggingInterceptor("MyRestClient")
 * );
 * }</pre>
 * </p>
 *
 * @author taige
 * @see org.springframework.http.client.ClientHttpRequestInterceptor
 * @see org.springframework.web.client.RestTemplate
 * @see org.springframework.http.client.BufferingClientHttpRequestFactory
 * @since 2020/3/31
 */
public class RestClientLoggingInterceptor implements ClientHttpRequestInterceptor {
    private final Logger LOGGER;

    private final String logTag;
    private final URI baseUri;


    public RestClientLoggingInterceptor(String loggerName) {
        this.logTag = ClassUtil.getShortClassName(loggerName);
        this.LOGGER = LoggerFactory.getLogger(loggerName);
        this.baseUri = null;
    }

    public RestClientLoggingInterceptor(String loggerName, URI baseUri) {
        this.logTag = ClassUtil.getShortClassName(loggerName);
        this.LOGGER = LoggerFactory.getLogger(loggerName);
        this.baseUri = baseUri;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        String requestUri = baseUri == null ? request.getURI().toString() : "/" + baseUri.relativize(request.getURI());
        HttpMethod method = request.getMethod();
        
        LOGGER.info("[%s]%sing : %s, Headers: %s", logTag, method, requestUri, request.getHeaders());
        if (LOGGER.isDebugEnabled() && ! HttpMethod.GET.equals(method) && ! HttpMethod.HEAD.equals(method)) {
            if (isTextType(request.getHeaders().getContentType())) {
                LOGGER.debug("[%s]Request body: %s", logTag, new String(body, StandardCharsets.UTF_8));
            } else {
                LOGGER.debug("[%s]Request body: [Binary data] Content-Type: %s, Length: %d", logTag, request.getHeaders().getContentType(), body.length);
            }
        }

        long startMS = System.currentTimeMillis();
        ClientHttpResponse response = execution.execute(request, body);
        HttpHeaders headers = response.getHeaders();
        LOGGER.info("[%s]%sed : %s, Status: %s, Headers: %s, time: %sms", logTag,
                method, requestUri, response.getStatusCode(), headers, Formatter.formatNS(System.currentTimeMillis() - startMS));

        if (LOGGER.isDebugEnabled()) {
            if (isTextType(headers.getContentType())) {
                Charset contentCharset = Optional.ofNullable(Optional.ofNullable(headers.getContentType())
                        .orElse(MediaType.APPLICATION_JSON).getCharset()).orElse(StandardCharsets.UTF_8);
                String responseBody = StreamUtils.copyToString(response.getBody(), contentCharset);
                LOGGER.debug("[%s]Response body: %s", logTag, responseBody);
            } else {
                LOGGER.debug("[%s]Response body: [Binary data] Content-Type: %s, Length: %d", logTag, headers.getContentType(), headers.getContentLength());
            }
        }

        return response;
    }

    private boolean isTextType(MediaType mediaType) {
        if (mediaType == null) {
            return false;
        }
        return "text".equals(mediaType.getType()) || mediaType.getSubtype().contains("json") || mediaType.getSubtype().contains("xml") || mediaType.getSubtype().contains("html");
    }

}
