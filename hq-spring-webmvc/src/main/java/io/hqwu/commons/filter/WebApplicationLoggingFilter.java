package io.hqwu.commons.filter;

import com.umpay.commons.util.Logger;
import com.umpay.commons.util.StringUtil;
import io.hqwu.commons.servlet.support.CachedBodyHttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.AbstractRequestLoggingFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA for checkpay-umf
 *
 * logging filter for RestController as a rest service
 *
 * User: taige
 * Date: 2020/4/5
 * Time: 15:59
 */
public class WebApplicationLoggingFilter extends AbstractRequestLoggingFilter {
    private static final Logger LOGGER = new Logger();

    private String beforeMessagePrefix = DEFAULT_BEFORE_MESSAGE_PREFIX;

    private String beforeMessageSuffix = DEFAULT_BEFORE_MESSAGE_SUFFIX;

    private String afterMessagePrefix = DEFAULT_AFTER_MESSAGE_PREFIX;

    private String afterMessageSuffix = DEFAULT_AFTER_MESSAGE_SUFFIX;

    private boolean enabled = true;

    private final String traceIdHeaderName;

    public WebApplicationLoggingFilter() {
        this(null);
    }

    public WebApplicationLoggingFilter(String traceIdHeaderName) {
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
            requestToUse = new CachedBodyHttpServletRequest(request);
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
        return wrapper == null ? null : _getMessagePayload(wrapper.getContentAsByteArray(), wrapper.getCharacterEncoding());
    }

    protected String getMessagePayload(HttpServletResponse response) {
        ContentCachingResponseWrapper wrapper =
                WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        return wrapper == null ? null :  _getMessagePayload(wrapper.getContentAsByteArray(), wrapper.getCharacterEncoding());
    }

    private String _getMessagePayload(byte[] contentAsByteArray, String characterEncoding) {
        if (contentAsByteArray != null && contentAsByteArray.length > 0) {
            int length = Math.min(contentAsByteArray.length, getMaxPayloadLength());
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
            msg.append(", trace-id=").append(traceId);
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
            msg.append(", trace-id=").append(traceId);
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
                payload = getMessagePayload(response);
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
