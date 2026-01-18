package io.hqwu.commons.annotation.support;

import com.fasterxml.jackson.databind.JsonNode;
import io.hqwu.commons.annotation.JsonField;
import io.hqwu.commons.servlet.support.CachedBodyHttpServletRequest;
import io.hqwu.commons.util.ClassUtil;
import io.hqwu.commons.util.Logger;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMessageConverterMethodArgumentResolver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JsonField解析器
 * 解决的问题：
 * 1、单个字符串等包装类型都要写一个对象才可以用@RequestBody接收；
 * 2、多个对象需要封装到一个对象里才可以用@RequestBody接收。
 * 主要优势：
 * 1、支持通过注解的value指定JSON的key来解析对象。
 * 2、支持通过注解无value，直接根据参数名来解析对象
 * 3、支持基本类型的注入
 * 4、支持GET和其他请求方式注入
 * 5、支持通过注解无value且参数名不匹配JSON串key时，根据属性解析对象。
 * 6、支持多余属性(不解析、不报错)、支持参数“共用”（不指定value时，参数名不为JSON串的key）
 * 7、支持当value和属性名找不到匹配的key时，对象是否匹配所有属性。
 *
 * 灵感来源: Wangyang Liu https://github.com/chujianyun/Spring-MultiRequestBody
 * @author Wu, Hongqiang (重新实现)
 * @since 2018/08/27
 *
 */
public class JsonFieldArgumentProcessor extends AbstractMessageConverterMethodArgumentResolver {
    private static final Logger LOGGER = new Logger();

    private static final String SERVLET_SERVER_HTTP_REQUEST = "ServletServerHttpRequest";
    private static final String BYTES_BODY_MAP = "BytesBodyMap";

    public JsonFieldArgumentProcessor(List<HttpMessageConverter<?>> converters) {
        super(converters);
    }

    public JsonFieldArgumentProcessor(List<HttpMessageConverter<?>> converters, List<Object> requestResponseBodyAdvice) {
        super(converters, requestResponseBodyAdvice);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(JsonField.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory)
            throws Exception {
        parameter = parameter.nestedIfOptional();
        Object arg = readWithMessageConverters(webRequest, parameter, parameter.getNestedGenericParameterType());
        String name = Conventions.getVariableNameForParameter(parameter);

        if (binderFactory != null) {
            WebDataBinder binder = binderFactory.createBinder(webRequest, arg, name);
            if (arg != null) {
                validateIfApplicable(binder, parameter);
                if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
                    throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
                }
            }
            if (mavContainer != null) {
                mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
            }
        }

        return adaptArgumentIfNecessary(arg, parameter);
    }

    @Override
    protected Object readWithMessageConverters(NativeWebRequest webRequest, MethodParameter parameter, Type paramType)
            throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {
        Object value = null;
        HttpInputMessage inputMessage = createInputMessage(webRequest, parameter);
        if (inputMessage != null) {
            value = readWithMessageConverters(inputMessage, parameter, paramType);
            checkRequired(value, parameter, inputMessage);
        }
        return value;
    }

    protected void checkRequired(Object value, MethodParameter parameter, HttpInputMessage inputMessage)
            throws HttpMessageNotReadableException {
        JsonField requestBody = parameter.getParameterAnnotation(JsonField.class);
        if (requestBody != null && value == null && requestBody.required() && !parameter.isOptional()) {
            throw new HttpMessageNotReadableException("Required request body is missing: " +
                    parameter.getExecutable().toGenericString(), inputMessage);
        }
    }

