package io.hqwu.commons.filter;

import com.umpay.commons.util.ClassUtil;
import com.umpay.commons.util.Formatter;
import com.umpay.commons.util.Logger;
import com.umpay.commons.util.LoggerFactory;
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
 * Created with IntelliJ IDEA for hq-spring-webmvc
 *
 * logging interceptor for RestTemplate as a rest client
 *
 * User: taige
 * Date: 2020/3/31
 * Time: 10:35
 */
public class RestClientLoggingInterceptor implements ClientHttpRequestInterceptor {
    private final Logger LOGGER;

    private final String logTag;
    private final URI baseUri;


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

        URI requestUri = baseUri.relativize(request.getURI());
        HttpMethod method = request.getMethod();
        
        LOGGER.info("[%s]%sing : /%s, Headers: %s", logTag, method, requestUri, request.getHeaders());
        if (LOGGER.isDebugEnabled() && ! HttpMethod.GET.equals(method) && ! HttpMethod.HEAD.equals(method)) {
            LOGGER.debug("[%s]Request body: %s", logTag, new String(body, StandardCharsets.UTF_8));
        }

        long startMS = System.currentTimeMillis();
        ClientHttpResponse response = execution.execute(request, body);
        HttpHeaders headers = response.getHeaders();
        LOGGER.info("[%s]%sed : /%s, Status: %s, Headers: %s, time: %sms", logTag,
                method, requestUri, response.getStatusCode(), headers, Formatter.formatNS(System.currentTimeMillis() - startMS));

        if (LOGGER.isDebugEnabled()) {
            Charset contentCharset = Optional.ofNullable(Optional.ofNullable(headers.getContentType())
                    .orElse(MediaType.APPLICATION_JSON).getCharset()).orElse(StandardCharsets.UTF_8);
            String responseBody = StreamUtils.copyToString(response.getBody(), contentCharset);
            LOGGER.debug("[%s]Response body: %s", logTag, responseBody);
        }

        return response;
    }

}
