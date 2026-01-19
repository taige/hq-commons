package io.hqwu.commons.filter;

import io.hqwu.commons.servlet.support.CachedBodyHttpServletRequest;
import io.hqwu.commons.util.Logger;
import io.hqwu.commons.util.StringUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.AbstractRequestLoggingFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.stream.Collectors;

/**
 * Web应用日志过滤器，用于记录RESTful服务的HTTP请求和响应详细信息
 *
 * <p>该过滤器扩展了Spring框架的{@link AbstractRequestLoggingFilter}，
 * 专门用于记录Web应用中RESTful接口的请求和响应日志。主要功能包括：</p>
 *
 * <ul>
 *   <li>记录HTTP请求的方法、URI、查询参数、请求头和请求体</li>
 *   <li>记录HTTP响应的状态码、响应头和响应体</li>
 *   <li>支持缓存请求体和响应体，以便进行完整的日志记录</li>
 *   <li>智能检测二进制内容，避免记录图片、文件等非文本数据</li>
 *   <li>支持通过配置开启/关闭日志功能</li>
 *   <li>支持自定义跟踪ID头名称，便于请求链路追踪</li>
 * </ul>
 *
 * <p><strong>日志级别：</strong></p>
 * <ul>
 *   <li>INFO级别：记录请求和响应的详细信息</li>
 *   <li>DEBUG级别：记录额外的调试信息和异常信息</li>
 * </ul>
 *
 * <p><strong>使用示例：</strong></p>
 * <pre>{@code
 * @Bean
 * public WebApplicationLoggingFilter loggingFilter() {
 *     WebApplicationLoggingFilter filter = new WebApplicationLoggingFilter();
 *     filter.setIncludeQueryString(true);
 *     filter.setIncludePayload(true);
 *     filter.setIncludeHeaders(true);
 *     filter.setIncludeClientInfo(true);
 *     filter.setMaxPayloadLength(10000);
 *     filter.setTraceIdHeaderName("X-Trace-Id");
 *     return filter;
 * }
 * }</pre>
 *
 * <p><strong>注意事项：</strong></p>
 * <ul>
 *   <li>请求体和响应体需要使用{@link CachedBodyHttpServletRequest}和
 *       {@link ContentCachingResponseWrapper}进行缓存，以支持多次读取</li>
 *   <li>仅记录文本类型的请求体和响应体（text/*, application/json, application/xml等），
 *       二进制内容会被标记为[BIN]或[IGNORED Content-Type: xxx]</li>
 *   <li>过滤器会自动检测并缓存符合条件的请求和响应，无需手动包装</li>
 * </ul>
 *
 * @author taige
 * @see AbstractRequestLoggingFilter
 * @see CachedBodyHttpServletRequest
 * @see ContentCachingResponseWrapper
 * @see RestClientLoggingInterceptor
 * @since 2020/4/5
 */
public class WebApplicationLoggingFilter extends AbstractRequestLoggingFilter {
    private static final Logger LOGGER = new Logger();

    private String beforeMessagePrefix = DEFAULT_BEFORE_MESSAGE_PREFIX;

    private String beforeMessageSuffix = DEFAULT_BEFORE_MESSAGE_SUFFIX;

    private String afterMessagePrefix = DEFAULT_AFTER_MESSAGE_PREFIX;

    private String afterMessageSuffix = DEFAULT_AFTER_MESSAGE_SUFFIX;

    private boolean enabled = true;

    private String traceIdHeaderName;

    public String getBeforeMessagePrefix() {
        return beforeMessagePrefix;
    }

    public String getBeforeMessageSuffix() {
        return beforeMessageSuffix;
    }

    public String getAfterMessagePrefix() {
        return afterMessagePrefix;
    }

