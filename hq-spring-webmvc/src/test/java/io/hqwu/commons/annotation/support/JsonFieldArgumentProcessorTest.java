package io.hqwu.commons.annotation.support;

import io.hqwu.commons.annotation.JsonField;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link JsonFieldArgumentProcessor} running in a Spring Boot test context.
 * <p>
 * Tests verify the functionality by invoking Controller methods via MockMvc and checking
 * the actual arguments received by the Controller.
 * </p>
 */
@SpringBootTest(classes = JsonFieldArgumentProcessorTest.TestConfig.class)
public class JsonFieldArgumentProcessorTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private TestController testController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        // Use the full WebApplicationContext to ensure all beans and configurers are applied
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
        testController.reset();
    }

    @Configuration
    @EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        RedisAutoConfiguration.class
    })
    static class TestConfig implements WebMvcConfigurer {
        
        @Bean
        public JsonFieldArgumentProcessor jsonFieldArgumentProcessor() {
            List<HttpMessageConverter<?>> converters = new ArrayList<>();
            converters.add(new MappingJackson2HttpMessageConverter());
            return new JsonFieldArgumentProcessor(converters);
        }

        @Bean
        public TestController testController() {
            return new TestController();
        }

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            // Register our custom argument resolver
            resolvers.add(jsonFieldArgumentProcessor());
        }
    }

    /**
     * 功能 1: 支持通过注解的 value 指定 JSON 的 key 来解析对象。
     */
    @Test
    public void testResolveArgument_WithAnnotationValue() throws Exception {
        String jsonBody = "{\"user_name\": \"Zhang San\", \"user_age\": 25}";

        mockMvc.perform(post("/user/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isOk());

        assertEquals("Zhang San", testController.f1_name);
        assertEquals(25, testController.f1_age);
    }

    /**
     * 功能 2: 支持通过注解无 value，直接根据参数名来解析对象。
     */
    @Test
    public void testResolveArgument_WithParameterName() throws Exception {
        String jsonBody = "{\"username\": \"Li Si\", \"age\": 30}";

        mockMvc.perform(post("/user/create/default")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isOk());

        assertEquals("Li Si", testController.f2_username);
        assertEquals(30, testController.f2_age);
    }

    /**
     * 功能 3: 支持基本类型的注入。
     */
    @Test
    public void testResolveArgument_PrimitiveWrapper() throws Exception {
        String jsonBody = "{\"count\": 10, \"enabled\": true, \"score\": 95.5}";

        mockMvc.perform(post("/data/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isOk());

        assertEquals(10, testController.f3_count);
        assertEquals(true, testController.f3_enabled);
        assertEquals(95.5, testController.f3_score);
    }

    /**
     * 功能 4: 支持 GET 和其他请求方式注入。
     */
    @Test
    public void testResolveArgument_GetRequestWithBody() throws Exception {
        String jsonBody = "{\"keyword\": \"Spring\", \"category\": \"Tech\"}";

        // GET request with body
        mockMvc.perform(get("/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isOk());

        assertEquals("Spring", testController.f4_keyword);
        assertEquals("Tech", testController.f4_category);
    }

    /**
     * 功能 4 (补充): 验证 GET 请求的 Query 参数**不支持**注入。
     * @JsonField 仅针对 Request Body 解析，不会读取 URL 参数。
     * 
     * 注意：为了验证这一点，我们需要确保 Controller 方法允许空 Body (required=false)，
     * 否则会因为缺少 Body 而直接抛出 400 异常，导致无法验证参数是否为 null。
     */
    @Test
    public void testResolveArgument_GetRequestWithQueryParams_NotSupported() throws Exception {
        // GET request with query parameters but NO body
        mockMvc.perform(get("/search")
                .param("keyword", "Spring")
                .param("category", "Tech")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Expectation: Query parameters are ignored, fields remain null
        assertNull(testController.f4_keyword);
        assertNull(testController.f4_category);
    }

    /**
     * 功能 5: 支持通过注解无 value 且参数名不匹配 JSON 串 key 时，根据属性解析对象。
     */
    @Test
    public void testResolveArgument_PojoAttributeMatching() throws Exception {
        String jsonBody = "{\"userName\": \"Wang Wu\", \"userAge\": 28}";

        mockMvc.perform(post("/user/dto")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isOk());

        assertNotNull(testController.f5_dto);
        assertEquals("Wang Wu", testController.f5_dto.getUserName());
        assertEquals(28, testController.f5_dto.getUserAge());
    }

    /**
     * 功能 6: 支持多余属性（不解析、不报错）、支持参数"共用"。
     */
    @Test
    public void testResolveArgument_ParameterSharingAndExtraProperties() throws Exception {
        // JSON contains fields for both User and Role, plus an extra field
        String jsonBody = "{\"username\": \"Zhao Liu\", \"roleName\": \"Admin\", \"extra\": \"ignored\"}";

        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isOk());

        assertNotNull(testController.f6_user);
        assertEquals("Zhao Liu", testController.f6_user.getUsername());

        assertNotNull(testController.f6_role);
        assertEquals("Admin", testController.f6_role.getRoleName());
    }

    /**
     * 功能 7: 支持当 value 和属性名找不到匹配的 key 时，对象是否匹配所有属性。 （功能5 之 parseAllFields = false）
     */
    @Test
    public void testResolveArgument_ParseAllFieldsOption_Fixed() throws Exception {
        String jsonBody = "{\"username\": \"Sun Qi\"}";

        mockMvc.perform(post("/user/strict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isOk());

        assertNotNull(testController.f7_user);
        assertEquals("Sun Qi", testController.f7_user.getUsername());

        // userStrict should be null because parseAllFields=false and key "userStrict" is missing
        assertNull(testController.f7_userStrict);
    }

    /**
     * 功能 5 (补充): 验证 parseAllFields=false 且 required=true 时，缺少 key 会抛出 HttpMessageNotReadableException。
     */
    @Test
    public void testResolveArgument_ParseAllFieldsFalse_RequiredTrue_ThrowsException() throws Exception {
        String jsonBody = "{\"username\": \"Sun Qi\"}";

        // f10 expects a strict UserDTO (parseAllFields=false, required=true)
        // The JSON body does not have a key "userStrictRequired", so it should fail.
        mockMvc.perform(post("/user/strict/required")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isBadRequest()); // HttpMessageNotReadableException results in 400 Bad Request
    }

    /**
     * 补充测试: 验证 required=true 时，缺少 key 会抛出异常 (400 Bad Request)。
     */
    @Test
    public void testResolveArgument_RequiredMissing() throws Exception {
        String jsonBody = "{\"other\": \"value\"}";

        mockMvc.perform(post("/check/required")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isBadRequest());
    }

    /**
     * 补充测试: 验证使用基本类型 (int) 时，如果 JSON 中存在对应 Key，可以正常解析。
     */
    @Test
    public void testResolveArgument_PrimitiveType_Success() throws Exception {
        String jsonBody = "{\"val\": 123}";

        mockMvc.perform(post("/check/primitive")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isOk());
    }

    /**
     * 补充测试: 验证使用基本类型 (int) 时，如果 JSON 中缺失对应 Key，会抛出 IllegalArgumentException。
     * (因为基本类型无法赋值为 null)
     */
    @Test
    public void testResolveArgument_PrimitiveType_Missing_ThrowsException() {
        String jsonBody = "{\"other\": 1}";

        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/check/primitive")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonBody));
        });

        // The exception might be wrapped in NestedServletException
        Throwable cause = exception.getCause();
        assertTrue(cause instanceof IllegalArgumentException || exception instanceof IllegalArgumentException);
    }

    /**
     * 补充测试: 验证基本类型包装类 (Wrapper) 在 required=false 且缺失 Key 时，应注入 null 而非报错。
     */
    @Test
    public void testResolveArgument_PrimitiveWrapper_NotRequired_Missing() throws Exception {
        String jsonBody = "{\"other\": 1}";

        mockMvc.perform(post("/check/wrapper/optional")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isOk());

        // Expectation: The optional Integer field is null
        assertNull(testController.f11_optionalAge);
    }

    /**
     * 补充测试: 验证 JSR-303 / Bean Validation 集成 (@Valid)。
     * 验证当参数对象内部校验失败时，会抛出 MethodArgumentNotValidException (400 Bad Request)。
     */
    @Test
    public void testResolveArgument_Validation_Fails() throws Exception {
        // ValidatedDTO requires name != null and age >= 18
        String jsonBody = "{\"name\": null, \"age\": 16}";

        mockMvc.perform(post("/check/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isBadRequest()); // MethodArgumentNotValidException
    }

    /**
     * 补充测试: 验证 JSR-303 / Bean Validation 集成 (@Valid)。
     * 验证当参数对象内部校验成功时，正常执行。
     */
    @Test
    public void testResolveArgument_Validation_Success() throws Exception {
        // ValidatedDTO requires name != null and age >= 18
        String jsonBody = "{\"name\": \"Valid User\", \"age\": 20}";

        mockMvc.perform(post("/check/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isOk());
    }

    /**
     * 补充测试: 验证 Optional<T> 类型的支持。
     * 验证当 JSON 中存在对应 Key 时，Optional 包含正确的值。
     */
    @Test
    public void testResolveArgument_Optional_Present() throws Exception {
        String jsonBody = "{\"optVal\": \"present\"}";

        mockMvc.perform(post("/check/optional")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isOk());

        assertNotNull(testController.f13_optional);
        assertTrue(testController.f13_optional.isPresent());
        assertEquals("present", testController.f13_optional.get());
    }

    /**
     * 补充测试: 验证 Optional<T> 类型的支持。
     * 验证当 JSON 中缺失对应 Key 时，Optional 为 empty。
     * 
     * 注意：这里不需要在 @JsonField 中显式指定 required=false，
     * 因为 JsonFieldArgumentProcessor 应该能自动识别 Optional 并放宽限制。
     */
    @Test
    public void testResolveArgument_Optional_Empty() throws Exception {
        String jsonBody = "{\"other\": \"value\"}";

        mockMvc.perform(post("/check/optional")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isOk());

        assertNotNull(testController.f13_optional);
        assertFalse(testController.f13_optional.isPresent());
    }

    /**
     * 补充测试: 验证空 Body 的处理。
     * 当 Body 为空时，应该抛出 HttpMessageNotReadableException (400 Bad Request)，
     * 除非参数是 Optional 或 required=false。
     */
    @Test
    public void testResolveArgument_EmptyBody_ThrowsException() throws Exception {
        String jsonBody = "";

        mockMvc.perform(post("/user/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isBadRequest());
    }

    /**
     * 补充测试: 验证非 JSON 格式 Body 的处理。
     * 当 Body 格式错误（如缺少括号）时，应该抛出 HttpMessageNotReadableException (400 Bad Request)。
     */
    @Test
    public void testResolveArgument_InvalidJson_ThrowsException() throws Exception {
        String jsonBody = "{invalid_json";

        mockMvc.perform(post("/user/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isBadRequest());
    }


    // --- Helper Controller & Classes ---

    @RestController
    static class TestController {
        // Feature 1
        String f1_name;
        Integer f1_age;
        @PostMapping("/user/create")
        public void createUser(@JsonField("user_name") String name, @JsonField("user_age") Integer age) {
            this.f1_name = name;
            this.f1_age = age;
        }

        // Feature 2
        String f2_username;
        Integer f2_age;
        @PostMapping("/user/create/default")
        public void createUserDefault(@JsonField String username, @JsonField Integer age) {
            this.f2_username = username;
            this.f2_age = age;
        }

        // Feature 3
        Integer f3_count;
        Boolean f3_enabled;
        Double f3_score;
        @PostMapping("/data/process")
        public void processData(@JsonField Integer count, @JsonField Boolean enabled, @JsonField Double score) {
            this.f3_count = count;
            this.f3_enabled = enabled;
            this.f3_score = score;
        }

        // Feature 4
        String f4_keyword;
        String f4_category;
        @GetMapping("/search")
        public void search(@JsonField(required = false) String keyword, @JsonField(required = false) String category) {
            this.f4_keyword = keyword;
            this.f4_category = category;
        }

        // Feature 5
        UserDTO f5_dto;
        @PostMapping("/user/dto")
        public void createUserDto(@JsonField UserDTO dto) {
            this.f5_dto = dto;
        }

        // Feature 6
        User f6_user;
        Role f6_role;
        @PostMapping("/register")
        public void register(@JsonField User user, @JsonField Role role) {
            this.f6_user = user;
            this.f6_role = role;
        }

        // Feature 7
        User f7_user;
        User f7_userStrict;
        @PostMapping("/user/strict")
        public void createUserStrict(
                @JsonField(parseAllFields = true) User user,
                @JsonField(parseAllFields = false, required = false) User userStrict
        ) {
            this.f7_user = user;
            this.f7_userStrict = userStrict;
        }

        // Feature 8: Required check
        @PostMapping("/check/required")
        public void requiredCheck(@JsonField(value = "must_exist", required = true) String val) {
        }

        // Feature 9: Primitive type check
        @PostMapping("/check/primitive")
        public void primitiveCheck(@JsonField int val) {
        }

        // Feature 5 (Supplement): ParseAllFields=false & Required=true
        @PostMapping("/user/strict/required")
        public void strictRequiredCheck(
                @JsonField(parseAllFields = false, required = true) UserDTO userStrictRequired
        ) {
        }

        // Feature 3 (Supplement): Wrapper type not required
        Integer f11_optionalAge;
        @PostMapping("/check/wrapper/optional")
        public void wrapperNotRequiredCheck(@JsonField(required = false) Integer optionalAge) {
            this.f11_optionalAge = optionalAge;
        }

        // Validation Check
        @PostMapping("/check/validation")
        public void validationCheck(@JsonField @Valid ValidatedDTO dto) {
        }

        // Optional Check
        Optional<String> f13_optional;
        @PostMapping("/check/optional")
        public void optionalCheck(@JsonField Optional<String> optVal) {
            this.f13_optional = optVal;
        }

        public void reset() {
            f1_name = null; f1_age = null;
            f2_username = null; f2_age = null;
            f3_count = null; f3_enabled = null; f3_score = null;
            f4_keyword = null; f4_category = null;
            f5_dto = null;
            f6_user = null; f6_role = null;
            f7_user = null; f7_userStrict = null;
            f11_optionalAge = null;
            f13_optional = null;
        }
    }

    static class UserDTO {
        private String userName;
        private Integer userAge;

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public Integer getUserAge() { return userAge; }
        public void setUserAge(Integer userAge) { this.userAge = userAge; }
    }

    static class User {
        private String username;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }

    static class Role {
        private String roleName;
        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
    }

    static class ValidatedDTO {
        @NotNull
        private String name;

        @Min(18)
        private Integer age;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
    }
}
