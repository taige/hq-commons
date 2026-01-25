package io.hqwu.commons.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ValidatedDeserializer} 的单元测试类。
 * <p>
 * 测试覆盖：
 * <ul>
 *   <li>基本校验功能：验证 JSON 反序列化后的对象是否通过校验</li>
 *   <li>校验失败场景：验证校验失败时是否抛出正确的异常</li>
 *   <li>校验分组功能：验证 validateGroups() 方法的分组校验</li>
 *   <li>ValidatedJsonResponse 特殊处理：验证错误响应时跳过校验</li>
 *   <li>多字段校验：验证多个字段同时校验的场景</li>
 * </ul>
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 */
class ValidatedDeserializerTest {

    private ObjectMapper objectMapper;
    private Validator validator;
    private ViolationExceptionFactory exceptionFactory;
    private ValidatorFactory validatorFactory;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();

        // 创建一个简单的异常工厂，用于测试
        exceptionFactory = (violations, json) -> {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Validation failed");
            return new ValidationException(message, violations);
        };

        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyDeserializer(
                    DeserializationConfig config,
                    BeanDescription beanDesc,
                    JsonDeserializer<?> deserializer) {
                if (ValidatedJson.class.isAssignableFrom(beanDesc.getBeanClass())
                        && deserializer instanceof BeanDeserializerBase) {
                    return new ValidatedDeserializer(
                            (BeanDeserializerBase) deserializer,
                            validator,
                            exceptionFactory
                    );
                }
                return deserializer;
            }
        });
        objectMapper.registerModule(module);
    }

    @AfterEach
    void tearDown() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    /**
     * 测试用例 1: 基本的校验成功场景
     * 验证：当 JSON 数据满足所有约束时，反序列化成功
     */
    @Test
    void testDeserialize_validObject_success() throws IOException {
        String json = "{\"name\":\"John Doe\",\"email\":\"john@example.com\"}";

        SimpleRequest request = objectMapper.readValue(json, SimpleRequest.class);

        assertNotNull(request);
        assertEquals("John Doe", request.getName());
        assertEquals("john@example.com", request.getEmail());
    }

    /**
     * 测试用例 2: 校验失败场景 - 必填字段为空
     * 验证：当必填字段为 null 时，抛出 ValidationException
     */
    @Test
    void testDeserialize_nullRequiredField_throwsException() {
        String json = "{\"name\":null,\"email\":\"john@example.com\"}";

        ValidationException exception = assertThrows(ValidationException.class,
                () -> objectMapper.readValue(json, SimpleRequest.class));

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("不能为空") ||
                   exception.getMessage().contains("must not be blank"));
    }

    /**
     * 测试用例 3: 校验失败场景 - 字符串长度不符合要求
     * 验证：当字段不满足长度约束时，抛出 ValidationException
     */
    @Test
    void testDeserialize_invalidSize_throwsException() {
        String json = "{\"name\":\"Jo\",\"email\":\"john@example.com\"}";

        ValidationException exception = assertThrows(ValidationException.class,
                () -> objectMapper.readValue(json, SimpleRequest.class));

        assertNotNull(exception);
        assertNotNull(exception.getViolations());
        assertFalse(exception.getViolations().isEmpty());
    }

    /**
     * 测试用例 4: 多个字段同时违反约束
     * 验证：当多个字段都违反约束时，异常包含所有违规信息
     */
    @Test
    void testDeserialize_multipleViolations_throwsException() {
        String json = "{\"name\":null,\"email\":\"\"}";

        ValidationException exception = assertThrows(ValidationException.class,
                () -> objectMapper.readValue(json, SimpleRequest.class));

        assertNotNull(exception);
        assertNotNull(exception.getViolations());
        assertTrue(exception.getViolations().size() >= 2);
    }

    /**
     * 测试用例 5: 校验分组功能 - 基本分组校验成功
     * 验证：当对象指定了校验分组且满足分组约束时，反序列化成功
     */
    @Test
    void testDeserialize_withValidationGroups_success() throws IOException {
        String json = "{\"username\":\"john_doe\",\"userId\":\"12345\"}";

        GroupedRequest request = objectMapper.readValue(json, GroupedRequest.class);

        assertNotNull(request);
        assertEquals("john_doe", request.getUsername());
        assertEquals("12345", request.getUserId());
    }

    /**
     * 测试用例 6: 校验分组功能 - 分组校验失败
     * 验证：当对象指定了校验分组但不满足分组约束时，抛出异常
     */
    @Test
    void testDeserialize_withValidationGroups_failure() {
        String json = "{\"username\":\"john_doe\",\"userId\":null}";

        ValidationException exception = assertThrows(ValidationException.class,
                () -> objectMapper.readValue(json, GroupedRequest.class));

        assertNotNull(exception);
    }

    /**
     * 测试用例 7: ValidatedJsonResponse - 成功响应需要校验
     * 验证：当响应标识为成功时，执行正常的校验流程
     */
    @Test
    void testDeserialize_successResponse_validated() throws IOException {
        String json = "{\"success\":true,\"data\":\"result\"}";

        ApiResponse response = objectMapper.readValue(json, ApiResponse.class);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("result", response.getData());
    }

    /**
     * 测试用例 8: ValidatedJsonResponse - 失败响应跳过校验
     * 验证：当响应标识为失败（hasError 返回 true）时，跳过校验
     */
    @Test
    void testDeserialize_errorResponse_skipValidation() throws IOException {
        // data 为 null，但因为是错误响应，应该跳过校验
        String json = "{\"success\":false,\"data\":null}";

        ApiResponse response = objectMapper.readValue(json, ApiResponse.class);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertNull(response.getData());
    }

    /**
     * 测试用例 9: ValidatedJsonResponse - 成功响应但数据违规
     * 验证：当响应为成功但数据违反约束时，应抛出异常
     */
    @Test
    void testDeserialize_successResponseWithInvalidData_throwsException() {
        String json = "{\"success\":true,\"data\":null}";

        ValidationException exception = assertThrows(ValidationException.class,
                () -> objectMapper.readValue(json, ApiResponse.class));

        assertNotNull(exception);
    }

    /**
     * 测试用例 10: 空字符串与 @NotBlank 约束
     * 验证：空字符串应该违反 @NotBlank 约束
     */
    @Test
    void testDeserialize_blankString_throwsException() {
        String json = "{\"name\":\"   \",\"email\":\"john@example.com\"}";

        ValidationException exception = assertThrows(ValidationException.class,
                () -> objectMapper.readValue(json, SimpleRequest.class));

        assertNotNull(exception);
    }

    /**
     * 测试用例 11: 无校验分组的对象
     * 验证：当 validateGroups() 返回 null 时，只执行默认校验
     */
    @Test
    void testDeserialize_noValidationGroups_success() throws IOException {
        String json = "{\"name\":\"John Doe\",\"email\":\"john@example.com\"}";

        SimpleRequest request = objectMapper.readValue(json, SimpleRequest.class);

        assertNotNull(request);
        assertNull(request.validateGroups());
    }

    /**
     * 测试用例 12: 空数组校验分组
     * 验证：当 validateGroups() 返回空数组时，只执行默认校验
     */
    @Test
    void testDeserialize_emptyValidationGroups_success() throws IOException {
        String json = "{\"title\":\"Test Title\"}";

        EmptyGroupRequest request = objectMapper.readValue(json, EmptyGroupRequest.class);

        assertNotNull(request);
        assertEquals("Test Title", request.getTitle());
    }

    // ==================== 测试辅助类 ====================

    /**
     * 简单请求对象 - 测试基本校验功能
     */
    static class SimpleRequest implements ValidatedJson {
        @NotBlank(message = "姓名不能为空")
        @Size(min = 3, max = 50, message = "姓名长度必须在3-50之间")
        private String name;

        @NotBlank(message = "邮箱不能为空")
        private String email;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    /**
     * 分组校验请求对象 - 测试校验分组功能
     */
    static class GroupedRequest implements ValidatedJson {
        @NotBlank
        private String username;

        @NotBlank(groups = UpdateGroup.class)
        private String userId;

        @Override
        public Class<?>[] validateGroups() {
            return new Class<?>[]{UpdateGroup.class};
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }

    /**
     * 空分组请求对象 - 测试空数组校验分组
     */
    static class EmptyGroupRequest implements ValidatedJson {
        @NotBlank
        private String title;

        @Override
        public Class<?>[] validateGroups() {
            return new Class<?>[]{};
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    /**
     * API 响应对象 - 测试 ValidatedJsonResponse 功能
     */
    static class ApiResponse implements ValidatedJsonResponse {
        private boolean success;

        @NotNull(message = "数据不能为空")
        private String data;

        @Override
        public boolean hasError() {
            return !success;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    /**
     * 更新分组标识接口
     */
    interface UpdateGroup {
    }

    /**
     * 自定义校验异常 - 用于测试
     */
    static class ValidationException extends RuntimeException {
        private final Set<? extends ConstraintViolation<?>> violations;

        public ValidationException(String message, Set<? extends ConstraintViolation<?>> violations) {
            super(message);
            this.violations = violations;
        }

        public Set<? extends ConstraintViolation<?>> getViolations() {
            return violations;
        }
    }
}
