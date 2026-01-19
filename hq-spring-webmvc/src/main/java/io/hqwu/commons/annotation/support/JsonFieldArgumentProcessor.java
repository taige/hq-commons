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
import org.springframework.web.context.request.RequestAttributes;
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
 * JSON 字段参数解析器。
 * <p>
 * 用于解析 Spring MVC 控制器方法中带有 {@link JsonField} 注解的参数。
 * 该解析器继承自 {@link AbstractMessageConverterMethodArgumentResolver}，
 * 利用消息转换器将 HTTP 请求体中的 JSON 数据绑定到方法参数上。
 * </p>
 *
 * <h3>注意事项：</h3>
 * <p>
 * <strong>关于参数名获取：</strong><br>
 * 当 {@link JsonField#value()} 未指定时，本解析器依赖方法参数名来匹配 JSON key。
 * 在 Spring Boot 3.x (Spring Framework 6.1+) 中，必须在编译时添加 {@code -parameters} 选项，
 * 才能通过反射正确获取参数名。否则参数名将丢失，导致无法正确匹配 JSON key。
 * </p>
 *
 * <h3>解决的问题：</h3>
 * <ol>
 * <li>单个字符串等包装类型都要写一个对象才可以用 {@code @RequestBody} 接收；</li>
 * <li>多个对象需要封装到一个对象里才可以用 {@code @RequestBody} 接收。</li>
 * </ol>
 *
 * <h3>主要功能：</h3>
 * <ol>
 * <li><b>支持通过注解的 value 指定 JSON 的 key 来解析对象。</b>
 * <pre>
 * // 请求体: {"user_name": "张三", "user_age": 25}
 * &#64;PostMapping("/user")
 * public void createUser(
 *     &#64;JsonField("user_name") String name,  // 显式指定JSON key为user_name
 *     &#64;JsonField("user_age") Integer age    // 显式指定JSON key为user_age
 * ) { }
 * </pre>
 * </li>
 *
 * <li><b>支持通过注解无 value，直接根据参数名来解析对象。</b>
 * <pre>
 * // 请求体: {"username": "张三", "age": 25}
 * &#64;PostMapping("/user")
 * public void createUser(
 *     &#64;JsonField String username,  // 自动使用参数名username作为JSON key
 *     &#64;JsonField Integer age       // 自动使用参数名age作为JSON key
 * ) { }
 * </pre>
 * </li>
 *
 * <li><b>支持基本类型的注入。</b>
 * <pre>
 * // 请求体: {"count": 10, "enabled": true, "score": 95.5}
 * &#64;PostMapping("/data")
 * public void processData(
 *     &#64;JsonField Integer count,    // 包装类型
 *     &#64;JsonField Boolean enabled,  // 包装类型
 *     &#64;JsonField Double score      // 包装类型
 * ) { }
 * </pre>
 * </li>
 *
 * <li><b>支持 GET 和其他请求方式注入。</b>
 * <pre>
 * // GET请求 (注意：需发送 JSON 请求体)
 * // 请求体: {"keyword": "Spring", "category": "技术"}
 * &#64;GetMapping("/search")
 * public void search(
 *     &#64;JsonField String keyword,
 *     &#64;JsonField String category
 * ) { }
 *
 * // PUT请求
 * // 请求体: {"id": 100, "data": "content"}
 * &#64;PutMapping("/update")
 * public void update(
 *     &#64;JsonField Long id,
 *     &#64;JsonField String data
 * ) { }
 * </pre>
 * </li>
 *
 * <li><b>支持当 value 和属性名找不到匹配的 key 时，根据属性解析对象（可配置）。</b>
 * <p>
 * 当指定的 key（或参数名）在 JSON 中不存在时，默认会尝试将<b>整个 JSON 对象</b>绑定到参数对象上，
 * 从而实现扁平化 JSON 映射或参数共用。可以通过 {@code parseAllFields} 属性控制此行为。
 * </p>
 * <p>
 * <strong>注意：</strong> 如果 {@code parseAllFields = false} 且参数被标记为 {@code required = true}（默认），
 * 当找不到匹配的 key 时，将抛出 {@link HttpMessageNotReadableException} 异常。
 * </p>
 * <pre>
 * // 场景 1: 默认行为 (parseAllFields = true) - 智能匹配属性
 * // 请求体: {"userName": "张三", "userAge": 25}
 * // UserDTO 类包含 userName, userAge 属性
 * &#64;PostMapping("/user")
 * public void createUser(
 *     &#64;JsonField UserDTO dto  // 参数名dto不匹配，但自动解析整个JSON匹配内部属性
 * ) { }
 *
 * // 场景 2: 禁用智能匹配 (parseAllFields = false) - 严格匹配 key
 * // 请求体: {"userName": "张三"}
 * &#64;PostMapping("/user/strict")
 * public void createUserStrict(
 *     &#64;JsonField(parseAllFields = false) UserDTO dto  // 找不到 key "dto" 时抛出异常 (默认 required=true)
 * ) { }
 * </pre>
 * </li>
 *
 * <li><b>支持多余属性（不解析、不报错）、支持参数"共用"（不指定 value 且参数名不匹配时，参数名不为 JSON 串的 key）。</b>
 * <pre>
 * // 1. 支持多余属性示例：
 * // 请求体: {"name": "张三", "age": 25, "email": "zhang@example.com", "extra": "ignored"}
 * &#64;PostMapping("/user")
 * public void createUser(
 *     &#64;JsonField String name,
 *     &#64;JsonField Integer age
 *     // email和extra字段被忽略，不会报错
 * ) { }
 *
 * // 2. 参数共用示例（多个对象从同一个JSON中各取所需）：
 * // 请求体: {
 * //   "username": "张三",
 * //   "roleName": "管理员",
 * //   "captcha": "X7Y9"
 * // }
 *
 * // User 类包含 username 属性
 * // Role 类包含 roleName 属性
 *
 * &#64;PostMapping("/register")
 * public void register(
 *     &#64;JsonField User user,  // 自动匹配 username，忽略 roleName 和 captcha
 *     &#64;JsonField Role role   // 自动匹配 roleName，忽略 username 和 captcha
 * ) {
 *     // 两个参数共用同一个 JSON 请求体进行属性匹配
 * }
 * </pre>
 * </li>
 * </ol>
 *
 * <h3>综合使用示例：</h3>
 * <pre>
 * // 请求体: {
 * //   "user_id": 123,
 * //   "userName": "张三",
 * //   "age": 25,
 * //   "email": "zhang@example.com",
 * //   "extraData": "will be ignored"
 * // }
 * //
 * // UserContact 类包含 email 属性
 *
 * &#64;PostMapping("/user/complex")
 * public ResponseEntity&lt;String&gt; complexExample(
 *     &#64;JsonField("user_id") Long userId,           // 功能1: 指定JSON key
 *     &#64;JsonField String userName,                  // 功能2: 参数名匹配
 *     &#64;JsonField Integer age,                      // 功能3: 基本类型包装类
 *     &#64;JsonField UserContact contact               // 功能5: 参数名contact不匹配，解析整个JSON匹配email属性
 * ) {
 *     // 处理逻辑
 *     return ResponseEntity.ok("success");
 * }
 * </pre>
 *
 * <p>
 * 灵感来源：Wangyang Liu https://github.com/chujianyun/Spring-MultiRequestBody
 * </p>
 *
 * @author Wu, Hongqiang (重新实现)
 * @see JsonField
 * @see AbstractMessageConverterMethodArgumentResolver
 * @see HttpMessageConverter
 * @see org.springframework.web.bind.annotation.RequestBody
 * @since 2018/08/27
 */
public class JsonFieldArgumentProcessor extends AbstractMessageConverterMethodArgumentResolver {
    private static final Logger LOGGER = new Logger();

    private static final String SERVLET_SERVER_HTTP_REQUEST = "ServletServerHttpRequest";
    private static final String BYTES_BODY_MAP = "BytesBodyMap";
    private static final String IS_OPTIONAL_ATTRIBUTE = JsonFieldArgumentProcessor.class.getName() + ".IS_OPTIONAL";

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
        // 记录原始参数是否为 Optional
        boolean isOptional = parameter.isOptional();
        if (isOptional) {
            webRequest.setAttribute(IS_OPTIONAL_ATTRIBUTE, true, RequestAttributes.SCOPE_REQUEST);
        }

        try {
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
        } finally {
            // 清理 Attribute，避免污染后续处理
            if (isOptional) {
                webRequest.removeAttribute(IS_OPTIONAL_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            }
        }
    }

    @Override
    protected Object readWithMessageConverters(NativeWebRequest webRequest, MethodParameter parameter, Type paramType)
            throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {
        Object value = null;
        HttpInputMessage inputMessage = createInputMessage(webRequest, parameter);
        if (inputMessage != null) {
            value = readWithMessageConverters(inputMessage, parameter, paramType);
            checkRequired(value, parameter, inputMessage, webRequest);
        } else {
            // 如果 inputMessage 为 null，说明 Body 为空或无法解析
            // 此时仍需检查 required 属性
            checkRequired(null, parameter, null, webRequest);
        }
        return value;
    }

    private boolean isOptional(MethodParameter parameter, NativeWebRequest webRequest) {
        return parameter.isOptional() || Boolean.TRUE.equals(webRequest.getAttribute(IS_OPTIONAL_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST));
    }

    protected void checkRequired(Object value, MethodParameter parameter, HttpInputMessage inputMessage, NativeWebRequest webRequest)
            throws HttpMessageNotReadableException {
        JsonField requestBody = parameter.getParameterAnnotation(JsonField.class);
        if (requestBody != null && value == null && requestBody.required() && !isOptional(parameter, webRequest)) {
            throw new HttpMessageNotReadableException("Required request body is missing: " +
                    parameter.getExecutable().toGenericString(), inputMessage);
        }
    }

    protected HttpInputMessage createInputMessage(NativeWebRequest webRequest, MethodParameter parameter)
            throws IOException, HttpMediaTypeNotSupportedException {
        HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        Assert.state(servletRequest != null, "No HttpServletRequest");
        // 包装请求，缓存 Body，以便多次读取
        servletRequest = new CachedBodyHttpServletRequest(servletRequest);

        ServletServerHttpRequest inputMessage;
        Map<String, byte[]> bsBodyMap;
        long contentLength;
        // 检查是否已经解析过 Body
        if (webRequest.getAttribute(SERVLET_SERVER_HTTP_REQUEST, NativeWebRequest.SCOPE_REQUEST) == null) {
            // [功能4] 支持 GET 和其他请求方式注入
            // 这里直接从 HttpServletRequest 读取 Body，不限制 HTTP Method。
            // 只要请求包含 JSON Body，无论是 POST, PUT 还是 GET，都能被解析。
            inputMessage = new ServletServerHttpRequest(servletRequest);

            // 使用消息转换器将整个 Body 解析为 JsonRequest (Map<String, JsonNode>)
            // 这是实现按 Key 解析 (功能1, 2) 的基础
            JsonRequest jsonRequest = (JsonRequest) readWithMessageConverters(inputMessage, parameter, JsonRequest.class);
            if (jsonRequest == null) {
                return null;
            }

            // 将解析后的 JSON 节点转回 byte 数组，存入 Map 中，以便后续按需提取
            bsBodyMap = new HashMap<>();
            Charset contentCharset = Optional.ofNullable(Optional.ofNullable(inputMessage.getHeaders().getContentType())
                    .orElse(MediaType.APPLICATION_JSON).getCharset()).orElse(StandardCharsets.UTF_8);
            jsonRequest.forEach((name, node) -> {
                byte[] bs = node.toString().getBytes(contentCharset);
                bsBodyMap.put(name, bs);
            });
            contentLength = inputMessage.getHeaders().getContentLength();
            // 将解析结果存入 Request 域缓存，避免同一个请求中多个 @JsonField 参数重复解析 Body
            webRequest.setAttribute(HttpHeaders.CONTENT_LENGTH, contentLength, NativeWebRequest.SCOPE_REQUEST);
            webRequest.setAttribute(SERVLET_SERVER_HTTP_REQUEST, inputMessage, NativeWebRequest.SCOPE_REQUEST);
            webRequest.setAttribute(BYTES_BODY_MAP, bsBodyMap, NativeWebRequest.SCOPE_REQUEST);
        } else {
            // 从缓存获取
            inputMessage = (ServletServerHttpRequest) webRequest.getAttribute(
                    SERVLET_SERVER_HTTP_REQUEST, NativeWebRequest.SCOPE_REQUEST);
            bsBodyMap = (Map<String, byte[]>) webRequest.getAttribute(BYTES_BODY_MAP, NativeWebRequest.SCOPE_REQUEST);
            contentLength = (long) webRequest.getAttribute(HttpHeaders.CONTENT_LENGTH, NativeWebRequest.SCOPE_REQUEST);
        }
        return getInputMessage(inputMessage, bsBodyMap, contentLength, parameter, webRequest);
    }

    protected HttpInputMessage getInputMessage(HttpInputMessage inputMessage, Map<String, byte[]> bsBodyMap,
                                               long contentLength, MethodParameter parameter, NativeWebRequest webRequest) {
        JsonField annJsonField = parameter.getParameterAnnotation(JsonField.class);
        assert annJsonField != null;

        byte[] body;
        // 1. 确定 JSON Key
        String key = annJsonField.value();
        if (StringUtils.isNotEmpty(key)) {
            // [功能1] 支持通过注解的 value 指定 JSON 的 key 来解析对象
            // 尝试根据注解指定的 key 获取对应的 JSON 片段
            body = bsBodyMap.get(key);
            // 如果指定了 key 但没找到，且参数是必须的，则在 checkRequired 中抛出异常
            checkRequired(body, parameter, inputMessage, webRequest);
        } else {
            // [功能2] 支持通过注解无 value，直接根据参数名来解析对象
            // 未指定 value 时，默认使用参数名作为 key
            key = parameter.getParameterName();
            body = bsBodyMap.get(key);
        }

        // 2. 处理 Key 未匹配的情况
        if (body == null) {
            // 进入此分支意味着：指定的 key (或参数名) 在 JSON 第一层中不存在。

            Class<?> parameterType = parameter.getParameterType();

            // [功能3] 支持基本类型的注入 (边界处理)
            // 如果是基本类型或其包装类，它们必须对应 JSON 中的一个具体字段值。
            // 既然 key 匹配失败，基本类型无法承载整个 JSON 对象，因此这里直接结束解析。
            if (ClassUtil.isPrimitiveWrapper(parameterType)) {
                checkRequired(null, parameter, inputMessage, webRequest);
                return null;
            } else if (parameterType.isPrimitive()) {
                // 原始类型无法接受 null，抛出异常提示开发者使用包装类
                String methodName = parameter.getMethod() == null ? "null" :
                        parameter.getMethod().getDeclaringClass().getName() + "#" + parameter.getMethod().getName();
                LOGGER.error("fail to resolve method `%s` #%d argument `%s` with primitive type `%s`, " +
                                "use PrimitiveWrapper class instead in case of IllegalArgumentException.",
                        methodName, parameter.getParameterIndex(), parameter.getParameterName(), parameterType.getName());
                throw new IllegalArgumentException(String.format("method `%s` #%d argument `%s` with improperly primitive type `%s`",
                        methodName, parameter.getParameterIndex(), parameter.getParameterName(), parameterType.getName()));
            }

            // [功能5] 支持当 value 和属性名找不到匹配的 key 时，根据属性解析对象（可配置）
            // 如果配置了 parseAllFields = false (默认 true)，或者参数非必须，则不尝试解析整个 JSON。
            if (! annJsonField.parseAllFields() || ! annJsonField.required() || isOptional(parameter, webRequest)) {
                checkRequired(null, parameter, inputMessage, webRequest);
                return null;
            }

            // [功能6] 支持多余属性、支持参数"共用"
            // 逻辑：将整个 HTTP 请求体 (inputMessage) 返回。
            // 后续 Spring 的 MessageConverter 会尝试将这个完整的 JSON 映射到当前参数对象 (parameterType) 上。
            // 这样，JSON 中的属性会自动匹配到对象的同名字段，多余的属性会被忽略 (Jackson 特性)，
            // 且同一个 JSON Body 可以被多个参数对象重复使用 (参数共用)。
            if (contentLength >= 0) {
                inputMessage.getHeaders().setContentLength(contentLength);
            }
            return inputMessage;
        }

        // 3. 处理 Key 匹配成功的情况
        // [功能1, 2, 3] 的具体实现基础
        // 构造一个新的 HttpInputMessage，其 Body 仅包含匹配到的那个 JSON 片段 (body 字节数组)。
        // 后续 Converter 会将这个片段解析为参数类型 (包括基本类型包装类)。
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