    public String getAfterMessageSuffix() {
        return afterMessageSuffix;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getTraceIdHeaderName() {
        return traceIdHeaderName;
    }

    public void setTraceIdHeaderName(String traceIdHeaderName) {
        this.traceIdHeaderName = traceIdHeaderName;
    }

    @Override
    protected boolean shouldLog(HttpServletRequest request) {
        return enabled && LOGGER.isInfoEnabled();
    }

    @Override
    protected void beforeRequest(HttpServletRequest request, String message) {
        LOGGER.info(message);
    }

    @Override
    protected void afterRequest(HttpServletRequest request, String message) {
        LOGGER.info(message);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        boolean isFirstRequest = !isAsyncDispatch(request);
        HttpServletRequest requestToUse = request;

        boolean shouldLog = shouldLog(requestToUse);
        if (shouldLog && isIncludePayload() && isFirstRequest && !(request instanceof CachedBodyHttpServletRequest)) {
            try {
                String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
                MediaType mediaType = StringUtil.isNotBlank(contentType) ? MediaType.parseMediaType(contentType) : null;
                if (mediaType != null && (mediaType.getType().equalsIgnoreCase("text") ||
                        (mediaType.getType().equalsIgnoreCase("application") &&
                                (mediaType.getSubtype().toLowerCase().contains("json") ||
                                        mediaType.getSubtype().toLowerCase().contains("xml"))))) {
                    requestToUse = new CachedBodyHttpServletRequest(request);
                }
            } catch (IllegalArgumentException e) {
                LOGGER.debug("IGNORED cache request body exception: ", e);
            } catch (IOException e) {
                try {
                    StringBuilder msg = buildMessageHead(request, "cache request body failed: ");
                    HttpHeaders headers = new ServletServerHttpRequest(request).getHeaders();
                    msg.append(", headers=").append(headers);
                    LOGGER.info(msg.toString());
                } catch (Exception ex) {
                    LOGGER.info("log `cache request body failed` reason failed: ", ex);
                }
                throw e;
            }
        }

        if (shouldLog && isFirstRequest) {
            beforeRequest(requestToUse, getBeforeMessage(requestToUse));
        }
        HttpServletResponse responseToUse = response;
        if (shouldLog && isFirstRequest && ! (response instanceof ContentCachingResponseWrapper)) {
            responseToUse = new ContentCachingResponseWrapper(response);
        }

        try {
            filterChain.doFilter(requestToUse, responseToUse);
        }
        finally {
            if (shouldLog && !isAsyncStarted(requestToUse)) {
                afterRequest(requestToUse, getAfterMessage(requestToUse, responseToUse));
            }
            if (responseToUse instanceof ContentCachingResponseWrapper && responseToUse != response) {
                // new cachingResponse in this filter, then do copyBodyToResponse()
                ((ContentCachingResponseWrapper) responseToUse).copyBodyToResponse();
            }
        }
    }

    @Override
    protected String getMessagePayload(HttpServletRequest request) {
        CachedBodyHttpServletRequest wrapper =
                WebUtils.getNativeRequest(request, CachedBodyHttpServletRequest.class);
        return wrapper == null ? "[NOT LOGGABLE]" : _getMessagePayload(wrapper.getContentAsByteArray(), wrapper.getCharacterEncoding());
    }

    protected String getMessagePayload(HttpServletResponse response) {
        ContentCachingResponseWrapper wrapper =
                WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        return wrapper == null ? null :  _getMessagePayload(wrapper.getContentAsByteArray(), wrapper.getCharacterEncoding());
    }

    private String _getMessagePayload(byte[] contentAsByteArray, String characterEncoding) {
        if (contentAsByteArray != null && contentAsByteArray.length > 0) {
            int length = Math.min(contentAsByteArray.length, getMaxPayloadLength());
            // 简单的二进制检测：检查是否有 NULL 字节 (0x00)，这在文本中极少出现
            // 原有的 < 32 || > 126 逻辑会误杀 UTF-8 中文
            for (int i = 0; i < length && i < 50; i++) {
                if (contentAsByteArray[i] == 0) {
                    return "[BIN]";
                }
            }
            try {
                if (characterEncoding == null) {
                    characterEncoding = StandardCharsets.UTF_8.name();
                }
                String payload = new String(contentAsByteArray, 0, length, characterEncoding);
                return length == contentAsByteArray.length ? payload : payload.concat("...");
            }
            catch (UnsupportedEncodingException ex) {
                return "unknown";
            }
        }
        return null;
    }

    @Override
    public void setBeforeMessagePrefix(String beforeMessagePrefix) {
        super.setBeforeMessagePrefix(beforeMessagePrefix);
        this.beforeMessagePrefix = beforeMessagePrefix;
    }

    @Override
    public void setBeforeMessageSuffix(String beforeMessageSuffix) {
        super.setBeforeMessageSuffix(beforeMessageSuffix);
        this.beforeMessageSuffix = beforeMessageSuffix;
    }

    @Override
    public void setAfterMessagePrefix(String afterMessagePrefix) {
        super.setAfterMessagePrefix(afterMessagePrefix);
        this.afterMessagePrefix = afterMessagePrefix;
    }

    @Override
    public void setAfterMessageSuffix(String afterMessageSuffix) {
        super.setAfterMessageSuffix(afterMessageSuffix);
        this.afterMessageSuffix = afterMessageSuffix;
    }

    /**
     * Get the message to write to the log before the request.
     * @see #createMessage
     */
    private String getBeforeMessage(HttpServletRequest request) {
        StringBuilder msg = buildMessageHead(request, this.beforeMessagePrefix);

        if (isIncludeHeaders()) {
            HttpHeaders headers = new ServletServerHttpRequest(request).getHeaders();
            if (getHeaderPredicate() != null) {
                Enumeration<String> names = request.getHeaderNames();
                while (names.hasMoreElements()) {
                    String header = names.nextElement();
                    if (!getHeaderPredicate().test(header)) {
                        headers.set(header, "masked");
                    }
                }
            }
            msg.append(", headers=").append(headers);
        } else if (StringUtil.isNotBlank(traceIdHeaderName)) {
            String traceId = request.getHeader(traceIdHeaderName);
            msg.append(", ").append(traceIdHeaderName.toLowerCase()).append("=").append(traceId);
        }

        if (isIncludePayload()) {
            String payload = getMessagePayload(request);
            if (payload != null) {
                msg.append(", payload=").append(payload);
            }
        }

        msg.append(this.beforeMessageSuffix);
        return msg.toString();
    }

    /**
     * Get the message to write to the log after the request.
     * @see #createMessage
     */
    private String getAfterMessage(HttpServletRequest request, HttpServletResponse response) {
        if (! (response instanceof ContentCachingResponseWrapper)) {
            return createMessage(request, this.afterMessagePrefix, this.afterMessageSuffix);
        }
        // create response log message
        StringBuilder msg = buildMessageHead(request, this.afterMessagePrefix);

        // status code
        msg.append(", status=");
        try {
            msg.append(HttpStatus.valueOf(response.getStatus()));
        } catch (IllegalArgumentException e) {
            msg.append(response.getStatus());
        }

        if (StringUtil.isNotBlank(traceIdHeaderName)) {
            String traceId = request.getHeader(traceIdHeaderName);
            msg.append(", ").append(traceIdHeaderName.toLowerCase()).append("=").append(traceId);
        }
        if (isIncludeHeaders()) {
            HttpHeaders headers = new HttpHeaders();
            for (String header: response.getHeaderNames()) {
                headers.addAll(header, new ArrayList<>(response.getHeaders(header)));
            }
            msg.append(", headers=").append(headers);
        }

        if (isIncludePayload()) {
            String payload;
            String contentDisposition = response.getHeader(HttpHeaders.CONTENT_DISPOSITION);
            if (contentDisposition != null && contentDisposition.contains("attachment")) {
                payload = contentDisposition;
            } else {
                // 增加对 Response Content-Type 的检查，避免打印图片等二进制流
                String contentType = response.getContentType();
                if (contentType != null && (contentType.startsWith("text/") || contentType.contains("json") || contentType.contains("xml"))) {
                    payload = getMessagePayload(response);
                } else {
                    payload = "[IGNORED Content-Type: " + contentType + "]";
                }
            }
            msg.append(", payload=[").append(payload).append("]");
        }

        msg.append(this.afterMessageSuffix);
        return msg.toString();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private StringBuilder buildMessageHead(HttpServletRequest request, String prefix) {
        StringBuilder msg = new StringBuilder();
        msg.append(prefix);
        msg.append(request.getMethod()).append(" ");
        msg.append(request.getRequestURI());

        if (isIncludeQueryString()) {
            String params = request.getParameterMap().entrySet().stream()
                    .map(entry -> entry.getKey() + ":" + Arrays.toString(entry.getValue()))
                    .collect(Collectors.joining(", "));
            msg.append(", parameters={").append(params).append("}");
        }

        if (isIncludeClientInfo()) {
            String client = request.getRemoteAddr();
            if (StringUtils.hasLength(client)) {
                msg.append(", client=").append(client);
            }
            HttpSession session = request.getSession(false);
            if (session != null) {
                msg.append(", session=").append(session.getId());
            }
            String user = request.getRemoteUser();
            if (user != null) {
                msg.append(", user=").append(user);
            }
        }
        return msg;
    }

}