    protected HttpInputMessage createInputMessage(NativeWebRequest webRequest, MethodParameter parameter)
            throws IOException, HttpMediaTypeNotSupportedException {
        HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        Assert.state(servletRequest != null, "No HttpServletRequest");
        servletRequest = new CachedBodyHttpServletRequest(servletRequest);

        ServletServerHttpRequest inputMessage;
        Map<String, byte[]> bsBodyMap;
        long contentLength;
        if (webRequest.getAttribute(SERVLET_SERVER_HTTP_REQUEST, NativeWebRequest.SCOPE_REQUEST) == null) {
            inputMessage = new ServletServerHttpRequest(servletRequest);
            JsonRequest jsonRequest = (JsonRequest) readWithMessageConverters(inputMessage, parameter, JsonRequest.class);
            if (jsonRequest == null) {
                return null;
            }
            bsBodyMap = new HashMap<>();
            Charset contentCharset = Optional.ofNullable(Optional.ofNullable(inputMessage.getHeaders().getContentType())
                    .orElse(MediaType.APPLICATION_JSON).getCharset()).orElse(StandardCharsets.UTF_8);
            jsonRequest.forEach((name, node) -> {
                byte[] bs = node.toString().getBytes(contentCharset);
                bsBodyMap.put(name, bs);
            });
            contentLength = inputMessage.getHeaders().getContentLength();
            webRequest.setAttribute(HttpHeaders.CONTENT_LENGTH, contentLength, NativeWebRequest.SCOPE_REQUEST);
            webRequest.setAttribute(SERVLET_SERVER_HTTP_REQUEST, inputMessage, NativeWebRequest.SCOPE_REQUEST);
            webRequest.setAttribute(BYTES_BODY_MAP, bsBodyMap, NativeWebRequest.SCOPE_REQUEST);
        } else {
            inputMessage = (ServletServerHttpRequest) webRequest.getAttribute(
                    SERVLET_SERVER_HTTP_REQUEST, NativeWebRequest.SCOPE_REQUEST);
            bsBodyMap = (Map<String, byte[]>) webRequest.getAttribute(BYTES_BODY_MAP, NativeWebRequest.SCOPE_REQUEST);
            contentLength = (long) webRequest.getAttribute(HttpHeaders.CONTENT_LENGTH, NativeWebRequest.SCOPE_REQUEST);
        }
        return getInputMessage(inputMessage, bsBodyMap, contentLength, parameter);
    }

    protected HttpInputMessage getInputMessage(HttpInputMessage inputMessage, Map<String, byte[]> bsBodyMap,
                                               long contentLength, MethodParameter parameter) {
        JsonField annJsonField = parameter.getParameterAnnotation(JsonField.class);
        assert annJsonField != null;

        byte[] body;
        //注解的value是json的key
        String key = annJsonField.value();
        if (StringUtils.isNotEmpty(key)) {
            // 如果设置了value但是解析不到，报错
            body = bsBodyMap.get(key);
            checkRequired(body, parameter, inputMessage);
        } else {
            // 未设置value则用参数名当做json的key
            key = parameter.getParameterName();
            body = bsBodyMap.get(key);
        }
        if (body == null) {
            // key在json中不存在，则尝试将整个json串解析为当前参数类型
            Class<?> parameterType = parameter.getParameterType();
            // 基本类型的包装类
            if (ClassUtil.isPrimitiveWrapper(parameterType)) {
                // checkRequired
                checkRequired(null, parameter, inputMessage);
                // 否则返回null
                return null;
            } else if (parameterType.isPrimitive()) {
                // 原始类型没有解析会触发invoke异常
                String methodName = parameter.getMethod() == null ? "null" :
                        parameter.getMethod().getDeclaringClass().getName() + "#" + parameter.getMethod().getName();
                LOGGER.error("fail to resolve method `%s` #%d argument `%s` with primitive type `%s`, " +
                                "use PrimitiveWrapper class instead in case of IllegalArgumentException.",
                        methodName, parameter.getParameterIndex(), parameter.getParameterName(), parameterType.getName());
                throw new IllegalArgumentException(String.format("method `%s` #%d argument `%s` with improperly primitive type `%s`",
                        methodName, parameter.getParameterIndex(), parameter.getParameterName(), parameterType.getName()));
            }
            // 非基本类型, (不解析所有字段 || 非必须)
            if (! annJsonField.parseAllFields() || ! annJsonField.required()) {
                // checkRequired
                checkRequired(null, parameter, inputMessage);
                // 否则返回null
                return null;
            }
            // 非基本类型，解析所有外层字段
            if (contentLength >= 0) {
                inputMessage.getHeaders().setContentLength(contentLength);
            }
            return inputMessage;
        }
        // 伪造 HttpInputMessage
        if (contentLength >= 0) {
            inputMessage.getHeaders().setContentLength(body.length);
        }
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() throws IOException {
                return new ByteArrayInputStream(body);
            }

            @Override
            public HttpHeaders getHeaders() {
                return inputMessage.getHeaders();
            }
        };
    }

    private static class JsonRequest extends HashMap<String, JsonNode> {
        
    }
}
